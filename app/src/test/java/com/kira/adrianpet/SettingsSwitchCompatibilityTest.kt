package com.kira.adrianpet

import android.view.View
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsSwitchCompatibilityTest {

    @Test
    fun settingsSwitchesNeverMeasureNullStateText() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val switches = listOf(
            activity.findViewById<MaterialSwitch>(R.id.service_switch),
            activity.findViewById<MaterialSwitch>(R.id.perch_switch),
            activity.findViewById<MaterialSwitch>(R.id.bubbles_switch),
            activity.findViewById<MaterialSwitch>(R.id.haptics_switch),
        )

        switches.forEach { switch ->
            assertFalse("State text must not be drawn", switch.showText)
            assertNotNull("ON text must be non-null for OEM measurement", switch.textOn)
            assertNotNull("OFF text must be non-null for OEM measurement", switch.textOff)
        }
    }

    @Test
    fun settingsUseIndependentVisibleSwitchControls() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val switches = listOf(
            activity.findViewById<SwitchCompat>(R.id.service_switch),
            activity.findViewById<SwitchCompat>(R.id.perch_switch),
            activity.findViewById<SwitchCompat>(R.id.bubbles_switch),
            activity.findViewById<SwitchCompat>(R.id.haptics_switch),
        )

        switches.forEach { switch ->
            assertEquals(SwitchCompat::class.java, switch.javaClass)
            assertNotNull("Switch thumb must have an explicit drawable", switch.thumbDrawable)
            assertNotNull("Switch track must have an explicit drawable", switch.trackDrawable)
        }
    }

    @Test
    fun tappingTheWholeServiceSettingRowTogglesItsSwitch() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val switch = activity.findViewById<SwitchCompat>(R.id.service_switch)
        val row = switch.parent as View
        val wasChecked = switch.isChecked

        assertTrue("The whole setting row must handle taps", row.performClick())
        assertEquals(!wasChecked, switch.isChecked)
    }
}
