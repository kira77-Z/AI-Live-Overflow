package com.kira.adrianpet.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kira.adrianpet.overlay.OverlayPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.petDataStore by preferencesDataStore(name = "pet_preferences")

data class OverlayPreferences(
    val normal: OverlayPosition,
    val perch: OverlayPosition,
    val perchEnabled: Boolean,
    val hapticsEnabled: Boolean,
    val bubblesEnabled: Boolean,
    val serviceEnabled: Boolean,
)

class PetPreferences(private val context: Context) {
    val overlay: Flow<OverlayPreferences> = context.petDataStore.data.map { values ->
        OverlayPreferences(
            normal = OverlayPosition(values[NORMAL_X] ?: DEFAULT_X, values[NORMAL_Y] ?: DEFAULT_Y),
            perch = OverlayPosition(values[PERCH_X] ?: DEFAULT_X, values[PERCH_Y] ?: DEFAULT_PERCH_Y),
            perchEnabled = values[PERCH_ENABLED] ?: false,
            hapticsEnabled = values[HAPTICS_ENABLED] ?: true,
            bubblesEnabled = values[BUBBLES_ENABLED] ?: true,
            serviceEnabled = values[SERVICE_ENABLED] ?: false,
        )
    }

    suspend fun savePosition(perch: Boolean, position: OverlayPosition) {
        context.petDataStore.edit { values ->
            val (xKey, yKey) = if (perch) PERCH_X to PERCH_Y else NORMAL_X to NORMAL_Y
            values[xKey] = position.x
            values[yKey] = position.y
        }
    }

    suspend fun setPerchEnabled(enabled: Boolean) {
        context.petDataStore.edit { it[PERCH_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.petDataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }

    suspend fun setBubblesEnabled(enabled: Boolean) {
        context.petDataStore.edit { it[BUBBLES_ENABLED] = enabled }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.petDataStore.edit { it[SERVICE_ENABLED] = enabled }
    }

    suspend fun resetPositions() {
        context.petDataStore.edit { values ->
            values.remove(NORMAL_X)
            values.remove(NORMAL_Y)
            values.remove(PERCH_X)
            values.remove(PERCH_Y)
        }
    }

    suspend fun wasCommandPresented(commandId: String): Boolean =
        context.petDataStore.data.first()[PRESENTED_COMMAND_IDS]?.contains(commandId) == true

    suspend fun markCommandPresented(commandId: String) {
        context.petDataStore.edit { values ->
            val ids = values[PRESENTED_COMMAND_IDS].orEmpty().toMutableSet()
            ids += commandId
            values[PRESENTED_COMMAND_IDS] = ids.toList().takeLast(MAX_PRESENTED_COMMAND_IDS).toSet()
        }
    }

    suspend fun clearPresentedCommand(commandId: String) {
        context.petDataStore.edit { values ->
            values[PRESENTED_COMMAND_IDS] = values[PRESENTED_COMMAND_IDS].orEmpty() - commandId
        }
    }

    companion object {
        val NORMAL_X: Preferences.Key<Int> = intPreferencesKey("normal_x")
        val NORMAL_Y: Preferences.Key<Int> = intPreferencesKey("normal_y")
        val PERCH_X: Preferences.Key<Int> = intPreferencesKey("perch_x")
        val PERCH_Y: Preferences.Key<Int> = intPreferencesKey("perch_y")
        val PERCH_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("perch_enabled")
        val HAPTICS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("haptics_enabled")
        val BUBBLES_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("bubbles_enabled")
        val SERVICE_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("service_enabled")
        val PRESENTED_COMMAND_IDS: Preferences.Key<Set<String>> = stringSetPreferencesKey("presented_command_ids")

        val DEFAULT_NORMAL_POSITION = OverlayPosition(24, 360)
        val DEFAULT_PERCH_POSITION = OverlayPosition(24, 1_500)

        private const val DEFAULT_X = 24
        private const val DEFAULT_Y = 360
        private const val DEFAULT_PERCH_Y = 1_500
        private const val MAX_PRESENTED_COMMAND_IDS = 256
    }
}
