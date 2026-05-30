package com.example.trackwell.ui.main

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackwell.TrackWellApplication
import com.example.trackwell.data.Transaction
import com.example.trackwell.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as TrackWellApplication).repository }
    val transactions by repository.allTransactions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Form Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("Food") }
    var inputType by remember { mutableStateOf("EXPENSE") } // INCOME or EXPENSE
    var inputNote by remember { mutableStateOf("") }
    var selectedImagePath by remember { mutableStateOf<String?>(null) }

    // Pick image from gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = copyUriToInternalStorage(context, it)
            selectedImagePath = file?.absolutePath
        }
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
                    text = "Transactions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            IconButton(
                onClick = {
                    inputAmount = ""
                    inputCategory = "Food"
                    inputType = "EXPENSE"
                    inputNote = ""
                    selectedImagePath = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .background(NeonCyan, CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction", tint = DeepObsidian)
            }
        }

        // List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.7f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No financial records. Click '+' to add manually.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark
                        )
                    }
                }
            } else {
                items(transactions) { transaction ->
                    var showDetailSheet by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDetailSheet = true },
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
                                            if (transaction.type == "EXPENSE") SunsetOrange.copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (transaction.type == "EXPENSE") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = transaction.type,
                                        tint = if (transaction.type == "EXPENSE") SunsetOrange else EmeraldGreen,
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
                                        text = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(transaction.date)),
                                        fontSize = 11.sp,
                                        color = TextSecondaryDark
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (transaction.attachmentPath != null) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Attachment",
                                        tint = NeonCyan,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(end = 8.dp)
                                    )
                                }
                                Text(
                                    text = "${if (transaction.type == "EXPENSE") "-" else "+"}₹${String.format("%.2f", transaction.amount)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (transaction.type == "EXPENSE") SunsetOrange else EmeraldGreen
                                )
                            }
                        }
                    }

                    // Detail Sheet
                    if (showDetailSheet) {
                        AlertDialog(
                            onDismissRequest = { showDetailSheet = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    scope.launch {
                                        repository.deleteTransaction(transaction)
                                        showDetailSheet = false
                                    }
                                }) {
                                    Text("Delete Record", color = SunsetOrange)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDetailSheet = false }) {
                                    Text("Close", color = TextSecondaryDark)
                                }
                            },
                            title = { Text(transaction.category, color = NeonCyan) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Type: ${transaction.type}", color = TextPrimaryDark, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Amount: ₹${String.format("%.2f", transaction.amount)}", color = TextPrimaryDark)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Note: ${transaction.note}", color = TextSecondaryDark)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    if (transaction.attachmentPath != null) {
                                        val file = File(transaction.attachmentPath)
                                        if (file.exists()) {
                                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                            if (bitmap != null) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Receipt Attachment",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            containerColor = CardDark
                        )
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Manual Entry", color = NeonCyan) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDark),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { inputType = "EXPENSE" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (inputType == "EXPENSE") SunsetOrange else CardDarkSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Expense", color = TextPrimaryDark)
                        }
                        Button(
                            onClick = { inputType = "INCOME" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (inputType == "INCOME") EmeraldGreen else CardDarkSecondary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Income", color = TextPrimaryDark)
                        }
                    }

                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        label = { Text("Amount (₹)", color = TextSecondaryDark) },
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
                        label = { Text("Category (e.g. Food, Utilities)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        label = { Text("Note / Detail", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Image attachment button
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = CardDarkSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedImagePath != null) "📷 Image Attached" else "Attach Receipt Photo", color = TextPrimaryDark)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = inputAmount.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            scope.launch {
                                repository.insertTransaction(
                                    Transaction(
                                        amount = amount,
                                        category = inputCategory,
                                        type = inputType,
                                        date = System.currentTimeMillis(),
                                        note = inputNote,
                                        attachmentPath = selectedImagePath
                                    )
                                )
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Add", color = DeepObsidian, fontWeight = FontWeight.Bold)
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

// Copy picture helper for gallery selections
private fun copyUriToInternalStorage(context: Context, uri: Uri): File? {
    return try {
        val destFile = File(context.filesDir, "receipt_attach_${System.currentTimeMillis()}.jpg")
        val inputStream = context.contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(destFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        destFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
