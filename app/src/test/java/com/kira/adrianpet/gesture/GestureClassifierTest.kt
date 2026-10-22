package com.kira.adrianpet.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureClassifierTest {
    private val classifier = GestureClassifier(touchSlopPx = 12, rapidTapWindowMs = 2_000)

    @Test
    fun `single tap emits tap on release`() {
        assertTrue(classifier.onTouch(sample(TouchAction.DOWN, 10, 20, 0)).isEmpty())
        assertEquals(listOf(GestureResult.Tap), classifier.onTouch(sample(TouchAction.UP, 11, 20, 80)))
    }

    @Test
    fun `third tap within two seconds emits rapid tap`() {
        tapAt(0)
        assertEquals(listOf(GestureResult.Tap), tapAt(500))
        assertEquals(listOf(GestureResult.RapidTap), tapAt(1_200))
    }

    @Test
    fun `movement beyond touch slop becomes drag and ends without tap`() {
        classifier.onTouch(sample(TouchAction.DOWN, 100, 200, 0))
        assertTrue(classifier.onTouch(sample(TouchAction.MOVE, 108, 205, 20)).isEmpty())
        assertEquals(listOf(GestureResult.DragMove(30, 40)), classifier.onTouch(sample(TouchAction.MOVE, 130, 240, 40)))
        assertEquals(listOf(GestureResult.DragEnd), classifier.onTouch(sample(TouchAction.UP, 132, 242, 60)))
    }

    @Test
    fun `cancelled drag settles but never taps`() {
        classifier.onTouch(sample(TouchAction.DOWN, 0, 0, 0))
        classifier.onTouch(sample(TouchAction.MOVE, 40, 0, 20))

        assertEquals(listOf(GestureResult.DragEnd), classifier.onTouch(sample(TouchAction.CANCEL, 40, 0, 30)))
    }

    @Test
    fun `tap drag tap resets streak after drag`() {
        assertEquals(listOf(GestureResult.Tap), tapAt(0))
        classifier.onTouch(sample(TouchAction.DOWN, 20, 20, 300))
        classifier.onTouch(sample(TouchAction.MOVE, 80, 80, 350))
        classifier.onTouch(sample(TouchAction.UP, 80, 80, 400))

        assertEquals(listOf(GestureResult.Tap), tapAt(500))
    }

    private fun tapAt(time: Long): List<GestureResult> {
        classifier.onTouch(sample(TouchAction.DOWN, 10, 10, time))
        return classifier.onTouch(sample(TouchAction.UP, 10, 10, time + 50))
    }

    private fun sample(action: TouchAction, x: Int, y: Int, at: Long) =
        TouchSample(action, x.toFloat(), y.toFloat(), at)
}
