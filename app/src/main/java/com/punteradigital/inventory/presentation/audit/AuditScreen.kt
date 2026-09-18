package com.punteradigital.inventory.presentation.audit

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
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.ui.theme.CriticalRed
import com.punteradigital.inventory.ui.theme.DispatchGreen
import com.punteradigital.inventory.ui.theme.SpaceGrotesk
import com.punteradigital.inventory.ui.theme.WarningOrange
import com.punteradigital.inventory.ui.theme.StandByAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
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

    // Selected rack to audit
    var selectedRack by remember { mutableStateOf("B1") }
    var rackExpanded by remember { mutableStateOf(false) }
    val rackLocations = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3")

    // Expected products in selected rack
    var expectedProducts by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    val scannedUuids = remember { mutableStateListOf<String>() }

    // Load expected products when selectedRack changes
    LaunchedEffect(selectedRack) {
        expectedProducts = viewModel.getProductsAtRack(selectedRack)
        scannedUuids.clear()
    }

    // Tabs for results
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Correctos", "Faltantes", "Extras")

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

    // Grouping
    val matchedProducts = expectedProducts.filter { it.uuid in scannedUuids }
    val missingProducts = expectedProducts.filter { it.uuid !in scannedUuids }
    val extraUuids = scannedUuids.filter { uuid -> expectedProducts.none { it.uuid == uuid } }

    fun processScannedUuid(uuid: String) {
        if (isPaused) return
        isPaused = true

        scope.launch {
            if (uuid in scannedUuids) {
                // Already scanned
                delay(800)
                isPaused = false
                return@launch
            }

            scannedUuids.add(uuid)

            // Sensory Feedback
            val isExpected = expectedProducts.any { it.uuid == uuid }
            if (isExpected) {
                showGreenFlash = true
                vibrateSuccess(context)
                viewModel.soundManager.playSuccessBeep()
            } else {
                // Extra item scanned at this location
                showGreenFlash = true // Flash green since it scanned successfully
                vibrateSuccess(context)
                viewModel.soundManager.playSuccessBeep()
            }

            delay(1000)
            isPaused = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auditoría de Racks", fontWeight = FontWeight.Bold) },
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
            // Split layout: 35% Camera Scanner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
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

                                        analysis.setAnalyzer(com.punteradigital.inventory.presentation.scanner.components.CameraAnalysisExecutor.executor) { imageProxy ->
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
                                    Log.e("Audit", "Camera error", exc)
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
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .align(Alignment.Center)
                )

                // Status overlay
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        if (isPaused) "🔄 PROCESANDO..." else "📷 ESCANEANDO RACK $selectedRack",
                        color = if (isPaused) StandByAmber else Color.Green,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            // Split layout: 65% Audit Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Select rack dropdown card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = rackExpanded,
                        onExpandedChange = { rackExpanded = it },
                        modifier = Modifier.weight(0.6f)
                    ) {
                        KineticTextField(
                            value = "📍 Auditar Rack: $selectedRack",
                            onValueChange = {},
                            readOnly = true,
                            label = "Rack Físico",
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
                                        selectedRack = rack
                                        rackExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Reset button
                    Button(
                        onClick = { scannedUuids.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.4f).height(56.dp)
                    ) {
                        Text("Reiniciar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Esperados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${expectedProducts.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DispatchGreen.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Escaneados", fontSize = 11.sp, color = DispatchGreen)
                            Text("${scannedUuids.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DispatchGreen)
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (missingProducts.isEmpty() && extraUuids.isEmpty()) DispatchGreen.copy(alpha = 0.15f) else CriticalRed.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Diferencia", fontSize = 11.sp, color = if (missingProducts.isEmpty() && extraUuids.isEmpty()) DispatchGreen else CriticalRed)
                            Text(
                                if (missingProducts.isEmpty() && extraUuids.isEmpty()) "OK" else "-${missingProducts.size} / +${extraUuids.size}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (missingProducts.isEmpty() && extraUuids.isEmpty()) DispatchGreen else CriticalRed
                            )
                        }
                    }
                }

                // Report Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        val count = when (index) {
                            0 -> matchedProducts.size
                            1 -> missingProducts.size
                            else -> extraUuids.size
                        }
                        val color = when (index) {
                            0 -> DispatchGreen
                            1 -> CriticalRed
                            else -> WarningOrange
                        }
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(title, fontSize = 12.sp)
                                    Badge(containerColor = color) {
                                        Text("$count", color = Color.White, fontSize = 10.sp)
                                    }
                                }
                            }
                        )
                    }
                }

                // Tab Content List
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val currentList: List<Any> = when (selectedTab) {
                        0 -> matchedProducts
                        1 -> missingProducts
                        else -> extraUuids
                    }

                    if (currentList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                when (selectedTab) {
                                    0 -> "Ningún par verificado aún"
                                    1 -> "¡Sin faltantes detectados!"
                                    else -> "Ningún par extra en este rack"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentList) { item ->
                                val uuid: String
                                val details: String
                                val badgeColor: Color

                                if (item is ProductEntity) {
                                    uuid = item.uuid
                                    details = "Modelo ${item.model} T. ${item.size} | Lote ${item.lot}"
                                    badgeColor = if (selectedTab == 0) DispatchGreen else CriticalRed
                                } else {
                                    uuid = item as String
                                    details = "Código escaneado extra en este rack"
                                    badgeColor = WarningOrange
                                }

                                KineticCard(padding = 10.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(uuid, style = MaterialTheme.typography.bodyMedium, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                                            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                when (selectedTab) {
                                                    0 -> "✓ OK"
                                                    1 -> "✗ FALTANTE"
                                                    else -> "+ EXTRA"
                                                },
                                                color = badgeColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Adjust System stock action button (only enabled if there are discrepancies)
                val hasDiscrepancy = missingProducts.isNotEmpty() || extraUuids.isNotEmpty()
                
                KineticButton(
                    text = "🔧 AJUSTAR STOCK SISTEMA",
                    onClick = {
                        viewModel.adjustAuditInventory(
                            location = selectedRack,
                            scannedUuids = scannedUuids.toList(),
                            missingUuids = missingProducts.map { it.uuid },
                            extraUuids = extraUuids,
                            userId = user?.id ?: "UNKNOWN"
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = hasDiscrepancy && uiState !is InventoryUiState.Loading,
                    isLoading = uiState is InventoryUiState.Loading,
                    type = ButtonType.PRIMARY
                )
            }
        }
    }

    // Success overlay or Snackbar
    if (uiState is InventoryUiState.SuccessMovement) {
        val state = uiState as InventoryUiState.SuccessMovement
        AlertDialog(
            onDismissRequest = {
                viewModel.resetUiState()
                scannedUuids.clear()
            },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = DispatchGreen, modifier = Modifier.size(48.dp)) },
            title = { Text("Ajuste Realizado", fontWeight = FontWeight.Bold) },
            text = { Text(state.message, textAlign = TextAlign.Center) },
            confirmButton = {
                KineticButton(
                    text = "ENTENDIDO",
                    onClick = {
                        viewModel.resetUiState()
                        scannedUuids.clear()
                        // Reload products
                        scope.launch {
                            expectedProducts = viewModel.getProductsAtRack(selectedRack)
                        }
                    }
                )
            }
        )
    }

    if (uiState is InventoryUiState.Error) {
        val state = uiState as InventoryUiState.Error
        LaunchedEffect(uiState) {
            delay(3000)
            viewModel.resetUiState()
        }
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
        Log.e("Audit", "Vibrate error", e)
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
        Log.e("Audit", "Vibrate error", e)
    }
}
