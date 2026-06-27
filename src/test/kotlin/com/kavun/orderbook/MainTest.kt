package com.kavun.orderbook

import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun `application name describes the project`() {
        assertEquals("Kotlin Limit Order Book", applicationName())
    }
}
