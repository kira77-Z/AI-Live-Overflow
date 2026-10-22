package com.kira.adrianpet.render

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class RenderEventTest {
    @Test
    fun `all states serialize to the renderer contract`() {
        PetState.entries.forEach { state ->
            val json = JSONObject(RenderEvent(state = state).toJson())

            assertEquals(state.wireName, json.getString("state"))
            assertEquals(4_000L, json.getLong("durationMs"))
            assertFalse(json.getBoolean("perch"))
            assertTrue(json.isNull("bubble"))
        }
        assertEquals(7, PetState.entries.size)
    }

    @Test
    fun `bubble is limited to 36 unicode characters`() {
        val input = "🐾".repeat(40)
        val json = JSONObject(RenderEvent(PetState.TAP, bubble = input).toJson())

        assertEquals("🐾".repeat(35) + "…", json.getString("bubble"))
        assertEquals(36, json.getString("bubble").codePointCount(0, json.getString("bubble").length))
    }

    @Test
    fun `perch layout flag is independent from animation state`() {
        val json = JSONObject(RenderEvent(PetState.RAPID_TAP, perch = true).toJson())

        assertEquals(PetState.RAPID_TAP.wireName, json.getString("state"))
        assertTrue(json.getBoolean("perch"))
    }

    @Test
    fun `quotes and newlines stay JSON data`() {
        val attack = "');\nwindow.evil = true;//"
        val jsonText = RenderEvent(PetState.RAPID_TAP, bubble = attack).toJson()
        val parsed = JSONObject(jsonText)

        assertEquals(attack, parsed.getString("bubble"))
        assertTrue(jsonText.contains("\\n"))
        assertFalse(jsonText.contains("window.petEngine"))
    }
}
