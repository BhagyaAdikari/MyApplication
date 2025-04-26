package com.example.myapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(context)

        val notification = NotificationCompat.Builder(context, "daily_reminders")
            .setSmallIcon(R.drawable.reminder)
            .setContentTitle("Expense Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,  // <- Changed from 'this' to 'context'
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // For BroadcastReceivers, we can't request permissions here
            // Just return without showing the notification
            return
        }
        notificationManager.notify(2, notification)
    }
}