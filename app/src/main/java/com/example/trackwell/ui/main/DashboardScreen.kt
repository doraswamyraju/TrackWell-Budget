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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToOcr: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToReminders: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                                text = "$${String.format("%.2f", totalBudget)}",
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
                                    text = "Spent: $${String.format("%.2f", expenseTotal)}",
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
                                    text = "Income: $${String.format("%.2f", incomeTotal)}",
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
                text = "${if (isExpense) "-" else "+"}$${String.format("%.2f", transaction.amount)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpense) SunsetOrange else EmeraldGreen
            )
        }
    }
}
