package com.example.trackwell.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
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
import com.example.trackwell.data.Reminder
import com.example.trackwell.notifications.ReminderReceiver
import com.example.trackwell.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as TrackWellApplication).repository }
    val scope = rememberCoroutineScope()

    val reminders by repository.allReminders.collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }
    var inputAmount by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Utilities") }
    var inputMinutesInFuture by remember { mutableStateOf("1") } // Schedule delay in minutes for fast testing

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
                    text = "Payment Alarms",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            IconButton(
                onClick = {
                    inputTitle = ""
                    inputAmount = ""
                    inputCategory = "Utilities"
                    inputMinutesInFuture = "1"
                    showAddDialog = true
                },
                modifier = Modifier
                    .background(NeonCyan, CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = DeepObsidian)
            }
        }

        // Description
        Text(
            text = "TrackWell uses exact system hardware alarms to notify you offline immediately when bills are due. Tap '+' to schedule.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (reminders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.7f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No upcoming payment alarms configured.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                items(reminders) { reminder ->
                    val dueDateStr = remember(reminder.dueDate) {
                        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(reminder.dueDate))
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(ElectricViolet.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "Alarm",
                                        tint = ElectricViolet,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = reminder.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = "Due: $dueDateStr",
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$${String.format("%.2f", reminder.amount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            ReminderReceiver.cancelReminder(context, reminder.id)
                                            repository.deleteReminder(reminder)
                                        }
                                    },
                                    modifier = Modifier
                                        .background(SunsetOrange.copy(alpha = 0.15f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add, // rotated/using add for simple removal
                                        contentDescription = "Cancel Alarm",
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

    // Add Alarm Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule Payment Notification", color = NeonCyan) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Bill / Alert Title", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text("Amount ($)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputCategory,
                        onValueChange = { inputCategory = it },
                        label = { Text("Category", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputMinutesInFuture,
                        onValueChange = { inputMinutesInFuture = it },
                        label = { Text("Minutes from now to trigger", color = TextSecondaryDark) },
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
                        val amount = inputAmount.toDoubleOrNull() ?: 0.0
                        val min = inputMinutesInFuture.toLongOrNull() ?: 1L
                        if (amount > 0 && inputTitle.isNotEmpty()) {
                            scope.launch {
                                val triggerTime = System.currentTimeMillis() + (min * 60 * 1000)
                                val reminder = Reminder(
                                    title = inputTitle,
                                    amount = amount,
                                    dueDate = triggerTime,
                                    category = inputCategory
                                )
                                // Insert to database
                                val generatedId = repository.insertReminder(reminder)
                                // Schedule exact alarm
                                ReminderReceiver.scheduleReminder(
                                    context = context,
                                    id = generatedId.toInt(),
                                    title = inputTitle,
                                    amount = amount,
                                    category = inputCategory,
                                    timeInMillis = triggerTime
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Schedule Exact", color = DeepObsidian, fontWeight = FontWeight.Bold)
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
