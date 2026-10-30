package app.gamenative.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import app.gamenative.MainActivity
import app.gamenative.R
import timber.log.Timber

/**
 * Foreground service held for as long as a game process is running, so Android doesn't treat the
 * app as cached and freeze or kill it (and the Wine processes under it) while it's in the background.
 */
class GameSessionService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        } catch (e: Exception) {
            Timber.e(e, "GameSessionService: startForeground failed")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Game session", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps the game running while the app is in the background"
                    setShowBadge(false)
                },
            )
        }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("World of Warcraft is running")
            .setContentText("Tap to return to the game")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "game_session"
        private const val NOTIFICATION_ID = 7301

        fun start(context: Context) {
            try {
                context.startForegroundService(Intent(context, GameSessionService::class.java))
            } catch (e: Exception) {
                // Android 12+ refuses to start a foreground service while the app is in the background
                Timber.w(e, "GameSessionService: could not start")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, GameSessionService::class.java))
        }
    }
}
