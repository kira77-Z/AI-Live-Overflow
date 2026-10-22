package com.kira.adrianpet.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

sealed interface RemoteEvent {
    data object Connected : RemoteEvent
    data class Command(val value: PetCommand) : RemoteEvent
    data object NetworkUnavailable : RemoteEvent
    data object SessionExpired : RemoteEvent
}

enum class SyncState { SignedOut, Connecting, Connected, OfflineRecoverable }

interface PetRemoteTransport {
    fun observe(userId: String): Flow<RemoteEvent>
    suspend fun recordEvent(event: PetEvent)
    suspend fun acknowledge(userId: String, commandId: String)
}

class PetRemoteRepository(private val transport: PetRemoteTransport) {
    private val mutableSyncState = MutableStateFlow(SyncState.Connecting)
    val syncState: StateFlow<SyncState> = mutableSyncState
    fun observeCommands(userId: String): Flow<PetCommand> = flow {
        val seenCommandIds = LinkedHashSet<String>()
        mutableSyncState.value = SyncState.Connecting
        transport.observe(userId).collect { event ->
            when (event) {
                RemoteEvent.Connected -> mutableSyncState.value = SyncState.Connected
                is RemoteEvent.Command -> if (seenCommandIds.add(event.value.id)) emit(event.value)
                RemoteEvent.NetworkUnavailable -> mutableSyncState.value = SyncState.OfflineRecoverable
                RemoteEvent.SessionExpired -> mutableSyncState.value = SyncState.SignedOut
            }
        }
    }

    suspend fun recordEvent(event: PetEvent) = transport.recordEvent(event)
    suspend fun acknowledge(userId: String, commandId: String) = transport.acknowledge(userId, commandId)
}
