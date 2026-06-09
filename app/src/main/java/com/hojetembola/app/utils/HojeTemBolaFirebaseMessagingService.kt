package com.hojetembola.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hojetembola.app.R
import com.hojetembola.app.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Serviço FCM responsável por:
 * 1. Receber notificações push em primeiro plano
 * 2. Atualizar o token FCM no Supabase quando renovado
 *
 * Registado no AndroidManifest.xml com o intent-filter
 * "com.google.firebase.MESSAGING_EVENT".
 */
@AndroidEntryPoint
class HojeTemBolaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM: $token")
        serviceScope.launch {
            userRepository.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Mensagem FCM recebida de: ${message.from}")

        val title   = message.notification?.title ?: message.data["titulo"] ?: return
        val body    = message.notification?.body  ?: message.data["mensagem"] ?: return
        val tipo    = message.data["tipo"] ?: "geral"

        showNotification(title, body, tipo)
    }

    // ── Notificação local ─────────────────────────────────────────────────────

    private fun showNotification(title: String, body: String, tipo: String) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Criar canal (obrigatório API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_DESC }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir a app ao clicar na notificação
        val intent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG          = "FCM_Service"
        const val CHANNEL_ID   = "hojetembola_channel"
        const val CHANNEL_NAME = "HojeTemBola"
        const val CHANNEL_DESC = "Notificações de jogos, convites e resultados"
    }
}
