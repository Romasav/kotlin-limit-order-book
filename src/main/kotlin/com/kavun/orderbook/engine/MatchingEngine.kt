package com.kavun.orderbook.engine

import com.kavun.orderbook.book.BookSnapshot
import com.kavun.orderbook.book.LimitOrderBook
import com.kavun.orderbook.domain.AmendOrder
import com.kavun.orderbook.domain.CancelOrder
import com.kavun.orderbook.domain.Command
import com.kavun.orderbook.domain.Event
import com.kavun.orderbook.domain.OrderAccepted
import com.kavun.orderbook.domain.OrderCommand
import com.kavun.orderbook.domain.OrderId
import com.kavun.orderbook.domain.OrderRejected
import com.kavun.orderbook.domain.PlaceLimitOrder
import com.kavun.orderbook.domain.PlaceMarketOrder
import com.kavun.orderbook.domain.Symbol

class MatchingEngine(
    val symbol: Symbol,
) {
    private val book = LimitOrderBook(symbol)
    private val acceptedOrderIds = mutableSetOf<OrderId>()

    fun process(command: Command): List<Event> =
        when (command) {
            is OrderCommand -> processOrderCommand(command)
        }

    fun snapshot(): BookSnapshot = book.snapshot()

    private fun processOrderCommand(command: OrderCommand): List<Event> {
        if (command.symbol != symbol) {
            return reject(command, "Engine handles $symbol, not ${command.symbol}")
        }

        return when (command) {
            is PlaceLimitOrder -> processLimitOrder(command)
            is PlaceMarketOrder -> processMarketOrder(command)
            is CancelOrder -> processCancelOrder(command)
            is AmendOrder -> processAmendOrder(command)
        }
    }

    private fun processLimitOrder(command: PlaceLimitOrder): List<Event> {
        rejectDuplicateOrderId(command)?.let { return it }

        val order = command.toOrder()
        return listOf(OrderAccepted(order)) + book.placeLimitOrder(order)
    }

    private fun processMarketOrder(command: PlaceMarketOrder): List<Event> {
        rejectDuplicateOrderId(command)?.let { return it }

        val order = command.toOrder()
        return listOf(OrderAccepted(order)) + book.placeMarketOrder(order)
    }

    private fun processCancelOrder(command: CancelOrder): List<Event> {
        val cancellation = book.cancelOrder(command.orderId)
            ?: return reject(command, "Order id ${command.orderId} is not open")

        return listOf(cancellation)
    }

    private fun processAmendOrder(command: AmendOrder): List<Event> =
        book.amendOrder(command.orderId, command.newQuantity, command.newPrice)
            ?: reject(command, "Order id ${command.orderId} is not open")

    private fun rejectDuplicateOrderId(command: OrderCommand): List<Event>? {
        if (acceptedOrderIds.add(command.orderId)) return null
        return reject(command, "Order id ${command.orderId} has already been used")
    }

    private fun reject(command: OrderCommand, reason: String): List<Event> =
        listOf(OrderRejected(symbol = command.symbol, orderId = command.orderId, reason = reason))
}
