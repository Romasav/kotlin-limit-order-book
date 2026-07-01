package com.kavun.orderbook.domain

sealed interface Order {
    val orderId: OrderId
    val symbol: Symbol
    val side: Side
    val quantity: Quantity
}

data class LimitOrder(
    override val orderId: OrderId,
    override val symbol: Symbol,
    override val side: Side,
    override val quantity: Quantity,
    val price: Price,
) : Order

data class MarketOrder(
    override val orderId: OrderId,
    override val symbol: Symbol,
    override val side: Side,
    override val quantity: Quantity,
) : Order
