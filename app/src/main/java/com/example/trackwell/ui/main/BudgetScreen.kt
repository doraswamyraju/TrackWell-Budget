package com.example.trackwell.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackwell.TrackWellApplication
import com.example.trackwell.data.Budget
import com.example.trackwell.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as TrackWellApplication).repository }
    val scope = rememberCoroutineScope()

    val currentMonthYear = remember { SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date()) }
    val budgets by repository.getBudgetsForMonth(currentMonthYear).collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var inputCategory by remember { mutableStateOf("TOTAL") } // TOTAL or specific categories like Food
    var inputLimit by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .background(CardDark, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Monthly Budgets",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            IconButton(
                onClick = {
                    inputCategory = "TOTAL"
                    inputLimit = ""
                    showAddDialog = true
                },
                modifier = Modifier
                    .background(NeonCyan, CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Set Budget", tint = DeepObsidian)
            }
        }

        // Instructions
        Text(
            text = "Configure your monthly budget allocations below. Dynamic progress ring on the dashboard will update automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (budgets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No budgets set for this month.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a 'TOTAL' budget first to enable visual tracking on the Home screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryDark
                            )
                        }
                    }
                }
            } else {
                items(budgets) { budget ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = budget.category,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Text(
                                    text = "Period: $currentMonthYear",
                                    fontSize = 12.sp,
                                    color = TextSecondaryDark
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹${String.format("%.2f", budget.limitAmount)}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.deleteBudget(budget)
                                        }
                                    },
                                    modifier = Modifier
                                        .background(SunsetOrange.copy(alpha = 0.15f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add, // Rotate custom delete or use close
                                        contentDescription = "Delete",
                                        tint = SunsetOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Set Monthly Budget Limit", color = NeonCyan) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputCategory,
                        onValueChange = { inputCategory = it },
                        label = { Text("Category (e.g. TOTAL, Food)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputLimit,
                        onValueChange = { inputLimit = it },
                        label = { Text("Limit Amount (₹)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = inputLimit.toDoubleOrNull() ?: 0.0
                        if (limit > 0) {
                            scope.launch {
                                repository.insertBudget(
                                    Budget(
                                        monthYear = currentMonthYear,
                                        category = inputCategory.uppercase(),
                                        limitAmount = limit
                                    )
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Save Limit", color = DeepObsidian, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            },
            containerColor = CardDark
        )
    }
}
