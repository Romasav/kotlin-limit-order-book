package com.kavun.orderbook.domain

sealed interface Command {
    val symbol: Symbol
}

sealed interface OrderCommand : Command {
    val orderId: OrderId
}

data class PlaceLimitOrder(
    override val orderId: OrderId,
    override val symbol: Symbol,
    val side: Side,
    val quantity: Quantity,
    val price: Price,
) : OrderCommand {
    fun toOrder(): LimitOrder = LimitOrder(
        orderId = orderId,
        symbol = symbol,
        side = side,
        quantity = quantity,
        price = price,
    )
}

data class PlaceMarketOrder(
    override val orderId: OrderId,
    override val symbol: Symbol,
    val side: Side,
    val quantity: Quantity,
) : OrderCommand {
    fun toOrder(): MarketOrder = MarketOrder(
        orderId = orderId,
        symbol = symbol,
        side = side,
        quantity = quantity,
    )
}

data class CancelOrder(
    override val orderId: OrderId,
    override val symbol: Symbol,
) : OrderCommand

data class AmendOrder(
    override val orderId: OrderId,
    override val symbol: Symbol,
    val newQuantity: Quantity? = null,
    val newPrice: Price? = null,
) : OrderCommand {
    init {
        require(newQuantity != null || newPrice != null) {
            "AmendOrder must include a new quantity, a new price, or both"
        }
    }
}
