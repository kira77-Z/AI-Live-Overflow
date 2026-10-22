package com.kira.adrianpet.data.remote

import com.kira.adrianpet.render.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetCommandTest {
    @Test
    fun `valid bubble command accepts known state and ignores unknown fields`() {
        val command = PetCommand.parse(
            """{"id":"cmd-1","type":"bubble","state":"tap","text":"在呢","duration_ms":4000,"future_field":true}""",
        )

        assertEquals("cmd-1", command?.id)
        assertEquals(PetState.TAP, command?.state)
        assertEquals("在呢", command?.text)
    }

    @Test
    fun `only bubble type and known states pass`() {
        assertNull(PetCommand.parse("""{"id":"1","type":"sound","state":"tap","text":"x"}"""))
        assertNull(PetCommand.parse("""{"id":"1","type":"bubble","state":"explode","text":"x"}"""))
    }

    @Test
    fun `text must be nonblank and is capped at 36 unicode characters`() {
        assertNull(PetCommand.parse("""{"id":"1","type":"bubble","state":"idle","text":"   "}"""))
        val command = PetCommand.parse(
            """{"id":"2","type":"bubble","state":"idle","text":"${"🐾".repeat(40)}"}""",
        )
        assertEquals(36, command?.text?.codePointCount(0, command.text.length))
        assertTrue(command?.text?.endsWith("…") == true)
    }

    @Test
    fun `duration is clamped to safe bounds`() {
        assertEquals(500L, PetCommand.parse("""{"id":"1","type":"bubble","state":"idle","text":"x","duration_ms":1}""")?.durationMs)
        assertEquals(15_000L, PetCommand.parse("""{"id":"2","type":"bubble","state":"idle","text":"x","duration_ms":999999}""")?.durationMs)
        assertTrue(PetCommand.parse("""{"id":"3","type":"bubble","state":"idle","text":"x"}""")?.durationMs == 4_000L)
    }
}
