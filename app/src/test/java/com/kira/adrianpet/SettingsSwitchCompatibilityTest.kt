package com.kira.adrianpet

import com.google.android.material.materialswitch.MaterialSwitch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
}
