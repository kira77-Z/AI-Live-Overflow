package com.kira.adrianpet.overlay

data class OverlayBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

data class OverlaySize(val width: Int, val height: Int)

data class OverlayPosition(val x: Int, val y: Int) {
    fun clamp(bounds: OverlayBounds, size: OverlaySize): OverlayPosition {
        val maxX = (bounds.right - size.width).coerceAtLeast(bounds.left)
        val maxY = (bounds.bottom - size.height).coerceAtLeast(bounds.top)
        return OverlayPosition(
            x = x.coerceIn(bounds.left, maxX),
            y = y.coerceIn(bounds.top, maxY),
        )
    }

    fun snapToEdge(bounds: OverlayBounds, size: OverlaySize): OverlayPosition {
        val safe = clamp(bounds, size)
        val rightEdge = (bounds.right - size.width).coerceAtLeast(bounds.left)
        val snappedX = if (safe.x - bounds.left <= rightEdge - safe.x) bounds.left else rightEdge
        return safe.copy(x = snappedX)
    }
}
