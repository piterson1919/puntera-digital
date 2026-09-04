package com.punteradigital.inventory.presentation.transfer

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.viewmodel.ScannedInfo
import com.punteradigital.inventory.ui.theme.DispatchGreen
import com.punteradigital.inventory.ui.theme.SpaceGrotesk
import com.punteradigital.inventory.ui.theme.StandByAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransferLog(
    val uuid: String,
    val model: String,
    val size: String,
    val type: String,
    val targetRack: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Selected target rack
    var targetRack by remember { mutableStateOf("A1") }
    var rackExpanded by remember { mutableStateOf(false) }
    val rackLocations = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3")

    // Session transfer log
    val transferLogs = remember { mutableStateListOf<TransferLog>() }

    // Flash states
    var showGreenFlash by remember { mutableStateOf(false) }
    var showRedFlash by remember { mutableStateOf(false) }

    LaunchedEffect(showGreenFlash) {
        if (showGreenFlash) {
            delay(250)
            showGreenFlash = false
        }
    }

    LaunchedEffect(showRedFlash) {
        if (showRedFlash) {
            delay(250)
            showRedFlash = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun processScannedUuid(uuid: String) {
        // Prevent double scans in rapid succession
        if (isPaused) return
        isPaused = true

        scope.launch {
            try {
                val info = viewModel.getScannedInfo(uuid)
                if (info != null) {
                    val model: String
                    val size: String
                    val type: String

                    when (info) {
                        is ScannedInfo.Master -> {
                            model = info.entity.model
                            size = info.entity.size
                            type = "Caja Master (${info.entity.activeChildCount} uds)"
                        }
                        is ScannedInfo.UnitInfo -> {
                            model = info.entity.model
                            size = info.entity.size
                            type = "Par Individual"
                        }
                        is ScannedInfo.Label -> {
                            model = info.entity.model
                            size = info.entity.size
                            type = "Etiqueta (${info.entity.labelType})"
                        }
                    }

                    // Execute transfer via ViewModel
                    viewModel.transferLocation(uuid, targetRack, user?.id ?: "UNKNOWN")

                    // Success Feedback
                    showGreenFlash = true
                    vibrateSuccess(context)
                    viewModel.soundManager.playSuccessBeep()

                    transferLogs.add(0, TransferLog(
                        uuid = uuid,
                        model = model,
                        size = size,
                        type = type,
                        targetRack = targetRack
                    ))
                } else {
                    // Code not found
                    showRedFlash = true
                    vibrateError(context)
                    viewModel.soundManager.playErrorBeep()
                    viewModel.setUiError("Código QR no registrado en el sistema: $uuid")
                }
            } catch (e: Exception) {
                showRedFlash = true
                vibrateError(context)
                viewModel.soundManager.playErrorBeep()
                viewModel.setUiError("Error al trasladar: ${e.message}")
            } finally {
                // Wait 1.2s before accepting next scan
                delay(1200)
                isPaused = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Traslado Interno (Re-ubicación)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Split layout: 40% Camera Scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color.Black)
            ) {
                if (hasCameraPermission) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        val barcodeScanner = BarcodeScanning.getClient()

                                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                            if (isPaused) {
                                                imageProxy.close()
                                                return@setAnalyzer
                                            }

                                            @androidx.annotation.OptIn(ExperimentalGetImage::class)
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                                barcodeScanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            barcode.rawValue?.let { qrValue ->
                                                                if (qrValue.startsWith("FS-") || qrValue.startsWith("SF-")) {
                                                                    processScannedUuid(qrValue)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    .addOnCompleteListener { imageProxy.close() }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview, imageAnalysis
                                    )
                                } catch (exc: Exception) {
                                    Log.e("Transfer", "Camera error", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Sensory Flashes
                androidx.compose.animation.AnimatedVisibility(
                    visible = showGreenFlash,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.4f)))
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showRedFlash,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.4f)))
                }

                // Scan overlay crosshairs
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .align(Alignment.Center)
                ) {
                    Text(
                        "ESCANEAR QR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                    )
                }

                // Status overlay
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        if (isPaused) "🔄 PROCESANDO..." else "📷 ESCÁNER ACTIVO",
                        color = if (isPaused) StandByAmber else Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Split layout: 60% Dashboard Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Target Rack Selector Card
                KineticCard(
                    padding = 16.dp
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                "Seleccionar Rack de Destino",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = rackExpanded,
                            onExpandedChange = { rackExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KineticTextField(
                                value = "📍 Trasladar a Rack: $targetRack",
                                onValueChange = {},
                                readOnly = true,
                                label = "Destino Seleccionado",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rackExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = rackExpanded,
                                onDismissRequest = { rackExpanded = false }
                            ) {
                                rackLocations.forEach { rack ->
                                    DropdownMenuItem(
                                        text = { Text("📍 Rack $rack") },
                                        onClick = {
                                            targetRack = rack
                                            rackExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Transfer log list
                Text(
                    "Traslados de la Sesión (${transferLogs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (transferLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Ningún traslado registrado aún",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Selecciona el rack destino y escanea códigos QR.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    val tf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(transferLogs) { log ->
                            KineticCard(
                                padding = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.uuid, style = MaterialTheme.typography.bodyMedium, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                                        Text("${log.type} | Modelo ${log.model} T. ${log.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DispatchGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "📍 ${log.targetRack}",
                                                color = DispatchGreen,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(tf.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Error snackbar/overlay if active
    if (uiState is InventoryUiState.Error) {
        val state = uiState as InventoryUiState.Error
        LaunchedEffect(uiState) {
            delay(3000)
            viewModel.resetUiState()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Text(state.message)
            }
        }
    }
}

private fun vibrateSuccess(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(150)
        }
    } catch (e: Exception) {
        Log.e("Transfer", "Vibrate error", e)
    }
}

private fun vibrateError(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } else {
            vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
        }
    } catch (e: Exception) {
        Log.e("Transfer", "Vibrate error", e)
    }
}
