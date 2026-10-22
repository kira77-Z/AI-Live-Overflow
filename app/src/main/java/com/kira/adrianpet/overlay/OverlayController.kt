package com.kira.adrianpet.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebView
import com.kira.adrianpet.render.PetRenderer
import com.kira.adrianpet.render.PetState
import com.kira.adrianpet.render.RenderEvent
import com.kira.adrianpet.behavior.PetBehaviorEngine
import com.kira.adrianpet.behavior.PetHaptics
import com.kira.adrianpet.gesture.GestureClassifier
import com.kira.adrianpet.gesture.GestureResult
import com.kira.adrianpet.gesture.TouchAction
import com.kira.adrianpet.gesture.TouchSample
import kotlin.math.roundToInt

class OverlayController(
    private val context: Context,
    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java),
    private val onPositionSettled: (perch: Boolean, position: OverlayPosition) -> Unit = { _, _ -> },
    private val onInteraction: () -> Unit = {},
    private val onPetEvent: (type: String, payload: Map<String, String>) -> Unit = { _, _ -> },
    private val onReady: () -> Unit = {},
    private val onOverlayFailure: () -> Unit = {},
) {
    private val size = OverlaySize(context.dp(180), context.dp(240))
    private var webView: WebView? = null
    private var renderer: PetRenderer? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var normalPosition = OverlayPosition(24, 360)
    private var perchPosition = OverlayPosition(24, 1_500)
    private var perchMode = false
    private var bubblesEnabled = true
    private val behavior = PetBehaviorEngine()
    private val haptics = PetHaptics(context)
    private val gestures = GestureClassifier(ViewConfiguration.get(context).scaledTouchSlop)
    private var dragOrigin = OverlayPosition(0, 0)

    val isShown: Boolean get() = webView != null
    val currentPosition: OverlayPosition
        get() = layoutParams?.let { OverlayPosition(it.x, it.y) }
            ?: activePosition()

    fun restorePositions(normal: OverlayPosition, perch: OverlayPosition) {
        normalPosition = normal
        perchPosition = perch
    }

    @SuppressLint("ClickableViewAccessibility")
    fun show() {
        if (isShown || !Settings.canDrawOverlays(context)) return
        val safePosition = activePosition().clamp(safeBounds(), size)
        setActivePosition(safePosition)
        val params = newLayoutParams(safePosition)
        val view = PetWebView(context).also { webView = it }
        view.setOnTouchListener { touchedView, event -> handleTouch(touchedView, event) }
        layoutParams = params
        renderer = PetRenderer(view) {
            renderMode()
            onReady()
        }
        runCatching { windowManager.addView(view, params) }
            .onFailure { failOverlay() }
    }

    fun hide() {
        webView?.let { view ->
            runCatching { windowManager.removeView(view) }
            view.stopLoading()
            view.loadUrl("about:blank")
            view.removeAllViews()
            view.destroy()
        }
        webView = null
        renderer = null
        layoutParams = null
    }

    fun moveTo(x: Int, y: Int) {
        val params = layoutParams ?: return
        val safe = OverlayPosition(x, y).clamp(safeBounds(), size)
        params.x = safe.x
        params.y = safe.y
        setActivePosition(safe)
        webView?.let { view ->
            runCatching { windowManager.updateViewLayout(view, params) }
                .onFailure { failOverlay() }
        }
    }

    fun finishMove() {
        val snapped = currentPosition.snapToEdge(safeBounds(), size)
        moveTo(snapped.x, snapped.y)
        onPositionSettled(perchMode, snapped)
    }

    fun setPerchMode(enabled: Boolean) {
        perchMode = enabled
        val target = activePosition().clamp(safeBounds(), size)
        moveTo(target.x, target.y)
        renderMode()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        haptics.enabled = enabled
    }

    fun setBubblesEnabled(enabled: Boolean) {
        bubblesEnabled = enabled
    }

    fun render(event: RenderEvent): Boolean {
        val visibleEvent = if (bubblesEnabled) event else event.copy(bubble = null)
        return renderer?.render(visibleEvent.copy(perch = perchMode)) == true
    }

    fun reClamp() {
        val safe = currentPosition.clamp(safeBounds(), size)
        moveTo(safe.x, safe.y)
    }

    fun destroy() = hide()

    private fun renderMode() {
        renderer?.render(RenderEvent(PetState.IDLE, perch = perchMode))
    }

    private fun handleTouch(view: View, event: MotionEvent): Boolean {
        onInteraction()
        if (event.actionMasked == MotionEvent.ACTION_DOWN) dragOrigin = currentPosition
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> TouchAction.DOWN
            MotionEvent.ACTION_MOVE -> TouchAction.MOVE
            MotionEvent.ACTION_UP -> TouchAction.UP
            MotionEvent.ACTION_CANCEL -> TouchAction.CANCEL
            else -> return true
        }
        gestures.onTouch(TouchSample(action, event.rawX, event.rawY, event.eventTime)).forEach { result ->
            when (result) {
                is GestureResult.DragMove -> moveTo(dragOrigin.x + result.deltaX, dragOrigin.y + result.deltaY)
                GestureResult.DragEnd -> {
                    finishMove()
                    haptics.tick()
                    onPetEvent(
                        "drag",
                        mapOf("x" to currentPosition.x.toString(), "y" to currentPosition.y.toString()),
                    )
                }
                GestureResult.Tap, GestureResult.RapidTap -> {
                    view.performClick()
                    haptics.tick()
                    onPetEvent(if (result == GestureResult.Tap) "tap" else "rapid_tap", emptyMap())
                }
            }
            render(behavior.onGesture(result))
        }
        return true
    }

    private fun activePosition() = if (perchMode) perchPosition else normalPosition

    private fun setActivePosition(position: OverlayPosition) {
        if (perchMode) perchPosition = position else normalPosition = position
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    @Suppress("DEPRECATION")
    private fun safeBounds(): OverlayBounds {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            return OverlayBounds(
                bounds.left + insets.left,
                bounds.top + insets.top,
                bounds.right - insets.right,
                bounds.bottom - insets.bottom,
            )
        }
        val size = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(size)
        val insets = webView?.rootWindowInsets
        if (insets != null) {
            @Suppress("DEPRECATION")
            return OverlayBounds(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                size.x - insets.systemWindowInsetRight,
                size.y - insets.systemWindowInsetBottom,
            )
        }
        val visibleFrame = Rect()
        webView?.getWindowVisibleDisplayFrame(visibleFrame)
        if (!visibleFrame.isEmpty) return OverlayBounds(
            visibleFrame.left,
            visibleFrame.top,
            visibleFrame.right,
            visibleFrame.bottom,
        )
        val statusBarId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (statusBarId > 0) context.resources.getDimensionPixelSize(statusBarId) else 0
        return OverlayBounds(0, statusBarHeight, size.x, size.y)
    }

    private fun failOverlay() {
        hide()
        onOverlayFailure()
    }

    private fun newLayoutParams(position: OverlayPosition) = WindowManager.LayoutParams(
        size.width,
        size.height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = position.x
        y = position.y
    }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}

private class PetWebView(context: Context) : WebView(context) {
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
