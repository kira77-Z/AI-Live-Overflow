package com.kira.adrianpet.data.cloud

import android.content.Context
import com.kira.adrianpet.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object CloudRuntime {
    val isConfigured: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    @Volatile
    private var client: SupabaseClient? = null

    fun clientOrNull(context: Context): SupabaseClient? {
        if (!isConfigured) return null
        client?.let { return it }
        return synchronized(this) {
            client ?: createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            ) {
            install(Auth) {
                sessionManager = AndroidSessionManager(context.applicationContext)
                codeVerifierCache = MemoryCodeVerifierCache()
            }
            install(Postgrest)
            install(Realtime)
            }.also { client = it }
        }
    }
}
