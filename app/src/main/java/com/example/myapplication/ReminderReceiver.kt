package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ReminderReceiver : BroadcastReceiver() {
    private val DAILY_REMINDER_CHANNEL_ID = "daily_transaction_reminder"
    private val DAILY_REMINDER_NOTIFICATION_ID = 3
    private val LAST_TRANSACTION_CHECK_KEY = "last_transaction_check"

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "CHECK_DAILY_TRANSACTIONS" -> checkDailyTransactions(context)
            else -> showDefaultReminder(context)
        }
    }

    private fun checkDailyTransactions(context: Context) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        // Get transactions from SharedPreferences
        val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
        val transactionsJson = prefs.getString("transactions", "[]")
        val type = object : TypeToken<List<Transaction>>() {}.type
        val transactions = Gson().fromJson<List<Transaction>>(transactionsJson, type)

        // Check if there are any transactions today
        val hasTransactionToday = transactions.any { it.date == today }

        // Check if we already reminded today
        val lastCheck = prefs.getString(LAST_TRANSACTION_CHECK_KEY, "") ?: ""
        
        if (!hasTransactionToday && lastCheck != today) {
            showDailyTransactionReminder(context)
            // Save that we reminded today
            prefs.edit().putString(LAST_TRANSACTION_CHECK_KEY, today).apply()
        }
    }

    private fun showDailyTransactionReminder(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DAILY_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.cutlery)
            .setContentTitle("Daily Transaction Reminder")
            .setContentText("Don't forget to add today's transactions!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(DAILY_REMINDER_NOTIFICATION_ID, notification)
    }

    private fun showDefaultReminder(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)

        val notification = NotificationCompat.Builder(context, "daily_reminders")
            .setSmallIcon(R.drawable.reminder)
            .setContentTitle("Expense Reminder")
            .setContentText("Don't forget to record today's expenses!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(2, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                DAILY_REMINDER_CHANNEL_ID,
                "Daily Transaction Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to add transactions daily"
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}