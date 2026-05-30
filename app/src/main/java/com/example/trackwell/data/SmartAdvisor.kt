package com.example.trackwell.data

import java.util.*

data class AdvisorInsight(
    val title: String,
    val description: String,
    val isAlert: Boolean = false
)

object SmartAdvisor {

    fun generateInsights(
        transactions: List<Transaction>,
        budgets: List<Budget>
    ): List<AdvisorInsight> {
        val insights = mutableListOf<AdvisorInsight>()
        
        val expenses = transactions.filter { it.type == "EXPENSE" }
        val income = transactions.filter { it.type == "INCOME" }
        
        val totalSpent = expenses.sumOf { it.amount }
        val totalIncome = income.sumOf { it.amount }
        
        val totalBudget = budgets.find { it.category == "TOTAL" }?.limitAmount ?: 2000.0

        // 1. Burn Rate & Prediction
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val dailyAverage = if (currentDay > 0) totalSpent / currentDay else 0.0
        val predictedSpend = dailyAverage * maxDays

        if (predictedSpend > totalBudget) {
            insights.add(
                AdvisorInsight(
                    title = "Over Budget Warning ⚠️",
                    description = "At your current spend velocity (₹${String.format("%.2f", dailyAverage)}/day), you are projected to spend ₹${String.format("%.2f", predictedSpend)} by end-of-month, exceeding your ₹${String.format("%.2f", totalBudget)} limit.",
                    isAlert = true
                )
            )
        } else {
            insights.add(
                AdvisorInsight(
                    title = "On Track! 🎯",
                    description = "You're spending an average of ₹${String.format("%.2f", dailyAverage)}/day. Your predicted monthly spend is ₹${String.format("%.2f", predictedSpend)}, keeping you safely within your budget.",
                    isAlert = false
                )
            )
        }

        // 2. High Category Spends
        val categoryGroups = expenses.groupBy { it.category }
        val highestCategory = categoryGroups.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
        
        if (highestCategory != null) {
            val catTotal = highestCategory.value.sumOf { it.amount }
            val catBudget = budgets.find { it.category.uppercase() == highestCategory.key.uppercase() }?.limitAmount
            
            if (catBudget != null) {
                val usage = catTotal / catBudget
                if (usage >= 0.8) {
                    insights.add(
                        AdvisorInsight(
                            title = "High Allocation Alert: ${highestCategory.key} 🚨",
                            description = "You have consumed ${String.format("%.0f", usage * 100)}% of your ₹${String.format("%.2f", catBudget)} budget allocated for ${highestCategory.key}.",
                            isAlert = true
                        )
                    )
                }
            } else {
                insights.add(
                    AdvisorInsight(
                        title = "Top Category: ${highestCategory.key} 🛍️",
                        description = "You've spent ₹${String.format("%.2f", catTotal)} on ${highestCategory.key} this month, making it your highest spending category.",
                        isAlert = false
                    )
                )
            }
        }

        // 3. Savings Rate
        if (totalIncome > 0) {
            val savingsRate = (totalIncome - totalSpent) / totalIncome
            if (savingsRate > 0.2) {
                insights.add(
                    AdvisorInsight(
                        title = "Super Saver Mode! 🐷",
                        description = "Incredible job! You've saved ${String.format("%.0f", savingsRate * 100)}% of your income so far. Consider putting this surplus into high-yield investments.",
                        isAlert = false
                    )
                )
            }
        }

        // 4. Default smart tips
        if (insights.size < 3) {
            insights.add(
                AdvisorInsight(
                    title = "Smart Subscription Check 💡",
                    description = "Analyze your recurring active subscriptions. De-activating just one unused membership can boost your annual savings rate.",
                    isAlert = false
                )
            )
        }

        return insights
    }
}
