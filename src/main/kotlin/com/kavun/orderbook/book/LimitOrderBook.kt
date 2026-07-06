package com.kavun.orderbook.book

import com.kavun.orderbook.domain.LimitOrder
import com.kavun.orderbook.domain.Price
import com.kavun.orderbook.domain.Quantity
import com.kavun.orderbook.domain.Side
import com.kavun.orderbook.domain.Symbol
import java.util.SortedMap

class LimitOrderBook(
    val symbol: Symbol,
) {
    private val bids = sortedMapOf<Price, ArrayDeque<LimitOrder>>(compareByDescending<Price> { it.value })
    private val asks = sortedMapOf<Price, ArrayDeque<LimitOrder>>()

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

    private fun SortedMap<Price, ArrayDeque<LimitOrder>>.toPriceLevels(): List<PriceLevel> =
        entries.map { (price, orders) ->
            PriceLevel(
                price = price,
                orders = orders.toList(),
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
