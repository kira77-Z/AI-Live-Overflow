package com.kira.adrianpet.render

import org.json.JSONObject

enum class PetState(val wireName: String) {
    IDLE("idle"),
    BLINK("blink"),
    TAP("tap"),
    RAPID_TAP("rapid-tap"),
    DRAG("drag"),
    SLEEP("sleep"),
    PERCH("perch"),
}

data class RenderEvent(
    val state: PetState,
    val bubble: String? = null,
    val durationMs: Long = 4_000L,
    val perch: Boolean = false,
) {
    fun toJson(): String = JSONObject()
        .put("state", state.wireName)
        .put("bubble", bubble?.takeCodePoints(MAX_BUBBLE_CODE_POINTS) ?: JSONObject.NULL)
        .put("durationMs", durationMs)
        .put("perch", perch)
        .toString()
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")

    private fun String.takeCodePoints(limit: Int): String {
        if (codePointCount(0, length) <= limit) return this
        return substring(0, offsetByCodePoints(0, limit - 1)) + "…"
    }

    private companion object {
        const val MAX_BUBBLE_CODE_POINTS = 36
    }
}
