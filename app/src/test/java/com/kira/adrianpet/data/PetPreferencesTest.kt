package com.kira.adrianpet.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class PetPreferencesTest {
    @Test
    fun `presented remote command survives repository recreation until acknowledged`() = runBlocking {
        val first = PetPreferences(RuntimeEnvironment.getApplication())
        first.markCommandPresented("command-persisted")

        val restored = PetPreferences(RuntimeEnvironment.getApplication())
        assertTrue(restored.wasCommandPresented("command-persisted"))

        restored.clearPresentedCommand("command-persisted")
        assertFalse(first.wasCommandPresented("command-persisted"))
    }
}
