package com.kavun.orderbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DomainModelTest {
    private val acme = Symbol.of("ACME")
    private val buyOrderId = OrderId.of("buy-1")
    private val sellOrderId = OrderId.of("sell-1")

    @Test
    fun `place limit order command creates a limit order model`() {
        val command = PlaceLimitOrder(
            orderId = buyOrderId,
            symbol = acme,
            side = Side.BUY,
            quantity = Quantity(10),
            price = Price(100),
        )

        assertEquals(
            LimitOrder(
                orderId = buyOrderId,
                symbol = acme,
                side = Side.BUY,
                quantity = Quantity(10),
                price = Price(100),
            ),
            command.toOrder(),
        )
    }

    @Test
    fun `place market order command creates a market order model`() {
        val command = PlaceMarketOrder(
            orderId = sellOrderId,
            symbol = acme,
            side = Side.SELL,
            quantity = Quantity(5),
        )

        assertEquals(
            MarketOrder(
                orderId = sellOrderId,
                symbol = acme,
                side = Side.SELL,
                quantity = Quantity(5),
            ),
            command.toOrder(),
        )
    }

    @Test
    fun `amend order must include at least one requested change`() {
        assertFailsWith<IllegalArgumentException> {
            AmendOrder(
                orderId = buyOrderId,
                symbol = acme,
            )
        }

        assertEquals(
            AmendOrder(
                orderId = buyOrderId,
                symbol = acme,
                newQuantity = Quantity(7),
            ),
            AmendOrder(
                orderId = buyOrderId,
                symbol = acme,
                newQuantity = Quantity(7),
            ),
        )
    }

    @Test
    fun `events carry strongly typed domain data`() {
        val restingOrder = LimitOrder(
            orderId = buyOrderId,
            symbol = acme,
            side = Side.BUY,
            quantity = Quantity(10),
            price = Price(100),
        )

        assertEquals(OrderAccepted(restingOrder), OrderAccepted(restingOrder))
        assertEquals(OrderRested(restingOrder), OrderRested(restingOrder))
        assertEquals(OrderCancelled(restingOrder), OrderCancelled(restingOrder))
        val marketRemainder = MarketOrder(
            orderId = sellOrderId,
            symbol = acme,
            side = Side.SELL,
            quantity = Quantity(2),
        )
        assertEquals(OrderCancelled(marketRemainder), OrderCancelled(marketRemainder))
        assertEquals(
            OrderAmended(restingOrder.copy(quantity = Quantity(8))),
            OrderAmended(restingOrder.copy(quantity = Quantity(8))),
        )
        assertEquals(
            TradeExecuted(
                symbol = acme,
                price = Price(100),
                quantity = Quantity(5),
                buyOrderId = buyOrderId,
                sellOrderId = sellOrderId,
            ),
            TradeExecuted(
                symbol = acme,
                price = Price(100),
                quantity = Quantity(5),
                buyOrderId = buyOrderId,
                sellOrderId = sellOrderId,
            ),
        )
    }

    @Test
    fun `rejected events must include a reason`() {
        assertEquals(
            OrderRejected(symbol = acme, orderId = buyOrderId, reason = "Unknown order"),
            OrderRejected(symbol = acme, orderId = buyOrderId, reason = "Unknown order"),
        )

        assertFailsWith<IllegalArgumentException> {
            OrderRejected(symbol = acme, orderId = buyOrderId, reason = " ")
        }
    }
}
