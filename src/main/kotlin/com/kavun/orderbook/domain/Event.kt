package com.kavun.orderbook.domain

sealed interface Event

data class OrderAccepted(
    val order: Order,
) : Event

data class OrderRejected(
    val symbol: Symbol?,
    val orderId: OrderId?,
    val reason: String,
) : Event {
    init {
        require(reason.isNotBlank()) { "Rejection reason must not be blank" }
    }
}

data class TradeExecuted(
    val symbol: Symbol,
    val price: Price,
    val quantity: Quantity,
    val buyOrderId: OrderId,
    val sellOrderId: OrderId,
) : Event

data class OrderRested(
    val restingOrder: LimitOrder,
) : Event

data class OrderCancelled(
    val cancelledOrder: LimitOrder,
) : Event

data class OrderAmended(
    val amendedOrder: LimitOrder,
) : Event
