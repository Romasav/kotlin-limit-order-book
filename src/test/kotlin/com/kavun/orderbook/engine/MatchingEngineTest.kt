package com.kavun.orderbook.engine

import com.kavun.orderbook.book.BookSnapshot
import com.kavun.orderbook.domain.AmendOrder
import com.kavun.orderbook.domain.CancelOrder
import com.kavun.orderbook.domain.Command
import com.kavun.orderbook.domain.Event
import com.kavun.orderbook.domain.OrderAccepted
import com.kavun.orderbook.domain.OrderId
import com.kavun.orderbook.domain.OrderRejected
import com.kavun.orderbook.domain.OrderRested
import com.kavun.orderbook.domain.PlaceLimitOrder
import com.kavun.orderbook.domain.PlaceMarketOrder
import com.kavun.orderbook.domain.Price
import com.kavun.orderbook.domain.Quantity
import com.kavun.orderbook.domain.Side
import com.kavun.orderbook.domain.Symbol
import com.kavun.orderbook.domain.TradeExecuted
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MatchingEngineTest {
    private val acme = Symbol.of("ACME")

    @Test
    fun `accepted limit order emits accepted before rested`() {
        val engine = MatchingEngine(acme)
        val command = limitOrder("buy-1", Side.BUY, quantity = 10, price = 100)
        val order = command.toOrder()

        assertEquals(
            listOf(OrderAccepted(order), OrderRested(order)),
            engine.process(command),
        )
        assertEquals(order, engine.snapshot().bids.single().orders.single())
    }

    @Test
    fun `crossing limit order emits accepted before trade`() {
        val engine = MatchingEngine(acme)
        val restingSell = limitOrder("sell-1", Side.SELL, quantity = 4, price = 100)
        val incomingBuy = limitOrder("buy-1", Side.BUY, quantity = 4, price = 101)

        engine.process(restingSell)

        assertEquals(
            listOf(
                OrderAccepted(incomingBuy.toOrder()),
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(4),
                    buyOrderId = incomingBuy.orderId,
                    sellOrderId = restingSell.orderId,
                ),
            ),
            engine.process(incomingBuy),
        )
        assertNull(engine.snapshot().bids.firstOrNull())
        assertNull(engine.snapshot().asks.firstOrNull())
    }

    @Test
    fun `command for another symbol is rejected without changing the book`() {
        val engine = MatchingEngine(acme)
        val command = PlaceLimitOrder(
            orderId = OrderId.of("buy-1"),
            symbol = Symbol.of("FOO"),
            side = Side.BUY,
            quantity = Quantity(10),
            price = Price(100),
        )

        assertEquals(
            listOf(
                OrderRejected(
                    symbol = command.symbol,
                    orderId = command.orderId,
                    reason = "Engine handles ACME, not FOO",
                ),
            ),
            engine.process(command),
        )
        assertEquals(emptyList(), engine.snapshot().bids)
        assertEquals(emptyList(), engine.snapshot().asks)
    }

    @Test
    fun `duplicate order id is rejected without changing the book`() {
        val engine = MatchingEngine(acme)
        val first = limitOrder("order-1", Side.BUY, quantity = 10, price = 100)
        val duplicate = limitOrder("order-1", Side.SELL, quantity = 3, price = 99)

        engine.process(first)

        assertEquals(
            listOf(
                OrderRejected(
                    symbol = acme,
                    orderId = duplicate.orderId,
                    reason = "Order id order-1 has already been used",
                ),
            ),
            engine.process(duplicate),
        )
        assertEquals(listOf(first.toOrder()), engine.snapshot().bids.single().orders)
        assertEquals(emptyList(), engine.snapshot().asks)
    }

    @Test
    fun `commands reserved for phase 6 are rejected without changing the book`() {
        val engine = MatchingEngine(acme)
        val commands = listOf<Command>(
            PlaceMarketOrder(
                orderId = OrderId.of("market-1"),
                symbol = acme,
                side = Side.BUY,
                quantity = Quantity(5),
            ),
            CancelOrder(
                orderId = OrderId.of("cancel-1"),
                symbol = acme,
            ),
            AmendOrder(
                orderId = OrderId.of("amend-1"),
                symbol = acme,
                newQuantity = Quantity(2),
            ),
        )

        assertEquals(
            listOf(
                listOf(
                    OrderRejected(
                        symbol = acme,
                        orderId = OrderId.of("market-1"),
                        reason = "Market orders are not supported yet",
                    ),
                ),
                listOf(
                    OrderRejected(
                        symbol = acme,
                        orderId = OrderId.of("cancel-1"),
                        reason = "Cancel orders are not supported yet",
                    ),
                ),
                listOf(
                    OrderRejected(
                        symbol = acme,
                        orderId = OrderId.of("amend-1"),
                        reason = "Amend orders are not supported yet",
                    ),
                ),
            ),
            commands.map(engine::process),
        )
        assertEquals(emptyList(), engine.snapshot().bids)
        assertEquals(emptyList(), engine.snapshot().asks)
    }

    @Test
    fun `same command sequence produces the same events and final state`() {
        val commands = listOf(
            limitOrder("sell-1", Side.SELL, quantity = 5, price = 100),
            limitOrder("sell-2", Side.SELL, quantity = 4, price = 101),
            limitOrder("buy-1", Side.BUY, quantity = 7, price = 101),
        )

        val firstRun = execute(commands)
        val secondRun = execute(commands)

        assertEquals(firstRun, secondRun)
    }

    private fun execute(commands: List<Command>): Pair<List<Event>, BookSnapshot> {
        val engine = MatchingEngine(acme)
        val events = commands.flatMap(engine::process)
        return events to engine.snapshot()
    }

    private fun limitOrder(
        id: String,
        side: Side,
        quantity: Long,
        price: Long,
    ): PlaceLimitOrder = PlaceLimitOrder(
        orderId = OrderId.of(id),
        symbol = acme,
        side = side,
        quantity = Quantity(quantity),
        price = Price(price),
    )
}
