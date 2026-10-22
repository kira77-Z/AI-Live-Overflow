package com.kira.adrianpet.render

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PetBubbleLayoutTest {
    @Test
    fun `multi-line bubble grows down from a safe top inset`() {
        val css = RuntimeEnvironment.getApplication().assets.open("pet/pet.css")
            .bufferedReader()
            .use { it.readText() }
        val bubbleRule = css.substringAfter(".bubble {").substringBefore("}")

        assertTrue(
            "Bubble must start inside the WebView so extra lines remain visible",
            Regex("""\btop\s*:\s*8px\s*;""").containsMatchIn(bubbleRule),
        )
        assertFalse(
            "Bottom anchoring makes multi-line bubbles grow past the WebView's top edge",
            Regex("""\bbottom\s*:""").containsMatchIn(bubbleRule),
        )
    }
}
