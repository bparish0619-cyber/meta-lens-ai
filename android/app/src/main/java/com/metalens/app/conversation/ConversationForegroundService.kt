package com.metalens.app.conversation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Foreground Service that keeps the conversation alive while the screen is locked.
 *
 * Owns microphone capture + Bluetooth SCO routing + OpenAI Realtime WebSocket.
 */
class ConversationForegroundService : Service() {
    companion object {
        private const val CHANNEL_ID = "conversation"
        private const val CHANNEL_NAME = "Conversation"
        private const val NOTIFICATION_ID = 42

        const val ACTION_START = "com.metalens.app.conversation.action.START"
        const val ACTION_STOP = "com.metalens.app.conversation.action.STOP"
        const val ACTION_SEND_TEXT = "com.metalens.app.conversation.action.SEND_TEXT"
        const val EXTRA_TEXT = "text"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var session: ConversationSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startInForegroundIfNeeded()
                ensureWakeLock()
                if (session == null) {
                    session = ConversationSession(appContext = applicationContext, scope = serviceScope)
                }
                session?.start()
            }
            ACTION_SEND_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                session?.sendText(text)
            }
            ACTION_STOP -> {
                stopSessionAndSelf()
            }
            else -> {
                // no-op
            }
        }

        // We don't want Android to restart the service automatically without user intent.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopSessionAndSelf(stopSelf = false)
        super.onDestroy()
    }

    private fun startInForegroundIfNeeded() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopSessionAndSelf(stopSelf: Boolean = true) {
        session?.stop()
        session = null

        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (_: Throwable) {
            // ignore
        }
        wakeLock = null

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Throwable) {
            // ignore
        }

        if (stopSelf) stopSelf()
    }

    private fun ensureWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MetaLensAI:Conversation").apply {
                setReferenceCounted(false)
                acquire(60 * 60 * 1000L) // 1h safety; we release on stop
            }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps voice conversation running in background"
            }
        mgr.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.metalens.app.R.drawable.smart_glasses_icon)
            .setContentTitle("Meta Lens AI")
            .setContentText("Conversation running…")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}

