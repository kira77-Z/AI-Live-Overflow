package com.kira.adrianpet.data.remote

import com.kira.adrianpet.render.PetState
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetRemoteRepositoryTest {
    @Test
    fun `network failure stays recoverable and local behavior is independent`() = runBlocking {
        val repository = PetRemoteRepository(FakeRemoteTransport(listOf(RemoteEvent.NetworkUnavailable)))

        assertTrue(repository.observeCommands("user-1").toList().isEmpty())
        assertEquals(SyncState.OfflineRecoverable, repository.syncState.value)
    }

    @Test
    fun `expired session moves sync to signed out`() = runBlocking {
        val repository = PetRemoteRepository(FakeRemoteTransport(listOf(RemoteEvent.SessionExpired)))

        repository.observeCommands("user-1").toList()

        assertEquals(SyncState.SignedOut, repository.syncState.value)
    }

    @Test
    fun `duplicate command ids render only once and reconnect continues`() = runBlocking {
        val command = PetCommand("cmd-1", PetState.TAP, "在呢", 4_000)
        val repository = PetRemoteRepository(
            FakeRemoteTransport(
                listOf(
                    RemoteEvent.Connected,
                    RemoteEvent.Command(command),
                    RemoteEvent.NetworkUnavailable,
                    RemoteEvent.Connected,
                    RemoteEvent.Command(command),
                    RemoteEvent.Command(command.copy(id = "cmd-2")),
                ),
            ),
        )

        val received = repository.observeCommands("user-1").toList()

        assertEquals(listOf("cmd-1", "cmd-2"), received.map { it.id })
        assertEquals(SyncState.Connected, repository.syncState.value)
    }

    @Test
    fun `command deduplication is scoped to one user observation`() = runBlocking {
        val command = PetCommand("cmd-1", PetState.TAP, "在呢", 4_000)
        val repository = PetRemoteRepository(FakeRemoteTransport(listOf(RemoteEvent.Command(command))))

        assertEquals(listOf(command), repository.observeCommands("user-1").toList())
        assertEquals(listOf(command), repository.observeCommands("user-2").toList())
    }
}

private class FakeRemoteTransport(private val events: List<RemoteEvent>) : PetRemoteTransport {
    override fun observe(userId: String) = flow { events.forEach { emit(it) } }
    override suspend fun recordEvent(event: PetEvent) = Unit
    override suspend fun acknowledge(userId: String, commandId: String) = Unit
}
