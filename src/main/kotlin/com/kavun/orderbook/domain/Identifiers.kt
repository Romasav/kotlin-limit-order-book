package com.kavun.orderbook.domain

import java.util.Locale

@JvmInline
value class Symbol(val value: String) {
    init {
        require(value.matches(PATTERN)) {
            "Symbol must start with a letter and contain only uppercase letters, digits, '.', '_', or '-'"
        }
    }

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("[A-Z][A-Z0-9._-]{0,15}")

        fun of(raw: String): Symbol = Symbol(raw.trim().uppercase(Locale.ROOT))
    }
}

@JvmInline
value class OrderId(val value: String) {
    init {
        require(value.matches(PATTERN)) {
            "OrderId must start with a letter or digit and contain only letters, digits, '.', '_', ':', or '-'"
        }
    }

    override fun toString(): String = value

    companion object {
        private val PATTERN = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,63}")

        fun of(raw: String): OrderId = OrderId(raw.trim())
    }
}

enum class Side {
    BUY,
    SELL,
}

@JvmInline
value class Price(val value: Long) : Comparable<Price> {
    init {
        require(value > 0) { "Price must be greater than zero" }
    }

    override fun compareTo(other: Price): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()
}

@JvmInline
value class Quantity(val value: Long) : Comparable<Quantity> {
    init {
        require(value > 0) { "Quantity must be greater than zero" }
    }

    override fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    override fun toString(): String = value.toString()
}
