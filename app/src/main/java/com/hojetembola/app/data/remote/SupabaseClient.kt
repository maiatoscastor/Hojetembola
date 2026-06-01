package com.hojetembola.app.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Singleton que expõe o cliente Supabase configurado com todos os plugins.
 *
 * NOTA: As credenciais são armazenadas aqui apenas para fins académicos.
 * Em produção deveriam estar em BuildConfig (local.properties, nunca em VCS).
 *
 * URL corrigido: o espaço no URL original era artefacto de copy-paste;
 * o ref correto vem do JWT: "ref":"xudejgyoknaampmtgdof".
 */
object SupabaseClient {

    private const val SUPABASE_URL =
        "https://xudejgyoknaampmtgdof.supabase.co"

    private const val SUPABASE_ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
        "eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh1ZGVqZ3lva25hYW1wbXRnZG9mIiwi" +
        "cm9sZSI6ImFub24iLCJpYXQiOjE3Nzk4ODY0ODcsImV4cCI6MjA5NTQ2MjQ4N30." +
        "2s4AE7YT_wCqcXUOwKR05zJBEHgCAVHpRuVyzL4B-vA"

    /**
     * Instância principal do cliente. Injetada via [com.hojetembola.app.di.AppModule]
     * e partilhada em toda a aplicação (singleton Hilt).
     */
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)   // queries SQL / RLS
        install(Auth)        // autenticação (email, OAuth)
        install(Realtime)    // subscriptions em tempo real
        install(Storage)     // upload/download de ficheiros
    }
}
