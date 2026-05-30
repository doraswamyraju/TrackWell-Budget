package com.example.trackwell.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.trackwell.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TrackWellFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("TrackWellFCM", "New registration token generated: $token")
        // Token can be sent to custom backend server or stored in SharedPreferences
        val prefs = getSharedPreferences("TrackWellPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("TrackWellFCM", "Message received from: ${remoteMessage.from}")

        // Check if message contains data payload
        val data = remoteMessage.data
        val title = data["title"] ?: remoteMessage.notification?.title ?: "TrackWell Premium Update ⚡"
        var body = data["body"] ?: remoteMessage.notification?.body ?: "Manage your budget and insights now."

        // Format any standard placeholder currency tags to Indian Rupee (₹)
        body = body.replace("$", "₹").replace("Rs.", "₹").replace("INR", "₹")

        val amountStr = data["amount"]
        val merchant = data["merchant"]

        if (!amountStr.isNullOrEmpty() && !merchant.isNullOrEmpty()) {
            try {
                val amount = amountStr.toDouble()
                sendTransactionPayloadNotification(amount, merchant, title, body)
            } catch (e: Exception) {
                sendGenericNotification(title, body)
            }
        } else {
            sendGenericNotification(title, body)
        }
    }

    private fun sendTransactionPayloadNotification(amount: Double, merchant: String, title: String, body: String) {
        val channelId = "trackwell_fcm_transactions"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FCM Smart Transactions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant financial push alerts with real-time budget synchronization."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap notification will trigger adding a transaction instantly
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("sms_amount", amount)
            putExtra("sms_merchant", merchant)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(300, notification)
    }

    private fun sendGenericNotification(title: String, body: String) {
        val channelId = "trackwell_fcm_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TrackWell Push Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant alerts for budget limits, bills, and system updates."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(301, notification)
    }
}
