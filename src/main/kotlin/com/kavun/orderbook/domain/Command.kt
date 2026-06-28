package com.kavun.orderbook.domain

sealed interface Command {
    val symbol: Symbol
}

data class PlaceLimitOrder(
    val orderId: OrderId,
    override val symbol: Symbol,
    val side: Side,
    val quantity: Quantity,
    val price: Price,
) : Command {
    fun toOrder(): LimitOrder = LimitOrder(
        orderId = orderId,
        symbol = symbol,
        side = side,
        quantity = quantity,
        price = price,
    )
}

data class PlaceMarketOrder(
    val orderId: OrderId,
    override val symbol: Symbol,
    val side: Side,
    val quantity: Quantity,
) : Command {
    fun toOrder(): MarketOrder = MarketOrder(
        orderId = orderId,
        symbol = symbol,
        side = side,
        quantity = quantity,
    )
}

data class CancelOrder(
    val orderId: OrderId,
    override val symbol: Symbol,
) : Command

data class AmendOrder(
    val orderId: OrderId,
    override val symbol: Symbol,
    val newQuantity: Quantity? = null,
    val newPrice: Price? = null,
) : Command {
    init {
        require(newQuantity != null || newPrice != null) {
            "AmendOrder must include a new quantity, a new price, or both"
        }
    }
}
