package com.example.trackwell.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.trackwell.MainActivity
import com.example.trackwell.R
import com.example.trackwell.TrackWellApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackWellWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Force update widget on action trigger
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TrackWellWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.trackwell_widget_layout)

        // Setup click intent to launch MainActivity
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Asynchronously load actual database data
        val app = context.applicationContext as? TrackWellApplication
        if (app != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val transactions = app.repository.allTransactions.first()
                    val expenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    
                    val monthYear = java.text.SimpleDateFormat("MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
                    val budgets = app.repository.getBudgetsForMonth(monthYear).first()
                    val totalLimit = budgets.find { it.category == "TOTAL" }?.limitAmount ?: 2000.0
                    val remaining = totalLimit - expenses

                    views.setTextViewText(
                        R.id.widget_amount,
                        "₹${String.format("%.2f", remaining)} Left"
                    )
                    views.setTextViewText(
                        R.id.widget_desc,
                        "Remaining out of ₹${String.format("%.0f", totalLimit)} Limit"
                    )

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        fun forceUpdateWidget(context: Context) {
            val intent = Intent(context, TrackWellWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
