package com.starmist.walker.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    @Test fun thousandsAreGrouped() {
        assertEquals("0", 0L.withCommas()); assertEquals("999", 999L.withCommas()); assertEquals("1,000", 1000L.withCommas())
        assertEquals("1,234,567", 1234567L.withCommas()); assertEquals("-12,345", (-12345L).withCommas())
    }

    @Test fun decimalsAreRoundedHalfUp() {
        assertEquals("3.14", 3.14159.twoDecimals()); assertEquals("0.05", 0.05.twoDecimals()); assertEquals("2.00", 2.0.twoDecimals())
        assertEquals("1.3", 1.25.fixed(1)); assertEquals("56", 55.6.fixed(0)); assertEquals("0", 0.4.fixed(0)); assertEquals("-1.5", (-1.5).fixed(1))
        assertEquals("0.00", (-0.001).twoDecimals())
    }
}
