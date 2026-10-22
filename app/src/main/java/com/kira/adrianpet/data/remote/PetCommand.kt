package com.kira.adrianpet.data.remote

import com.kira.adrianpet.render.PetState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

data class PetCommand(
    val id: String,
    val state: PetState,
    val text: String,
    val durationMs: Long,
) {
    fun asRenderEvent() = com.kira.adrianpet.render.RenderEvent(state, text, durationMs)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val MAX_TEXT_CODE_POINTS = 36

        fun parse(raw: String): PetCommand? = runCatching {
            val value = json.parseToJsonElement(raw).jsonObject
            if (value["type"]?.jsonPrimitive?.contentOrNull != "bubble") return null
            val stateName = value["state"]?.jsonPrimitive?.contentOrNull ?: return null
            val state = PetState.entries.firstOrNull { it.wireName == stateName } ?: return null
            val id = value["id"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: return null
            val rawText = value["text"]?.jsonPrimitive?.contentOrNull ?: return null
            if (rawText.isBlank()) return null
            val text = rawText.takeCodePoints(MAX_TEXT_CODE_POINTS)
            val duration = (value["duration_ms"]?.jsonPrimitive?.longOrNull ?: 4_000L).coerceIn(500L, 15_000L)
            PetCommand(id, state, text, duration)
        }.getOrNull()

        private fun String.takeCodePoints(limit: Int): String {
            if (codePointCount(0, length) <= limit) return this
            return substring(0, offsetByCodePoints(0, limit - 1)) + "…"
        }
    }
}

data class PetEvent(
    val type: String,
    val payload: Map<String, String> = emptyMap(),
)
