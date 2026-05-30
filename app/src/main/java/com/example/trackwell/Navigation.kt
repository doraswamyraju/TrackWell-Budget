package com.example.trackwell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.trackwell.ui.components.FloatingNavBar
import com.example.trackwell.ui.main.DashboardScreen
import com.example.trackwell.ui.main.OcrScreen
import com.example.trackwell.ui.main.TransactionScreen
import com.example.trackwell.ui.main.BudgetScreen
import com.example.trackwell.ui.main.ReminderScreen
import com.example.trackwell.ui.main.BackupSettingsScreen
import com.example.trackwell.ui.main.LoginScreen
import com.example.trackwell.ui.main.SubscriptionScreen
import com.example.trackwell.data.BackupManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember

@Composable
fun MainNavigation(
  smsAmount: Double,
  smsMerchant: String?,
  onClearSmsData: () -> Unit
) {
  val context = LocalContext.current
  val backupManager = remember { BackupManager(context) }
  val startDestination = remember {
    if (backupManager.getSignedInAccount() != null) Main else Login
  }

  val backStack = rememberNavBackStack(startDestination)
  val currentDestination = backStack.lastOrNull() ?: startDestination

  Box(modifier = Modifier.fillMaxSize()) {
    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Main> {
            DashboardScreen(
              onNavigateToOcr = { backStack.add(OcrScanner) },
              onNavigateToTransactions = { backStack.add(TransactionHistory) },
              onNavigateToBudgets = { backStack.add(BudgetPlanner) },
              onNavigateToReminders = { backStack.add(ReminderList) },
              onNavigateToBackupSettings = { backStack.add(BackupSettings) },
              onNavigateToSubscriptions = { backStack.add(Subscriptions) },
              smsAmount = smsAmount,
              smsMerchant = smsMerchant,
              onClearSmsData = onClearSmsData,
              modifier = Modifier.safeDrawingPadding().padding(16.dp).padding(bottom = 76.dp)
            )
          }
          entry<OcrScanner> {
            OcrScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp)
            )
          }
          entry<TransactionHistory> {
            TransactionScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp).padding(bottom = 76.dp)
            )
          }
          entry<BudgetPlanner> {
            BudgetScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp).padding(bottom = 76.dp)
            )
          }
          entry<ReminderList> {
            ReminderScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp).padding(bottom = 76.dp)
            )
          }
          entry<BackupSettings> {
            BackupSettingsScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp)
            )
          }
          entry<Login> {
            LoginScreen(
              onLoginSuccess = {
                // Clear login screen and navigate to Main Dashboard
                while (backStack.size > 0) {
                  backStack.removeLastOrNull()
                }
                backStack.add(Main)
              },
              modifier = Modifier.safeDrawingPadding().padding(16.dp)
            )
          }
          entry<Subscriptions> {
            SubscriptionScreen(
              onNavigateBack = { backStack.removeLastOrNull() },
              modifier = Modifier.safeDrawingPadding().padding(16.dp)
            )
          }
        },
    )

    // Only display floating bottom navbar on main destinations (hide on OCR, settings, login and subscriptions)
    if (currentDestination != OcrScanner && currentDestination != BackupSettings && currentDestination != Login && currentDestination != Subscriptions) {
      FloatingNavBar(
        currentDestination = currentDestination,
        onTabSelected = { key ->
          // Cleanly replace destination
          while (backStack.size > 1) {
            backStack.removeLastOrNull()
          }
          if (key != Main) {
            backStack.add(key)
          }
        },
        modifier = Modifier.align(Alignment.BottomCenter)
      )
    }
  }
}

