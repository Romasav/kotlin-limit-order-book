package com.kavun.orderbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IdentifiersTest {
    @Test
    fun `symbol input is trimmed and normalized`() {
        assertEquals(Symbol("ACME"), Symbol.of(" acme "))
    }

    @Test
    fun `symbol rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            Symbol.of("   ")
        }
    }

    @Test
    fun `symbol rejects unsupported characters`() {
        assertFailsWith<IllegalArgumentException> {
            Symbol.of("AC ME")
        }
    }

    @Test
    fun `order id input is trimmed`() {
        assertEquals(OrderId("order-1"), OrderId.of(" order-1 "))
    }

    @Test
    fun `order id rejects blank input`() {
        assertFailsWith<IllegalArgumentException> {
            OrderId.of("   ")
        }
    }

    @Test
    fun `price must be positive`() {
        assertEquals(Price(100), Price(100))

        assertFailsWith<IllegalArgumentException> {
            Price(0)
        }
        assertFailsWith<IllegalArgumentException> {
            Price(-1)
        }
    }

    @Test
    fun `quantity must be positive`() {
        assertEquals(Quantity(10), Quantity(10))

        assertFailsWith<IllegalArgumentException> {
            Quantity(0)
        }
        assertFailsWith<IllegalArgumentException> {
            Quantity(-1)
        }
    }

    @Test
    fun `price and quantity are comparable`() {
        assertTrue(Price(101) > Price(100))
        assertTrue(Quantity(11) > Quantity(10))
    }
}
