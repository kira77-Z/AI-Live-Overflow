package com.kira.adrianpet.overlay

import com.kira.adrianpet.data.PetPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OverlayPositionTest {
    private val portraitBounds = OverlayBounds(left = 0, top = 80, right = 1080, bottom = 2320)
    private val petSize = OverlaySize(width = 180, height = 240)

    @Test
    fun `negative and off-screen coordinates clamp into safe bounds`() {
        assertEquals(
            OverlayPosition(0, 80),
            OverlayPosition(-400, -20).clamp(portraitBounds, petSize),
        )
        assertEquals(
            OverlayPosition(900, 2080),
            OverlayPosition(4_000, 9_000).clamp(portraitBounds, petSize),
        )
    }

    @Test
    fun `landscape coordinates remain usable when returning to portrait`() {
        val landscapePosition = OverlayPosition(2_100, 700)

        assertEquals(OverlayPosition(900, 700), landscapePosition.clamp(portraitBounds, petSize))
    }

    @Test
    fun `edge snap chooses nearest horizontal safe edge`() {
        assertEquals(OverlayPosition(0, 600), OverlayPosition(230, 600).snapToEdge(portraitBounds, petSize))
        assertEquals(OverlayPosition(900, 600), OverlayPosition(700, 600).snapToEdge(portraitBounds, petSize))
    }

    @Test
    fun `normal and perch positions use distinct preference keys`() {
        assertNotEquals(PetPreferences.NORMAL_X.name, PetPreferences.PERCH_X.name)
        assertNotEquals(PetPreferences.NORMAL_Y.name, PetPreferences.PERCH_Y.name)
    }
}
