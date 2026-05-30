package com.example.trackwell.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsMessage
import androidx.core.app.NotificationCompat
import com.example.trackwell.MainActivity
import java.util.regex.Pattern

class SmsTransactionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            try {
                val pdus = bundle.get("pdus") as Array<*>? ?: return
                val format = bundle.getString("format")
                for (pdu in pdus) {
                    val sms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                    val body = sms.messageBody ?: continue
                    parseAndNotify(context, body, sms.originatingAddress ?: "Unknown")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseAndNotify(context: Context, messageBody: String, sender: String) {
        // Regex patterns for transaction amounts and merchants
        val amountPatterns = listOf(
            Pattern.compile("(?i)(?:rs\\.?|inr|debited|spent)\\s*([0-9,]+(?:\\.[0-9]{2})?)"),
            Pattern.compile("(?i)(?:transaction of|paid|sent)\\s*(?:rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{2})?)")
        )
        
        val merchantPatterns = listOf(
            Pattern.compile("(?i)(?:to|at|info)\\s+([A-Za-z0-9\\s]{3,20})"),
            Pattern.compile("(?i)(?:ref|spent on|transfer to)\\s+([A-Za-z0-9\\s]{3,20})")
        )

        var amount = 0.0
        var merchant = "Unknown Merchant"

        // Parse amount
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(messageBody)
            if (matcher.find()) {
                val amtStr = matcher.group(1)?.replace(",", "")
                try {
                    amount = amtStr?.toDouble() ?: 0.0
                    break
                } catch (e: Exception) {
                    // Ignore parsing issues
                }
            }
        }

        // Only proceed if a valid transaction amount was parsed
        if (amount <= 0.0) return

        // Parse merchant
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(messageBody)
            if (matcher.find()) {
                val possibleMerchant = matcher.group(1)?.trim()
                if (!possibleMerchant.isNullOrEmpty()) {
                    merchant = possibleMerchant
                    break
                }
            }
        }

        sendTransactionNotification(context, amount, merchant)
    }

    private fun sendTransactionNotification(context: Context, amount: Double, merchant: String) {
        val channelId = "trackwell_sms_transactions"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SMS Auto-Transactions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart alerts for incoming payment messages via PhonePe, GPay, Paytm etc."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Launch MainActivity and pass the parsed payment data
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("sms_amount", amount)
            putExtra("sms_merchant", merchant)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle("New Payment Detected! 💰")
            .setContentText("Did you spend ₹${String.format("%.2f", amount)} at $merchant? Tap to record this.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(200, notification)
    }
}
