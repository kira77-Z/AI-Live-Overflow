package com.kira.adrianpet.gesture

import kotlin.math.hypot
import kotlin.math.roundToInt

enum class TouchAction { DOWN, MOVE, UP, CANCEL }

data class TouchSample(
    val action: TouchAction,
    val rawX: Float,
    val rawY: Float,
    val atMs: Long,
)

sealed interface GestureResult {
    data object Tap : GestureResult
    data object RapidTap : GestureResult
    data class DragMove(val deltaX: Int, val deltaY: Int) : GestureResult
    data object DragEnd : GestureResult
}

class GestureClassifier(
    private val touchSlopPx: Int,
    private val rapidTapWindowMs: Long = 2_000L,
) {
    private var down: TouchSample? = null
    private var dragging = false
    private val tapTimes = ArrayDeque<Long>()

    fun onTouch(sample: TouchSample): List<GestureResult> = when (sample.action) {
        TouchAction.DOWN -> {
            down = sample
            dragging = false
            emptyList()
        }
        TouchAction.MOVE -> handleMove(sample)
        TouchAction.UP -> handleUp(sample)
        TouchAction.CANCEL -> handleCancel()
    }

    private fun handleMove(sample: TouchSample): List<GestureResult> {
        val origin = down ?: return emptyList()
        val dx = sample.rawX - origin.rawX
        val dy = sample.rawY - origin.rawY
        if (!dragging && hypot(dx.toDouble(), dy.toDouble()) <= touchSlopPx) return emptyList()
        if (!dragging) {
            dragging = true
            tapTimes.clear()
        }
        return listOf(GestureResult.DragMove(dx.roundToInt(), dy.roundToInt()))
    }

    private fun handleUp(sample: TouchSample): List<GestureResult> {
        if (down == null) return emptyList()
        down = null
        if (dragging) {
            dragging = false
            tapTimes.clear()
            return listOf(GestureResult.DragEnd)
        }

        while (tapTimes.isNotEmpty() && sample.atMs - tapTimes.first() > rapidTapWindowMs) {
            tapTimes.removeFirst()
        }
        tapTimes.addLast(sample.atMs)
        return if (tapTimes.size >= RAPID_TAP_COUNT) {
            tapTimes.clear()
            listOf(GestureResult.RapidTap)
        } else {
            listOf(GestureResult.Tap)
        }
    }

    private fun handleCancel(): List<GestureResult> {
        val wasDragging = dragging
        down = null
        dragging = false
        tapTimes.clear()
        return if (wasDragging) listOf(GestureResult.DragEnd) else emptyList()
    }

    private companion object {
        const val RAPID_TAP_COUNT = 3
    }
}
