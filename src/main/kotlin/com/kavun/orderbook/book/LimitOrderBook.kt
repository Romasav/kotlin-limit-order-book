package com.kavun.orderbook.book

import com.kavun.orderbook.domain.Event
import com.kavun.orderbook.domain.LimitOrder
import com.kavun.orderbook.domain.OrderRested
import com.kavun.orderbook.domain.Price
import com.kavun.orderbook.domain.Quantity
import com.kavun.orderbook.domain.Side
import com.kavun.orderbook.domain.Symbol
import com.kavun.orderbook.domain.TradeExecuted
import java.util.SortedMap
import kotlin.math.min

class LimitOrderBook(
    val symbol: Symbol,
) {
    private val bids = sortedMapOf<Price, ArrayDeque<LimitOrder>>(compareByDescending<Price> { it.value })
    private val asks = sortedMapOf<Price, ArrayDeque<LimitOrder>>()

    fun placeLimitOrder(order: LimitOrder): List<Event> {
        require(order.symbol == symbol) {
            "Cannot place order for ${order.symbol} in book for $symbol"
        }

        var remainingQuantity = order.quantity.value
        val events = mutableListOf<Event>()
        val oppositeSide = when (order.side) {
            Side.BUY -> asks
            Side.SELL -> bids
        }

        while (remainingQuantity > 0) {
            val bestPrice = oppositeSide.firstPriceOrNull() ?: break
            if (!order.crosses(bestPrice)) break

            val restingOrders = oppositeSide.getValue(bestPrice)
            val restingOrder = restingOrders.first()
            val tradedQuantity = min(remainingQuantity, restingOrder.quantity.value)

            events += order.tradeEvent(restingOrder, Quantity(tradedQuantity))
            remainingQuantity -= tradedQuantity

            val restingQuantity = restingOrder.quantity.value - tradedQuantity
            if (restingQuantity == 0L) {
                restingOrders.removeFirst()
                if (restingOrders.isEmpty()) {
                    oppositeSide.remove(bestPrice)
                }
            } else {
                restingOrders.removeFirst()
                restingOrders.addFirst(restingOrder.copy(quantity = Quantity(restingQuantity)))
            }
        }

        if (remainingQuantity > 0) {
            val restingOrder = order.copy(quantity = Quantity(remainingQuantity))
            addRestingOrder(restingOrder)
            events += OrderRested(restingOrder)
        }

        return events
    }

    fun addRestingOrder(order: LimitOrder) {
        require(order.symbol == symbol) {
            "Cannot add order for ${order.symbol} to book for $symbol"
        }

        val side = when (order.side) {
            Side.BUY -> bids
            Side.SELL -> asks
        }

        side.getOrPut(order.price) { ArrayDeque() }.addLast(order)
    }

    fun bestBid(): LimitOrder? = bids.bestOrder()

    fun bestAsk(): LimitOrder? = asks.bestOrder()

    fun snapshot(): BookSnapshot = BookSnapshot(
        symbol = symbol,
        bids = bids.toPriceLevels(),
        asks = asks.toPriceLevels(),
    )

    private fun SortedMap<Price, ArrayDeque<LimitOrder>>.bestOrder(): LimitOrder? =
        if (isEmpty()) null else get(firstKey())?.firstOrNull()

    private fun SortedMap<Price, ArrayDeque<LimitOrder>>.firstPriceOrNull(): Price? =
        if (isEmpty()) null else firstKey()

    private fun SortedMap<Price, ArrayDeque<LimitOrder>>.toPriceLevels(): List<PriceLevel> =
        entries.map { (price, orders) ->
            PriceLevel(
                price = price,
                orders = orders.toList(),
            )
        }

    private fun LimitOrder.crosses(restingPrice: Price): Boolean =
        when (side) {
            Side.BUY -> price >= restingPrice
            Side.SELL -> price <= restingPrice
        }

    private fun LimitOrder.tradeEvent(
        restingOrder: LimitOrder,
        quantity: Quantity,
    ): TradeExecuted =
        when (side) {
            Side.BUY -> TradeExecuted(
                symbol = symbol,
                price = restingOrder.price,
                quantity = quantity,
                buyOrderId = orderId,
                sellOrderId = restingOrder.orderId,
            )
            Side.SELL -> TradeExecuted(
                symbol = symbol,
                price = restingOrder.price,
                quantity = quantity,
                buyOrderId = restingOrder.orderId,
                sellOrderId = orderId,
            )
        }
}

data class BookSnapshot(
    val symbol: Symbol,
    val bids: List<PriceLevel>,
    val asks: List<PriceLevel>,
)

data class PriceLevel(
    val price: Price,
    val orders: List<LimitOrder>,
) {
    init {
        require(orders.isNotEmpty()) { "Price level must contain at least one order" }
    }

    val totalQuantity: Quantity = Quantity(orders.sumOf { it.quantity.value })
}
