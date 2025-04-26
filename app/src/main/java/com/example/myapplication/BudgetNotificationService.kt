package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BudgetNotificationService : Service() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Extract message from intent or use default
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Budget Tracker is running"

        // Start as foreground service (required for Android 8.0+)
        val notification = buildNotification(message)
        startForeground(NOTIFICATION_ID, notification)

        // Return sticky to keep service running
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for budget updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Budget Alert")
            .setContentText(message)
            .setSmallIcon(R.drawable.notify) // Ensure this icon exists
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "budget_channel"
        private const val NOTIFICATION_ID = 101
        private const val EXTRA_MESSAGE = "extra_message"

        /**
         * Start the service with an optional message
         */
        fun startService(context: Context, message: String? = null) {
            val intent = Intent(context, BudgetNotificationService::class.java).apply {
                message?.let { putExtra(EXTRA_MESSAGE, it) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Show a budget alert without starting a foreground service
         */
        fun showNotification(context: Context, message: String) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create channel if needed (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Notifications for budget updates" }
                notificationManager.createNotificationChannel(channel)
            }

            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Budget Alert")
                .setContentText(message)
                .setSmallIcon(R.drawable.notify)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
                .let { notificationManager.notify(NOTIFICATION_ID, it) }
        }
    }
}