package com.kira.adrianpet.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecordOrNull
import io.github.jan.supabase.realtime.postgresChangeFlow
import java.time.Instant
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabasePetRemoteTransport(private val client: SupabaseClient) : PetRemoteTransport {
    override fun observe(userId: String): Flow<RemoteEvent> = channelFlow {
        if (client.auth.currentSessionOrNull() == null) {
            send(RemoteEvent.SessionExpired)
            close()
            return@channelFlow
        }

        val channel = client.channel("pet-commands-$userId")
        val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "pet_commands"
            filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
        }
        val realtimeJob = launch {
            inserts.collect { change ->
                change.decodeRecordOrNull<PetCommandRow>()
                    ?.takeIf { it.isPending() }
                    ?.toCommand()
                    ?.let { send(RemoteEvent.Command(it)) }
            }
        }

        channel.subscribe(blockUntilSubscribed = true)
        send(RemoteEvent.Connected)
        fetchPending().forEach { row -> row.toCommand()?.let { send(RemoteEvent.Command(it)) } }

        awaitClose {
            realtimeJob.cancel()
            launch { channel.unsubscribe() }
        }
    }.catch {
        emit(if (client.auth.currentSessionOrNull() == null) RemoteEvent.SessionExpired else RemoteEvent.NetworkUnavailable)
    }

    override suspend fun recordEvent(event: PetEvent) {
        val userId = client.auth.currentUserOrNull()?.id ?: return
        client.from("pet_events").insert(
            PetEventRow(userId = userId, eventType = event.type, payload = event.payload),
        )
    }

    override suspend fun acknowledge(userId: String, commandId: String) {
        val updated = client.from("pet_commands").update(
            update = { set("consumed_at", Instant.now().toString()) },
            request = {
                filter {
                    eq("id", commandId)
                    eq("user_id", userId)
                }
                select(Columns.raw("id"))
            },
        ).decodeList<AcknowledgedCommandRow>()
        check(updated.any { it.id == commandId }) { "Command acknowledgement updated no rows" }
    }

    private suspend fun fetchPending(): List<PetCommandRow> =
        client.from("pet_commands").select {
            filter { exact("consumed_at", null) }
            order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            limit(100)
        }.decodeList<PetCommandRow>().filter(PetCommandRow::isPending)

    @Serializable
    private data class PetCommandRow(
        val id: String,
        @SerialName("command_type") val commandType: String,
        val state: String,
        val text: String,
        @SerialName("duration_ms") val durationMs: Long = 4_000L,
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("consumed_at") val consumedAt: String? = null,
    ) {
        fun isPending(): Boolean = consumedAt == null &&
            (expiresAt == null || runCatching { Instant.parse(expiresAt).isAfter(Instant.now()) }.getOrDefault(false))

        fun toCommand(): PetCommand? = PetCommand.parse(
            Json.encodeToString(
                mapOf(
                    "id" to id,
                    "type" to commandType,
                    "state" to state,
                    "text" to text,
                    "duration_ms" to durationMs.toString(),
                ),
            ),
        )
    }

    @Serializable
    private data class AcknowledgedCommandRow(val id: String)

    @Serializable
    private data class PetEventRow(
        @SerialName("user_id") val userId: String,
        @SerialName("event_type") val eventType: String,
        val payload: Map<String, String>,
    )
}
