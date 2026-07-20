package com.kavun.orderbook.engine

import com.kavun.orderbook.book.BookSnapshot
import com.kavun.orderbook.domain.AmendOrder
import com.kavun.orderbook.domain.CancelOrder
import com.kavun.orderbook.domain.Command
import com.kavun.orderbook.domain.Event
import com.kavun.orderbook.domain.OrderAccepted
import com.kavun.orderbook.domain.OrderAmended
import com.kavun.orderbook.domain.OrderCancelled
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
    fun `market buy partially fills the best ask`() {
        val engine = MatchingEngine(acme)
        val restingSell = limitOrder("sell-1", Side.SELL, quantity = 10, price = 100)
        val marketBuy = marketOrder("buy-1", Side.BUY, quantity = 4)

        engine.process(restingSell)

        assertEquals(
            listOf(
                OrderAccepted(marketBuy.toOrder()),
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(4),
                    buyOrderId = marketBuy.orderId,
                    sellOrderId = restingSell.orderId,
                ),
            ),
            engine.process(marketBuy),
        )
        assertEquals(
            restingSell.toOrder().copy(quantity = Quantity(6)),
            engine.snapshot().asks.single().orders.single(),
        )
        assertEquals(emptyList(), engine.snapshot().bids)
    }

    @Test
    fun `market sell consumes bids in price time priority`() {
        val engine = MatchingEngine(acme)
        val bestBid = limitOrder("buy-1", Side.BUY, quantity = 2, price = 101)
        val nextBid = limitOrder("buy-2", Side.BUY, quantity = 3, price = 100)
        val marketSell = marketOrder("sell-1", Side.SELL, quantity = 4)

        engine.process(nextBid)
        engine.process(bestBid)

        assertEquals(
            listOf(
                OrderAccepted(marketSell.toOrder()),
                TradeExecuted(
                    symbol = acme,
                    price = Price(101),
                    quantity = Quantity(2),
                    buyOrderId = bestBid.orderId,
                    sellOrderId = marketSell.orderId,
                ),
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(2),
                    buyOrderId = nextBid.orderId,
                    sellOrderId = marketSell.orderId,
                ),
            ),
            engine.process(marketSell),
        )
        assertEquals(
            nextBid.toOrder().copy(quantity = Quantity(1)),
            engine.snapshot().bids.single().orders.single(),
        )
        assertEquals(emptyList(), engine.snapshot().asks)
    }

    @Test
    fun `unfilled market quantity is cancelled when liquidity is insufficient`() {
        val engine = MatchingEngine(acme)
        val restingSell = limitOrder("sell-1", Side.SELL, quantity = 3, price = 100)
        val marketBuy = marketOrder("buy-1", Side.BUY, quantity = 5)
        val cancelledRemainder = marketBuy.toOrder().copy(quantity = Quantity(2))

        engine.process(restingSell)

        assertEquals(
            listOf(
                OrderAccepted(marketBuy.toOrder()),
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(3),
                    buyOrderId = marketBuy.orderId,
                    sellOrderId = restingSell.orderId,
                ),
                OrderCancelled(cancelledRemainder),
            ),
            engine.process(marketBuy),
        )
        assertEquals(emptyList(), engine.snapshot().bids)
        assertEquals(emptyList(), engine.snapshot().asks)
    }

    @Test
    fun `cancelling an open order removes it from the book`() {
        val engine = MatchingEngine(acme)
        val restingBuy = limitOrder("buy-1", Side.BUY, quantity = 5, price = 100)

        engine.process(restingBuy)

        assertEquals(
            listOf(OrderCancelled(restingBuy.toOrder())),
            engine.process(CancelOrder(orderId = restingBuy.orderId, symbol = acme)),
        )
        assertEquals(emptyList(), engine.snapshot().bids)
    }

    @Test
    fun `cancel for an order that is not open is rejected`() {
        val engine = MatchingEngine(acme)
        val command = CancelOrder(orderId = OrderId.of("missing-1"), symbol = acme)

        assertEquals(
            listOf(
                OrderRejected(
                    symbol = acme,
                    orderId = command.orderId,
                    reason = "Order id missing-1 is not open",
                ),
            ),
            engine.process(command),
        )
    }

    @Test
    fun `amend for an order that is not open is rejected`() {
        val engine = MatchingEngine(acme)
        val command = AmendOrder(
            orderId = OrderId.of("missing-1"),
            symbol = acme,
            newQuantity = Quantity(2),
        )

        assertEquals(
            listOf(
                OrderRejected(
                    symbol = acme,
                    orderId = command.orderId,
                    reason = "Order id missing-1 is not open",
                ),
            ),
            engine.process(command),
        )
    }

    @Test
    fun `increasing order quantity moves it behind existing orders at the same price`() {
        val engine = MatchingEngine(acme)
        val firstBuy = limitOrder("buy-1", Side.BUY, quantity = 2, price = 100)
        val secondBuy = limitOrder("buy-2", Side.BUY, quantity = 2, price = 100)
        val amendedFirstBuy = firstBuy.toOrder().copy(quantity = Quantity(3))

        engine.process(firstBuy)
        engine.process(secondBuy)

        assertEquals(
            listOf(OrderAmended(amendedFirstBuy), OrderRested(amendedFirstBuy)),
            engine.process(
                AmendOrder(
                    orderId = firstBuy.orderId,
                    symbol = acme,
                    newQuantity = Quantity(3),
                ),
            ),
        )
        assertEquals(
            listOf(secondBuy.toOrder(), amendedFirstBuy),
            engine.snapshot().bids.single().orders,
        )

        val marketSell = marketOrder("sell-1", Side.SELL, quantity = 2)
        val trade = engine.process(marketSell).filterIsInstance<TradeExecuted>().single()
        assertEquals(secondBuy.orderId, trade.buyOrderId)
    }

    @Test
    fun `reducing order quantity keeps its existing time priority`() {
        val engine = MatchingEngine(acme)
        val firstBuy = limitOrder("buy-1", Side.BUY, quantity = 3, price = 100)
        val secondBuy = limitOrder("buy-2", Side.BUY, quantity = 2, price = 100)
        val amendedFirstBuy = firstBuy.toOrder().copy(quantity = Quantity(1))

        engine.process(firstBuy)
        engine.process(secondBuy)
        assertEquals(
            listOf(OrderAmended(amendedFirstBuy)),
            engine.process(
                AmendOrder(
                    orderId = firstBuy.orderId,
                    symbol = acme,
                    newQuantity = Quantity(1),
                ),
            ),
        )

        assertEquals(
            listOf(amendedFirstBuy, secondBuy.toOrder()),
            engine.snapshot().bids.single().orders,
        )
    }

    @Test
    fun `amending price can cross the book and rest only the remainder`() {
        val engine = MatchingEngine(acme)
        val restingBuy = limitOrder("buy-1", Side.BUY, quantity = 5, price = 99)
        val restingSell = limitOrder("sell-1", Side.SELL, quantity = 2, price = 101)
        val amendedBuy = restingBuy.toOrder().copy(price = Price(101))
        val remainingBuy = amendedBuy.copy(quantity = Quantity(3))

        engine.process(restingBuy)
        engine.process(restingSell)

        assertEquals(
            listOf(
                OrderAmended(amendedBuy),
                TradeExecuted(
                    symbol = acme,
                    price = Price(101),
                    quantity = Quantity(2),
                    buyOrderId = restingBuy.orderId,
                    sellOrderId = restingSell.orderId,
                ),
                OrderRested(remainingBuy),
            ),
            engine.process(
                AmendOrder(
                    orderId = restingBuy.orderId,
                    symbol = acme,
                    newPrice = Price(101),
                ),
            ),
        )
        assertEquals(listOf(remainingBuy), engine.snapshot().bids.single().orders)
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

    private fun marketOrder(
        id: String,
        side: Side,
        quantity: Long,
    ): PlaceMarketOrder = PlaceMarketOrder(
        orderId = OrderId.of(id),
        symbol = acme,
        side = side,
        quantity = Quantity(quantity),
    )
}
