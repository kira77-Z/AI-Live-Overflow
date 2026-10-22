package com.kira.adrianpet.behavior

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kira.adrianpet.gesture.GestureResult
import com.kira.adrianpet.render.PetState
import com.kira.adrianpet.render.RenderEvent

class PetBehaviorEngine(private val bubbles: BubbleCatalog = BubbleCatalog()) {
    private var charging: Boolean? = null

    fun onGesture(result: GestureResult): RenderEvent = when (result) {
        GestureResult.Tap -> RenderEvent(PetState.TAP, bubbles.tap())
        GestureResult.RapidTap -> RenderEvent(PetState.RAPID_TAP, bubbles.rapidTap())
        is GestureResult.DragMove -> RenderEvent(PetState.DRAG)
        GestureResult.DragEnd -> RenderEvent(PetState.DRAG, bubbles.drag())
    }

    fun onChargingChanged(isCharging: Boolean): RenderEvent? {
        if (charging == isCharging) return null
        charging = isCharging
        return if (isCharging) {
            RenderEvent(PetState.TAP, bubbles.charging())
        } else {
            RenderEvent(PetState.IDLE, bubbles.unplugged())
        }
    }

    fun onIdleTimeout(): RenderEvent = RenderEvent(PetState.SLEEP, bubbles.sleep())
}

class PetHaptics(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var enabled: Boolean = true

    fun tick() {
        if (!enabled || !vibrator.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}
