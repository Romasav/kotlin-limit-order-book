package com.kavun.orderbook.book

import com.kavun.orderbook.domain.LimitOrder
import com.kavun.orderbook.domain.OrderId
import com.kavun.orderbook.domain.OrderRested
import com.kavun.orderbook.domain.Price
import com.kavun.orderbook.domain.Quantity
import com.kavun.orderbook.domain.Side
import com.kavun.orderbook.domain.Symbol
import com.kavun.orderbook.domain.TradeExecuted
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LimitOrderBookTest {
    private val acme = Symbol.of("ACME")

    @Test
    fun `empty book has no best bid or ask`() {
        val book = LimitOrderBook(acme)

        assertNull(book.bestBid())
        assertNull(book.bestAsk())
        assertEquals(BookSnapshot(symbol = acme, bids = emptyList(), asks = emptyList()), book.snapshot())
    }

    @Test
    fun `bids are stored from highest price to lowest price`() {
        val book = LimitOrderBook(acme)
        val lowBid = limitOrder("buy-1", Side.BUY, price = 100, quantity = 10)
        val highBid = limitOrder("buy-2", Side.BUY, price = 102, quantity = 5)
        val middleBid = limitOrder("buy-3", Side.BUY, price = 101, quantity = 7)

        book.addRestingOrder(lowBid)
        book.addRestingOrder(highBid)
        book.addRestingOrder(middleBid)

        assertEquals(highBid, book.bestBid())
        assertEquals(
            listOf(
                PriceLevel(price = Price(102), orders = listOf(highBid)),
                PriceLevel(price = Price(101), orders = listOf(middleBid)),
                PriceLevel(price = Price(100), orders = listOf(lowBid)),
            ),
            book.snapshot().bids,
        )
    }

    @Test
    fun `asks are stored from lowest price to highest price`() {
        val book = LimitOrderBook(acme)
        val highAsk = limitOrder("sell-1", Side.SELL, price = 103, quantity = 4)
        val lowAsk = limitOrder("sell-2", Side.SELL, price = 101, quantity = 6)
        val middleAsk = limitOrder("sell-3", Side.SELL, price = 102, quantity = 8)

        book.addRestingOrder(highAsk)
        book.addRestingOrder(lowAsk)
        book.addRestingOrder(middleAsk)

        assertEquals(lowAsk, book.bestAsk())
        assertEquals(
            listOf(
                PriceLevel(price = Price(101), orders = listOf(lowAsk)),
                PriceLevel(price = Price(102), orders = listOf(middleAsk)),
                PriceLevel(price = Price(103), orders = listOf(highAsk)),
            ),
            book.snapshot().asks,
        )
    }

    @Test
    fun `orders at the same bid price keep FIFO priority`() {
        val book = LimitOrderBook(acme)
        val firstBid = limitOrder("buy-1", Side.BUY, price = 100, quantity = 10)
        val secondBid = limitOrder("buy-2", Side.BUY, price = 100, quantity = 5)

        book.addRestingOrder(firstBid)
        book.addRestingOrder(secondBid)

        assertEquals(firstBid, book.bestBid())
        assertEquals(
            PriceLevel(price = Price(100), orders = listOf(firstBid, secondBid)),
            book.snapshot().bids.single(),
        )
    }

    @Test
    fun `orders at the same ask price keep FIFO priority`() {
        val book = LimitOrderBook(acme)
        val firstAsk = limitOrder("sell-1", Side.SELL, price = 101, quantity = 3)
        val secondAsk = limitOrder("sell-2", Side.SELL, price = 101, quantity = 9)

        book.addRestingOrder(firstAsk)
        book.addRestingOrder(secondAsk)

        assertEquals(firstAsk, book.bestAsk())
        assertEquals(
            PriceLevel(price = Price(101), orders = listOf(firstAsk, secondAsk)),
            book.snapshot().asks.single(),
        )
    }

    @Test
    fun `snapshot includes both sides and total quantity per price level`() {
        val book = LimitOrderBook(acme)
        val firstBid = limitOrder("buy-1", Side.BUY, price = 100, quantity = 10)
        val secondBid = limitOrder("buy-2", Side.BUY, price = 100, quantity = 5)
        val ask = limitOrder("sell-1", Side.SELL, price = 101, quantity = 7)

        book.addRestingOrder(firstBid)
        book.addRestingOrder(secondBid)
        book.addRestingOrder(ask)

        val snapshot = book.snapshot()

        assertEquals(acme, snapshot.symbol)
        assertEquals(Quantity(15), snapshot.bids.single().totalQuantity)
        assertEquals(Quantity(7), snapshot.asks.single().totalQuantity)
    }

    @Test
    fun `book rejects orders for another symbol`() {
        val book = LimitOrderBook(acme)
        val otherSymbolOrder = LimitOrder(
            orderId = OrderId.of("buy-1"),
            symbol = Symbol.of("FOO"),
            side = Side.BUY,
            quantity = Quantity(10),
            price = Price(100),
        )

        assertFailsWith<IllegalArgumentException> {
            book.addRestingOrder(otherSymbolOrder)
        }
    }

    @Test
    fun `incoming buy limit order matches best ask`() {
        val book = LimitOrderBook(acme)
        val restingAsk = limitOrder("sell-1", Side.SELL, price = 100, quantity = 6)
        val incomingBuy = limitOrder("buy-1", Side.BUY, price = 101, quantity = 6)

        book.addRestingOrder(restingAsk)

        assertEquals(
            listOf(
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(6),
                    buyOrderId = incomingBuy.orderId,
                    sellOrderId = restingAsk.orderId,
                ),
            ),
            book.placeLimitOrder(incomingBuy),
        )
        assertNull(book.bestBid())
        assertNull(book.bestAsk())
    }

    @Test
    fun `incoming sell limit order matches best bid`() {
        val book = LimitOrderBook(acme)
        val restingBid = limitOrder("buy-1", Side.BUY, price = 102, quantity = 4)
        val incomingSell = limitOrder("sell-1", Side.SELL, price = 101, quantity = 4)

        book.addRestingOrder(restingBid)

        assertEquals(
            listOf(
                TradeExecuted(
                    symbol = acme,
                    price = Price(102),
                    quantity = Quantity(4),
                    buyOrderId = restingBid.orderId,
                    sellOrderId = incomingSell.orderId,
                ),
            ),
            book.placeLimitOrder(incomingSell),
        )
        assertNull(book.bestBid())
        assertNull(book.bestAsk())
    }

    @Test
    fun `incoming non crossing limit order rests without trades`() {
        val book = LimitOrderBook(acme)
        val restingAsk = limitOrder("sell-1", Side.SELL, price = 105, quantity = 3)
        val incomingBuy = limitOrder("buy-1", Side.BUY, price = 104, quantity = 7)

        book.addRestingOrder(restingAsk)

        assertEquals(
            listOf(OrderRested(incomingBuy)),
            book.placeLimitOrder(incomingBuy),
        )
        assertEquals(incomingBuy, book.bestBid())
        assertEquals(restingAsk, book.bestAsk())
    }

    @Test
    fun `partial fill reduces resting order quantity`() {
        val book = LimitOrderBook(acme)
        val restingAsk = limitOrder("sell-1", Side.SELL, price = 100, quantity = 10)
        val incomingBuy = limitOrder("buy-1", Side.BUY, price = 100, quantity = 4)

        book.addRestingOrder(restingAsk)

        assertEquals(
            listOf(
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(4),
                    buyOrderId = incomingBuy.orderId,
                    sellOrderId = restingAsk.orderId,
                ),
            ),
            book.placeLimitOrder(incomingBuy),
        )
        assertEquals(restingAsk.copy(quantity = Quantity(6)), book.bestAsk())
        assertNull(book.bestBid())
    }

    @Test
    fun `unfilled incoming limit quantity rests after matching`() {
        val book = LimitOrderBook(acme)
        val restingAsk = limitOrder("sell-1", Side.SELL, price = 100, quantity = 4)
        val incomingBuy = limitOrder("buy-1", Side.BUY, price = 101, quantity = 10)
        val remainingBuy = incomingBuy.copy(quantity = Quantity(6))

        book.addRestingOrder(restingAsk)

        assertEquals(
            listOf(
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(4),
                    buyOrderId = incomingBuy.orderId,
                    sellOrderId = restingAsk.orderId,
                ),
                OrderRested(remainingBuy),
            ),
            book.placeLimitOrder(incomingBuy),
        )
        assertEquals(remainingBuy, book.bestBid())
        assertNull(book.bestAsk())
    }

    @Test
    fun `one incoming order can fill multiple resting orders in price time priority`() {
        val book = LimitOrderBook(acme)
        val firstAsk = limitOrder("sell-1", Side.SELL, price = 100, quantity = 5)
        val secondAsk = limitOrder("sell-2", Side.SELL, price = 101, quantity = 7)
        val tooExpensiveAsk = limitOrder("sell-3", Side.SELL, price = 102, quantity = 3)
        val incomingBuy = limitOrder("buy-1", Side.BUY, price = 101, quantity = 10)

        book.addRestingOrder(secondAsk)
        book.addRestingOrder(tooExpensiveAsk)
        book.addRestingOrder(firstAsk)

        assertEquals(
            listOf(
                TradeExecuted(
                    symbol = acme,
                    price = Price(100),
                    quantity = Quantity(5),
                    buyOrderId = incomingBuy.orderId,
                    sellOrderId = firstAsk.orderId,
                ),
                TradeExecuted(
                    symbol = acme,
                    price = Price(101),
                    quantity = Quantity(5),
                    buyOrderId = incomingBuy.orderId,
                    sellOrderId = secondAsk.orderId,
                ),
            ),
            book.placeLimitOrder(incomingBuy),
        )
        assertEquals(
            listOf(
                PriceLevel(price = Price(101), orders = listOf(secondAsk.copy(quantity = Quantity(2)))),
                PriceLevel(price = Price(102), orders = listOf(tooExpensiveAsk)),
            ),
            book.snapshot().asks,
        )
        assertNull(book.bestBid())
    }

    private fun limitOrder(
        id: String,
        side: Side,
        price: Long,
        quantity: Long,
    ): LimitOrder = LimitOrder(
        orderId = OrderId.of(id),
        symbol = acme,
        side = side,
        quantity = Quantity(quantity),
        price = Price(price),
    )
}
