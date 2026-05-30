package com.example.trackwell.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackwell.TrackWellApplication
import com.example.trackwell.data.Transaction
import com.example.trackwell.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SubscriptionItem(
    val name: String,
    val amount: Double,
    val renewalDay: Int,
    val isAutoDetected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as TrackWellApplication).repository }
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputAmount by remember { mutableStateOf("") }
    var inputDay by remember { mutableStateOf("5") }

    // Manual subscriptions state
    val manualSubs = remember {
        mutableStateListOf(
            SubscriptionItem("Netflix Premium", 649.0, 12),
            SubscriptionItem("Spotify Duo", 179.0, 24)
        )
    }

    // Auto-detect subscriptions from recent database transactions
    val autoDetectedSubs = remember(transactions) {
        val detected = mutableListOf<SubscriptionItem>()
        val keywords = listOf("prime", "netflix", "spotify", "gym", "youtube", "cloud", "rent", "insurance", "apple")
        
        val expenses = transactions.filter { it.type == "EXPENSE" }
        expenses.forEach { tx ->
            val cleanNote = tx.note.lowercase()
            val cleanCat = tx.category.lowercase()
            
            val match = keywords.find { cleanNote.contains(it) || cleanCat.contains(it) }
            if (match != null) {
                // Determine day of month
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                val day = cal.get(Calendar.DAY_OF_MONTH)
                
                // Avoid duplicates by name
                if (detected.none { it.name.equals(match, ignoreCase = true) }) {
                    detected.add(
                        SubscriptionItem(
                            name = match.replaceFirstChar { it.uppercase() } + " Sync",
                            amount = tx.amount,
                            renewalDay = day,
                            isAutoDetected = true
                        )
                    )
                }
            }
        }
        detected
    }

    val allSubscriptions = remember(manualSubs.size, autoDetectedSubs.size) {
        manualSubs + autoDetectedSubs
    }

    val totalSubCost = remember(allSubscriptions) {
        allSubscriptions.sumOf { it.amount }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
            }
            Text(
                text = "Subscription Auto-Tracker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .background(NeonCyan, CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription", tint = DeepObsidian)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Total Cost Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Monthly Subscription Aggregate", fontSize = 14.sp, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "₹${String.format("%.2f", totalSubCost)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Loop, contentDescription = "Recurring", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${allSubscriptions.size} Recurring Subscriptions Active",
                        fontSize = 12.sp,
                        color = TextSecondaryDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Subscriptions Timeline List
        Text(
            text = "Renewal Calendar Timeline",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimaryDark,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allSubscriptions.sortedBy { it.renewalDay }) { sub ->
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
                                        if (sub.isAutoDetected) EmeraldGreen.copy(alpha = 0.15f) else ElectricViolet.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (sub.isAutoDetected) Icons.Default.SmartButton else Icons.Default.CalendarToday,
                                    contentDescription = "Subscription",
                                    tint = if (sub.isAutoDetected) EmeraldGreen else ElectricViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = sub.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = "Renews on day ${sub.renewalDay} of month" + if (sub.isAutoDetected) " (Auto-Synced)" else "",
                                    fontSize = 11.sp,
                                    color = TextSecondaryDark
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "₹${String.format("%.2f", sub.amount)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            if (!sub.isAutoDetected) {
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = { manualSubs.remove(sub) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SunsetOrange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add manual dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Recurring Subscription", color = NeonCyan) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Subscription Name", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text("Amount (₹)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputDay,
                        onValueChange = { inputDay = it },
                        label = { Text("Renewal Day of Month (1-31)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, focusedLabelColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = inputAmount.toDoubleOrNull() ?: 0.0
                        val day = inputDay.toIntOrNull() ?: 5
                        if (amount > 0 && inputName.isNotEmpty()) {
                            manualSubs.add(SubscriptionItem(inputName, amount, day))
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Register Sub", color = DeepObsidian, fontWeight = FontWeight.Bold)
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
