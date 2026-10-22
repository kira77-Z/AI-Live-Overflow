package com.kira.adrianpet.behavior

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSwitchGateTest {
    private val gate = AppSwitchGate(2, 12_000L)

    @Test
    fun `an app must be observed twice before it is announced`() {
        assertNull(gate.observe("com.openai.chatgpt", 0L))
        assertEquals("com.openai.chatgpt", gate.observe("com.openai.chatgpt", 3_000L))
    }

    @Test
    fun `the active app is not announced repeatedly`() {
        gate.observe("com.openai.chatgpt", 0L)
        gate.observe("com.openai.chatgpt", 3_000L)

        assertNull(gate.observe("com.openai.chatgpt", 6_000L))
        assertNull(gate.observe("com.openai.chatgpt", 18_000L))
    }

    @Test
    fun `an unstable one-sample app switch stays quiet`() {
        assertNull(gate.observe("com.openai.chatgpt", 0L))
        assertNull(gate.observe("com.tencent.mm", 3_000L))
        assertNull(gate.observe("com.openai.chatgpt", 6_000L))
    }

    @Test
    fun `a stable switch waits for the global cooldown then announces`() {
        gate.observe("com.openai.chatgpt", 0L)
        gate.observe("com.openai.chatgpt", 3_000L)
        gate.observe("com.tencent.mm", 6_000L)

        assertNull(gate.observe("com.tencent.mm", 9_000L))
        assertEquals("com.tencent.mm", gate.observe("com.tencent.mm", 15_000L))
    }

    @Test
    fun `system and pet packages never announce`() {
        listOf(
            "com.kira.adrianpet",
            "com.android.systemui",
            "com.android.settings",
        ).forEach { packageName ->
            assertNull(gate.observe(packageName, 0L))
            assertNull(gate.observe(packageName, 3_000L))
        }
    }
}
