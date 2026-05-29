package com.example.trackwell

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object OcrScanner : NavKey
@Serializable data object TransactionHistory : NavKey
@Serializable data object BudgetPlanner : NavKey
@Serializable data object ReminderList : NavKey
