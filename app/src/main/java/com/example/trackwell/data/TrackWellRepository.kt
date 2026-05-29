package com.example.trackwell.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TrackWellRepository(private val db: AppDatabase) {

    val allTransactions: Flow<List<Transaction>> = db.transactionDao().getAllTransactions()
    val activeReminders: Flow<List<Reminder>> = db.reminderDao().getActiveReminders()
    val allReminders: Flow<List<Reminder>> = db.reminderDao().getAllReminders()

    fun getTransactionsForMonth(month: Int, year: Int): Flow<List<Transaction>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        return db.transactionDao().getTransactionsForPeriod(start, end)
    }

    fun getBudgetsForMonth(monthYear: String): Flow<List<Budget>> {
        return db.budgetDao().getBudgetsForMonth(monthYear)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        db.transactionDao().insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        db.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        db.transactionDao().deleteTransaction(transaction)
    }

    suspend fun insertBudget(budget: Budget) {
        db.budgetDao().insertBudget(budget)
    }

    suspend fun updateBudget(budget: Budget) {
        db.budgetDao().updateBudget(budget)
    }

    suspend fun deleteBudget(budget: Budget) {
        db.budgetDao().deleteBudget(budget)
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        return db.reminderDao().insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        db.reminderDao().updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        db.reminderDao().deleteReminder(reminder)
    }
}
