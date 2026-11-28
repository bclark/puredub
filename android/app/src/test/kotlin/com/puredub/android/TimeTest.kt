package com.puredub.android

import org.junit.Assert.*
import org.junit.Test

class TimeTest {
    @Test fun measuredTimesValidateAndRoundConsistently() {
        assertEquals(62.5, parseTime("1:02.500")!!, 0.000001)
        assertEquals("1:02.500", formatTime(62.5))
        assertEquals("60:00.000", formatTime(3600.0))
        listOf("", "NaN", "inf", "-1", "1:60", "1.5:20", "1e308:00").forEach { assertNull(it, parseTime(it)) }
    }
}
