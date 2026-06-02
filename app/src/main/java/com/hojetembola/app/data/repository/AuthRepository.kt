package com.hojetembola.app.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório de autenticação — encapsula todas as chamadas ao Supabase Auth.
 *
 * Contrato de retorno:
 *   [Result.success] — operação bem-sucedida, valor é o UID (String) ou Unit
 *   [Result.failure] — erro com mensagem localizada em PT para a UI
 *
 * Injetado via Hilt em [com.hojetembola.app.ui.auth.AuthViewModel].
 * O [SupabaseClient] é fornecido pelo [com.hojetembola.app.di.AppModule].
 */
@Singleton
class AuthRepository @Inject constructor(
    private val client: SupabaseClient
) {

    // ── Login com email / password ────────────────────────────────────────────

    /**
     * Autentica o utilizador com email e password.
     * @return [Result.success] com o UID da sessão, ou [Result.failure] com mensagem PT.
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            client.auth.signInWith(Email) {
                this.email    = email
                this.password = password
            }
            val uid = client.auth.currentSessionOrNull()?.user?.id
                ?: throw Exception("Sessão inativa após login.")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(Exception(mapError(e)))
        }
    }

    // ── Registo com email / password ──────────────────────────────────────────

    /**
     * Cria uma conta no Supabase com metadados adicionais (nome e perfil).
     *
     * Se a verificação de email estiver ativa no projeto Supabase,
     * não existe sessão imediata — o UID retornado pode ser uma string vazia.
     * O fragmento de registo trata este caso apresentando a mensagem de verificação.
     *
     * Os metadados (nome_completo, perfil) ficam em `raw_user_meta_data`
     * na tabela `auth.users` do Supabase.
     *
     * @return [Result.success] com o UID (vazio se verificação pendente),
     *         ou [Result.failure] com mensagem PT.
     */
    suspend fun register(
        email: String,
        password: String,
        nome: String,
        perfil: String
    ): Result<String> {
        return try {
            client.auth.signUpWith(Email) {
                this.email    = email
                this.password = password
                this.data     = buildJsonObject {
                    put("nome_completo", nome)
                    put("perfil",        perfil)
                }
            }
            // Quando a verificação de email está ativa, currentSession é null
            // mas currentUser devolve o utilizador criado.
            val uid = client.auth.currentSessionOrNull()?.user?.id
                ?: client.auth.currentUserOrNull()?.id
                ?: ""
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(Exception(mapError(e)))
        }
    }

    // ── Login com Google OAuth ────────────────────────────────────────────────

    /**
     * Inicia o fluxo OAuth com Google via Supabase.
     *
     * **Para ativar:**
     * 1. Google Cloud Console → Credentials → OAuth 2.0 → Web Application
     * 2. Supabase Dashboard → Authentication → Providers → Google
     *    → colar Client ID e Client Secret
     * 3. Adicionar URI de redirect no Google Cloud:
     *    `https://xudejgyoknaampmtgdof.supabase.co/auth/v1/callback`
     *
     * Até estar configurado, retorna erro informativo para a UI.
     *
     * @return [Result.failure] com mensagem informativa (MVP académico).
     */
    suspend fun loginWithGoogle(): Result<String> =
        Result.failure(
            Exception("Login com Google ainda não configurado.\nUsa email e password.")
        )

    // ── Recuperação de password ───────────────────────────────────────────────

    /**
     * Envia email de recuperação de password via Supabase Auth.
     *
     * O utilizador recebe um link para redefinir a password.
     * O template do email pode ser personalizado no Supabase Dashboard
     * → Authentication → Email Templates → Reset Password.
     *
     * @return [Result.success] Unit em caso de sucesso,
     *         ou [Result.failure] com mensagem PT.
     */
    suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            client.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapError(e)))
        }
    }

    // ── Mapeamento de erros ───────────────────────────────────────────────────

    /**
     * Converte mensagens de erro do Supabase (em inglês) para português.
     * Garante que o utilizador nunca vê mensagens técnicas em inglês.
     */
    private fun mapError(e: Exception): String {
        val msg = e.message ?: return "Ocorreu um erro. Tenta novamente."
        return when {
            msg.contains("Invalid login credentials",    ignoreCase = true) ->
                "Email ou password incorretos."
            msg.contains("Email not confirmed",          ignoreCase = true) ->
                "Verifica o teu email antes de entrares."
            msg.contains("already registered",           ignoreCase = true) ->
                "Este email já está registado."
            msg.contains("User already registered",      ignoreCase = true) ->
                "Este email já está registado."
            msg.contains("Password should be",           ignoreCase = true) ||
            msg.contains("weak",                         ignoreCase = true) ->
                "A password não cumpre os requisitos mínimos."
            msg.contains("rate limit",                   ignoreCase = true) ->
                "Demasiadas tentativas. Aguarda um momento."
            msg.contains("Unable to resolve host",       ignoreCase = true) ||
            msg.contains("network",                      ignoreCase = true) ||
            msg.contains("timeout",                      ignoreCase = true) ->
                "Sem ligação à internet. Verifica a rede."
            msg.contains("Sessão inativa",               ignoreCase = true) ->
                "Erro de autenticação. Tenta novamente."
            else -> msg
        }
    }
}
