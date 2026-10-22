package com.kira.adrianpet.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatestForegroundAppSelectorTest {
    @Test
    fun `newest foreground event wins even when input is unsorted`() {
        val events = listOf(
            AppActivityEvent("com.tencent.mm", true, 9L),
            AppActivityEvent("com.openai.chatgpt", true, 3L),
            AppActivityEvent("com.xingin.xhs", false, 12L),
        )

        assertEquals("com.tencent.mm", LatestForegroundAppSelector.select(events))
    }

    @Test
    fun `background blank and missing events stay quiet`() {
        assertNull(
            LatestForegroundAppSelector.select(
                listOf(
                    AppActivityEvent("com.openai.chatgpt", false, 3L),
                    AppActivityEvent("", true, 4L),
                ),
            ),
        )
        assertNull(LatestForegroundAppSelector.select(emptyList()))
    }
}
