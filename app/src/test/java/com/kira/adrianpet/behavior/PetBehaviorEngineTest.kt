package com.kira.adrianpet.behavior

import com.kira.adrianpet.gesture.GestureResult
import com.kira.adrianpet.render.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PetBehaviorEngineTest {
    private val engine = PetBehaviorEngine(BubbleCatalog { 0 })

    @Test
    fun `tap rapid tap and drag end each produce a four second bubble`() {
        listOf(GestureResult.Tap, GestureResult.RapidTap, GestureResult.DragEnd).forEach { gesture ->
            val event = engine.onGesture(gesture)
            assertNotNull(event.bubble)
            assertEquals(4_000L, event.durationMs)
        }
    }

    @Test
    fun `five minute idle timeout sleeps`() {
        assertEquals(PetState.SLEEP, engine.onIdleTimeout().state)
    }

    @Test
    fun `charging state is de-duplicated`() {
        val first = engine.onChargingChanged(true)
        assertNotNull(first)
        assertNotNull(first?.bubble)
        assertNull(engine.onChargingChanged(true))
        assertEquals(PetState.IDLE, engine.onChargingChanged(false)?.state)
        assertNull(engine.onChargingChanged(false))
    }

    @Test
    fun `tap words follow morning daytime and late night pools`() {
        assertEquals("早呀，小猫。", BubbleCatalog({ 0 }, { 8 }).tap())
        assertEquals("在呢。", BubbleCatalog({ 0 }, { 14 }).tap())
        assertEquals("这么晚还不睡？", BubbleCatalog({ 0 }, { 1 }).tap())
    }
}
