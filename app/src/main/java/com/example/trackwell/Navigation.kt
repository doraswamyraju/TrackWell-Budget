package com.example.trackwell

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.trackwell.ui.main.DashboardScreen
import com.example.trackwell.ui.main.OcrScreen
import com.example.trackwell.ui.main.TransactionScreen
import com.example.trackwell.ui.main.BudgetScreen
import com.example.trackwell.ui.main.ReminderScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)

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
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
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
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<BudgetPlanner> {
          BudgetScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
        entry<ReminderList> {
          ReminderScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            modifier = Modifier.safeDrawingPadding().padding(16.dp)
          )
        }
      },
  )
}
