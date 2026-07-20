package com.kavun.orderbook.book

import com.kavun.orderbook.domain.Event
import com.kavun.orderbook.domain.LimitOrder
import com.kavun.orderbook.domain.MarketOrder
import com.kavun.orderbook.domain.Order
import com.kavun.orderbook.domain.OrderAmended
import com.kavun.orderbook.domain.OrderCancelled
import com.kavun.orderbook.domain.OrderId
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
        val result = match(order) { restingPrice -> order.crosses(restingPrice) }

        if (result.remainingQuantity > 0) {
            val restingOrder = order.copy(quantity = Quantity(result.remainingQuantity))
            addRestingOrder(restingOrder)
            result.events += OrderRested(restingOrder)
        }

        return result.events
    }

    fun placeMarketOrder(order: MarketOrder): List<Event> {
        val result = match(order) { true }

        if (result.remainingQuantity > 0) {
            val cancelledRemainder = order.copy(quantity = Quantity(result.remainingQuantity))
            result.events += OrderCancelled(cancelledRemainder)
        }

        return result.events
    }

    fun cancelOrder(orderId: OrderId): OrderCancelled? {
        val location = findRestingOrder(orderId) ?: return null
        return OrderCancelled(removeRestingOrder(location))
    }

    fun amendOrder(
        orderId: OrderId,
        newQuantity: Quantity?,
        newPrice: Price?,
    ): List<Event>? {
        require(newQuantity != null || newPrice != null) {
            "An amendment must include a new quantity, a new price, or both"
        }

        val location = findRestingOrder(orderId) ?: return null
        val currentOrder = location.order
        val amendedOrder = currentOrder.copy(
            quantity = newQuantity ?: currentOrder.quantity,
            price = newPrice ?: currentOrder.price,
        )
        val keepsPriority = amendedOrder.price == currentOrder.price &&
            amendedOrder.quantity <= currentOrder.quantity

        if (keepsPriority) {
            location.orders[location.index] = amendedOrder
            return listOf(OrderAmended(amendedOrder))
        }

        removeRestingOrder(location)
        return listOf(OrderAmended(amendedOrder)) + placeLimitOrder(amendedOrder)
    }

    fun addRestingOrder(order: LimitOrder) {
        requireBookSymbol(order)

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

    private fun match(
        order: Order,
        canTradeAt: (Price) -> Boolean,
    ): MatchResult {
        requireBookSymbol(order)

        var remainingQuantity = order.quantity.value
        val events = mutableListOf<Event>()
        val oppositeSide = when (order.side) {
            Side.BUY -> asks
            Side.SELL -> bids
        }

        while (remainingQuantity > 0) {
            val bestPrice = oppositeSide.firstPriceOrNull() ?: break
            if (!canTradeAt(bestPrice)) break

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
                restingOrders[0] = restingOrder.copy(quantity = Quantity(restingQuantity))
            }
        }

        return MatchResult(events, remainingQuantity)
    }

    private fun findRestingOrder(orderId: OrderId): RestingOrderLocation? =
        bids.findRestingOrder(orderId) ?: asks.findRestingOrder(orderId)

    private fun SortedMap<Price, ArrayDeque<LimitOrder>>.findRestingOrder(
        orderId: OrderId,
    ): RestingOrderLocation? {
        for ((price, orders) in this) {
            val index = orders.indexOfFirst { it.orderId == orderId }
            if (index >= 0) {
                return RestingOrderLocation(this, price, orders, index)
            }
        }
        return null
    }

    private fun removeRestingOrder(location: RestingOrderLocation): LimitOrder {
        val removedOrder = location.orders.removeAt(location.index)
        if (location.orders.isEmpty()) {
            location.side.remove(location.price)
        }
        return removedOrder
    }

    private fun requireBookSymbol(order: Order) {
        require(order.symbol == symbol) {
            "Cannot process order for ${order.symbol} in book for $symbol"
        }
    }

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

    private fun Order.tradeEvent(
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

    private data class MatchResult(
        val events: MutableList<Event>,
        val remainingQuantity: Long,
    )

    private data class RestingOrderLocation(
        val side: SortedMap<Price, ArrayDeque<LimitOrder>>,
        val price: Price,
        val orders: ArrayDeque<LimitOrder>,
        val index: Int,
    ) {
        val order: LimitOrder = orders[index]
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
