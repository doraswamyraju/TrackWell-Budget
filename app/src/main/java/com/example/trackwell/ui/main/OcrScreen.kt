package com.example.trackwell.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.trackwell.TrackWellApplication
import com.example.trackwell.data.Transaction
import com.example.trackwell.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { (context.applicationContext as TrackWellApplication).repository }

    // Camera permissions
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera State
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturedImageFile by remember { mutableStateOf<File?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // OCR Results
    var isProcessing by remember { mutableStateOf(false) }
    var extractedAmount by remember { mutableStateOf(0.0) }
    var extractedCategory by remember { mutableStateOf("Shopping") }
    var extractedNote by remember { mutableStateOf("OCR Bill Scan") }
    var showResultsSheet by remember { mutableStateOf(false) }

    // Coroutine scope for saving transaction
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepObsidian)
    ) {
        if (capturedBitmap == null) {
            // Camera Preview Screen
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (exc: Exception) {
                                Log.e("TrackWellOCR", "Use case binding failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // overlay details for scanner
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(CardDark.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }

                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Align bill within camera and tap capture",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(CardDark.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        IconButton(
                            onClick = {
                                val photoFile = File(
                                    context.cacheDir,
                                    "receipt_${System.currentTimeMillis()}.jpg"
                                )
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                imageCapture?.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            capturedImageFile = photoFile
                                            capturedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                            // Trigger OCR immediately
                                            capturedBitmap?.let {
                                                runOcrOnImage(context, it) { amount, category, note ->
                                                    extractedAmount = amount
                                                    extractedCategory = category
                                                    extractedNote = note
                                                    showResultsSheet = true
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("TrackWellOCR", "Photo capture failed", exception)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .background(NeonCyan, CircleShape)
                                .size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Capture",
                                tint = DeepObsidian,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Please grant camera permission to scan bills", color = Color.White)
                }
            }
        } else {
            // Receipt Review and OCR confirmation page
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Receipt Captured",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                capturedBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Receipt preview",
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = {
                            capturedBitmap = null
                            capturedImageFile = null
                            showResultsSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardDarkSecondary)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Discard")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retake", color = TextPrimaryDark)
                    }
                }
            }
        }

        // Overlay dialog / Bottom Sheet for captured values
        if (showResultsSheet) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Smart OCR Bill Extracted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = extractedAmount.toString(),
                        onValueChange = { extractedAmount = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("Amount (₹)", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = extractedCategory,
                        onValueChange = { extractedCategory = it },
                        label = { Text("Category", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = extractedNote,
                        onValueChange = { extractedNote = it },
                        label = { Text("Note / Vendor", color = TextSecondaryDark) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardDarkSecondary,
                            focusedLabelColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                // Copy the file to app internal storage directory for persistence
                                val savedFile = copyFileToInternalStorage(context, capturedImageFile!!)
                                
                                val transaction = Transaction(
                                    amount = extractedAmount,
                                    category = extractedCategory,
                                    type = "EXPENSE",
                                    date = System.currentTimeMillis(),
                                    note = extractedNote,
                                    attachmentPath = savedFile?.absolutePath
                                )
                                repository.insertTransaction(transaction)
                                // Reset and go back
                                withContext(Dispatchers.Main) {
                                    capturedBitmap = null
                                    capturedImageFile = null
                                    showResultsSheet = false
                                    onNavigateBack()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = DeepObsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Payment to Budget", color = DeepObsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Smart local receipt text parsing algorithms
private fun runOcrOnImage(context: Context, bitmap: Bitmap, callback: (Double, String, String) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val fullText = visionText.text
            Log.d("TrackWellOCR", "Extracted Text: $fullText")

            var maxAmount = 0.0
            var finalCategory = "Shopping"
            var finalVendor = "Receipt Purchase"

            // Amount extraction regex patterns
            val lines = fullText.split("\n")
            val amountRegex = Regex("""(?:\$|USD|total|net|sum|due)?\s*(\d+[\.,]\d{2})\b""", RegexOption.IGNORE_CASE)

            for (line in lines) {
                // Match lines having 'total', 'sum', 'due' to prioritize amount finding
                if (line.contains("total", ignoreCase = true) || line.contains("sum", ignoreCase = true) || line.contains("due", ignoreCase = true) || line.contains("pay", ignoreCase = true)) {
                    val match = amountRegex.find(line)
                    if (match != null) {
                        val amt = match.groupValues[1].replace(",", ".").toDoubleOrNull()
                        if (amt != null && amt > maxAmount) {
                            maxAmount = amt
                        }
                    }
                }
            }

            // Fallback: search overall text for highest decimal amount
            if (maxAmount == 0.0) {
                val matches = amountRegex.findAll(fullText)
                for (match in matches) {
                    val amt = match.groupValues[1].replace(",", ".").toDoubleOrNull()
                    if (amt != null && amt > maxAmount) {
                        maxAmount = amt
                    }
                }
            }

            // Smart category tag matching
            val lowercaseText = fullText.lowercase()
            when {
                lowercaseText.contains("cafe") || lowercaseText.contains("coffee") || lowercaseText.contains("restaurant") || lowercaseText.contains("food") || lowercaseText.contains("dining") -> {
                    finalCategory = "Food"
                }
                lowercaseText.contains("uber") || lowercaseText.contains("taxi") || lowercaseText.contains("metro") || lowercaseText.contains("fuel") || lowercaseText.contains("gas") -> {
                    finalCategory = "Travel"
                }
                lowercaseText.contains("electric") || lowercaseText.contains("power") || lowercaseText.contains("water") || lowercaseText.contains("internet") || lowercaseText.contains("phone") || lowercaseText.contains("bill") -> {
                    finalCategory = "Utilities"
                }
                lowercaseText.contains("movie") || lowercaseText.contains("netflix") || lowercaseText.contains("ticket") || lowercaseText.contains("game") -> {
                    finalCategory = "Entertainment"
                }
            }

            // Extract Vendor name: use the first line or capital word sequence
            if (lines.isNotEmpty()) {
                val candidate = lines.first().trim()
                if (candidate.length > 2 && candidate.length < 30) {
                    finalVendor = candidate
                }
            }

            callback(maxAmount, finalCategory, finalVendor)
        }
        .addOnFailureListener { e ->
            Log.e("TrackWellOCR", "OCR process failed", e)
            callback(0.0, "Shopping", "Failed to parse receipt")
        }
}

// Copy internal files helper for persistence
private fun copyFileToInternalStorage(context: Context, sourceFile: File): File? {
    return try {
        val destFile = File(context.filesDir, "receipt_${System.currentTimeMillis()}.jpg")
        val input = sourceFile.inputStream()
        val output = FileOutputStream(destFile)
        input.copyTo(output)
        input.close()
        output.close()
        destFile
    } catch (e: Exception) {
        Log.e("TrackWellOCR", "Failed to save receipt image permanently", e)
        null
    }
}
