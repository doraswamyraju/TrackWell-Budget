package com.example.trackwell.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackwell.TrackWellApplication
import com.example.trackwell.data.Transaction
import com.example.trackwell.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.text.SimpleDateFormat
import java.util.*

import com.example.trackwell.data.SmartAdvisor
import com.example.trackwell.data.AdvisorInsight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToOcr: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToBackupSettings: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    smsAmount: Double = -1.0,
    smsMerchant: String? = null,
    onClearSmsData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { (context.applicationContext as TrackWellApplication).repository }

    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    
    // Calculate values
    val currentMonthYear = remember { SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date()) }
    val budgets by repository.getBudgetsForMonth(currentMonthYear).collectAsState(initial = emptyList())

    val totalBudget = budgets.find { it.category == "TOTAL" }?.limitAmount ?: 2000.0
    val incomeTotal = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val expenseTotal = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    val budgetUsage = if (totalBudget > 0) (expenseTotal / totalBudget).toFloat() else 0f
    val animatedUsage by animateFloatAsState(
        targetValue = budgetUsage.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000)
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TrackWell",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "Smart Financial Dashboard",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryDark
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToBackupSettings,
                    modifier = Modifier
                        .background(CardDarkSecondary, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud Backup",
                        tint = NeonCyan
                    )
                }
                IconButton(
                    onClick = onNavigateToReminders,
                    modifier = Modifier
                        .background(CardDarkSecondary, CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Reminders",
                        tint = NeonCyan
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Interactive Chart Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Custom Canvas Chart
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            Canvas(modifier = Modifier.size(130.dp)) {
                                // Background circle
                                drawArc(
                                    color = CardDarkSecondary,
                                    startAngle = -220f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                                // Gradient progress ring
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(ElectricViolet, NeonCyan, EmeraldGreen)
                                    ),
                                    startAngle = -220f,
                                    sweepAngle = animatedUsage * 260f,
                                    useCenter = false,
                                    style = Stroke(width = 16f, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(budgetUsage * 100).toInt()}%",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Used",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        // Right Info Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondaryDark
                            )
                            Text(
                                text = "₹${String.format("%.2f", totalBudget)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(EmeraldGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Spent: ₹${String.format("%.2f", expenseTotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(NeonCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Income: ₹${String.format("%.2f", incomeTotal)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Panel
            item {
                Text(
                    text = "Smart Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "OCR Scanner",
                        subtitle = "Scan receipt",
                        icon = Icons.Default.PhotoCamera,
                        color = NeonCyan,
                        onClick = onNavigateToOcr,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Add Details",
                        subtitle = "Manual entry",
                        icon = Icons.Default.AddCircle,
                        color = ElectricViolet,
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Set Budgets",
                        subtitle = "Manage limits",
                        icon = Icons.Default.PieChart,
                        color = GoldYellow,
                        onClick = onNavigateToBudgets,
                        modifier = Modifier.weight(1f)
                    )
                    ActionCard(
                        title = "Reminders",
                        subtitle = "Auto Alarms",
                        icon = Icons.Default.Alarm,
                        color = SunsetOrange,
                        onClick = onNavigateToReminders,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        title = "Subscriptions",
                        subtitle = "Auto Tracker",
                        icon = Icons.Default.Loop,
                        color = EmeraldGreen,
                        onClick = onNavigateToSubscriptions,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Smart Advisor Panel
            item {
                Text(
                    text = "Smart Financial Coach 🧠",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                val insights = remember(transactions, budgets) {
                    SmartAdvisor.generateInsights(transactions, budgets)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    insights.forEach { insight ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (insight.isAlert) SunsetOrange.copy(alpha = 0.15f) else CardDark
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = insight.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (insight.isAlert) SunsetOrange else NeonCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = insight.description,
                                    fontSize = 12.sp,
                                    color = TextPrimaryDark,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Visual Spend Map & Category Dashboard
            item {
                Text(
                    text = "Visual Spend Distribution 📊",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        val expenses = transactions.filter { it.type == "EXPENSE" }
                        val totalExp = expenses.sumOf { it.amount }
                        val categories = expenses.groupBy { it.category }

                        if (totalExp == 0.0) {
                            Text(
                                text = "Add transactions to display your visual spend map.",
                                fontSize = 12.sp,
                                color = TextSecondaryDark,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            categories.forEach { (catName, catList) ->
                                val catTotal = catList.sumOf { it.amount }
                                val percentage = (catTotal / totalExp).toFloat()

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = catName, fontSize = 13.sp, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold)
                                        Text(text = "${String.format("%.0f", percentage * 100)}% (₹${String.format("%.2f", catTotal)})", fontSize = 12.sp, color = NeonCyan)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = percentage,
                                        color = if (catName.uppercase() == "FOOD") EmeraldGreen else if (catName.uppercase() == "UTILITIES") ElectricViolet else NeonCyan,
                                        trackColor = CardDarkSecondary,
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        modifier = Modifier.clickable { onNavigateToTransactions() }
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No records yet. Tap 'OCR Scanner' to begin!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                items(transactions.take(5)) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }

        if (smsAmount > 0.0) {
            var selectedCategory by remember { mutableStateOf("Others") }
            val categories = listOf("Food", "Utilities", "Shopping", "Entertainment", "Others")

            AlertDialog(
                onDismissRequest = onClearSmsData,
                title = {
                    Text(
                        text = "Auto-SMS Match Detected! 💰",
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Did you just spend ₹${String.format("%.2f", smsAmount)} at ${smsMerchant ?: "Unknown Merchant"}?",
                            color = TextPrimaryDark,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Select Category:",
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.take(3).forEach { cat ->
                                val isCatSelected = selectedCategory == cat
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = cat },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCatSelected) ElectricViolet else CardDarkSecondary
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            color = TextPrimaryDark,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.drop(3).forEach { cat ->
                                val isCatSelected = selectedCategory == cat
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = cat },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCatSelected) ElectricViolet else CardDarkSecondary
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 11.sp,
                                            color = TextPrimaryDark,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                repository.insertTransaction(
                                    Transaction(
                                        amount = smsAmount,
                                        category = selectedCategory,
                                        type = "EXPENSE",
                                        date = System.currentTimeMillis(),
                                        note = "Auto-tracked from SMS payment at ${smsMerchant ?: "Merchant"}"
                                    )
                                )
                                com.example.trackwell.widget.TrackWellWidget.forceUpdateWidget(context)
                                onClearSmsData()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Log Expense", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onClearSmsData) {
                        Text("Discard", color = SunsetOrange)
                    }
                },
                containerColor = CardDark,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    val isExpense = transaction.type == "EXPENSE"
    val formattedDate = remember(transaction.date) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transaction.date))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isExpense) SunsetOrange.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpense) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = transaction.type,
                        tint = if (isExpense) SunsetOrange else EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = transaction.category,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "$formattedDate${if (transaction.attachmentPath != null) " • 📷 Attached" else ""}",
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                    )
                }
            }
            Text(
                text = "${if (isExpense) "-" else "+"}₹${String.format("%.2f", transaction.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) SunsetOrange else EmeraldGreen
            )
        }
    }
}
