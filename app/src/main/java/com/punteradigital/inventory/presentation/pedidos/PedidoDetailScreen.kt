package com.punteradigital.inventory.presentation.pedidos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class ScanResult {
    data class Success(val model: String, val size: String, val scanned: Int, val total: Int, val user: String) : ScanResult()
    data class WrongSize(val scannedSize: String, val expected: String) : ScanResult()
    data class WrongModel(val scannedModel: String, val expected: String) : ScanResult()
    data class AlreadyComplete(val model: String, val size: String, val total: Int) : ScanResult()
    data class NotFound(val uuid: String) : ScanResult()
    data class NotAvailable(val uuid: String, val status: String) : ScanResult()
    data class Duplicate(val uuid: String) : ScanResult()
    object Empty : ScanResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoDetailScreen(
    pedidoIndex: Int,
    onBack: () -> Unit,
    currentUserName: String = "José García",
    viewModel: com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
) {
    val pedidos = PedidoRepository.pedidos
    if (pedidoIndex !in pedidos.indices) {
        onBack()
        return
    }

    val pedido = pedidos[pedidoIndex]
    var scanInput by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf<ScanResult>(ScanResult.Empty) }
    var showCameraScanner by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsState()
    val isSupervisor = currentUser?.role == "ADMIN" || currentUser?.role == "SUPERVISOR_ALMACEN"
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Compute totals reactively
    val totalQty = pedido.lines.sumOf { it.qty }
    val totalScanned = pedido.lines.sumOf { it.scanned }
    val isComplete = totalScanned >= totalQty
    val overallPct = if (totalQty > 0) (totalScanned.toFloat() / totalQty * 100).toInt() else 0

    val statusColor = when (pedido.status) {
        "pendiente" -> StandByAmber
        "en-proceso" -> RefillBlue
        "completo" -> DispatchGreen
        "entregado" -> Color(0xFF009688)
        "retornado" -> CriticalRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Sensory flash states
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

    // Dynamic split filtering for order lines
    val pendingLines = remember(totalScanned) { pedido.lines.filter { it.scanned < it.qty } }
    val completedLines = remember(totalScanned) { pedido.lines.filter { it.scanned >= it.qty } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pedido ${pedido.id}", fontWeight = FontWeight.Bold)
                        Text(
                            pedido.client,
                            style = MaterialTheme.typography.bodySmall,
                            color = DispatchGreen
                        )
                    }
                },
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status + Creator header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                pedido.status.replace("-", " ").uppercase(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤 ", fontSize = 14.sp)
                            Text(pedido.creator, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Overall progress
                item {
                    KineticCard(padding = 16.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progreso General", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("$totalScanned/$totalQty pares", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DispatchGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isComplete) DispatchGreen else StandByAmber,
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                // Active Pending Lines
                if (pendingLines.isNotEmpty()) {
                    item {
                        Text(
                            "Líneas por Completar (${pendingLines.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StandByAmber,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(pendingLines, key = { "${it.model}_${it.size}" }) { line ->
                        PedidoLineRow(line)
                    }
                }

                // Completed Lines
                if (completedLines.isNotEmpty()) {
                    item {
                        Text(
                            "Líneas Completadas (${completedLines.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DispatchGreen,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(completedLines, key = { "${it.model}_${it.size}" }) { line ->
                        PedidoLineRow(line)
                    }
                }

                // Scan Input (hidden when complete or delivered/returned)
                if (!isComplete && pedido.status !in listOf("entregado", "retornado")) {
                    item {
                        Text(
                            "📷 Escanear Calzados",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KineticTextField(
                                value = scanInput,
                                onValueChange = { scanInput = it },
                                label = "UUID del par (ej: FS-2026A-42-001)",
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            FilledIconButton(
                                onClick = {
                                    scope.launch {
                                        val result = processScan(scanInput, pedido, currentUserName, viewModel)
                                        scanResult = result
                                        scanInput = ""
                                        if (result is ScanResult.Success) {
                                            showGreenFlash = true
                                            vibrateSuccess(context)
                                            viewModel.soundManager.playSuccessBeep()
                                        } else if (result !is ScanResult.Empty) {
                                            showRedFlash = true
                                            vibrateError(context)
                                            viewModel.soundManager.playErrorBeep()
                                        }
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Search, "Buscar")
                            }
                            FilledIconButton(
                                onClick = {
                                    showCameraScanner = true
                                },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("📷", fontSize = 24.sp)
                            }
                        }
                    }
                }

                // Scan Result Banner
                if (scanResult !is ScanResult.Empty) {
                    item {
                        ScanResultBanner(scanResult)
                    }
                }

                // Actions area (Confirm Delivery/Register Return)
                if (pedido.status !in listOf("entregado", "retornado")) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isComplete) DispatchGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isComplete) DispatchGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isComplete) {
                                    Text("🎉", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("¡Pedido Completo!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DispatchGreen)
                                    Text(
                                        "Todos los pares han sido escaneados correctamente.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text("📦", fontSize = 36.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Acciones de Pedido", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        "Progreso de escaneo: $totalScanned de $totalQty pares escaneados.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                if (isSupervisor) {
                                    KineticButton(
                                        text = "🚚 CONFIRMAR ENTREGA",
                                        onClick = {
                                            val scannedUuids = pedido.scannedPairs.map { it.uuid }
                                            viewModel.confirmPedidoDelivery(
                                                selectedUuids = scannedUuids,
                                                cliente = pedido.client,
                                                userId = currentUserName
                                            )
                                            pedido.status = "entregado"
                                            pedidos[pedidoIndex] = pedido.copy(status = "entregado")
                                        },
                                        type = ButtonType.PRIMARY
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    KineticButton(
                                        text = "↩ REGISTRAR RETORNO",
                                        onClick = {
                                            val scannedUuids = pedido.scannedPairs.map { it.uuid }
                                            viewModel.confirmPedidoReturn(
                                                selectedUuids = scannedUuids,
                                                userId = currentUserName
                                            )
                                            pedido.status = "retornado"
                                            pedidos[pedidoIndex] = pedido.copy(status = "retornado")
                                        },
                                        type = ButtonType.DANGER
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isComplete) DispatchGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)
                                    ) {
                                        Text(
                                            if (isComplete) "🎉 PEDIDO LISTO - Esperando confirmación del Supervisor" 
                                            else "⏳ PEDIDO EN PROCESO - Pendiente de escaneo completo",
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isComplete) DispatchGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Delivered/Returned status info cards
                if (pedido.status == "entregado") {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF009688).copy(alpha = 0.08f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🚚", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pedido Entregado", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF009688))
                                Text("Entrega confirmada por $currentUserName", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                if (pedido.status == "retornado") {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CriticalRed.copy(alpha = 0.08f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("↩", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Pedido Retornado", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CriticalRed)
                                Text("Retorno registrado por $currentUserName", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Scanned pairs history list
                item {
                    Text(
                        "Pares Escaneados (${pedido.scannedPairs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(pedido.scannedPairs) { sp ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = DispatchGreen.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = DispatchGreen.copy(alpha = 0.2f),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("✓", color = DispatchGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                sp.uuid,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                    Text(sp.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("👤 ${sp.user}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            // Screen sensory flashes overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showGreenFlash,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.3f)))
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showRedFlash,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.3f)))
            }
        }

        if (showCameraScanner) {
            CameraScannerDialog(
                onDismiss = { showCameraScanner = false },
                onScanSuccess = { scannedCode ->
                    showCameraScanner = false
                    scope.launch {
                        val result = processScan(scannedCode, pedido, currentUserName, viewModel)
                        scanResult = result
                        if (result is ScanResult.Success) {
                            showGreenFlash = true
                            vibrateSuccess(context)
                            viewModel.soundManager.playSuccessBeep()
                        } else if (result !is ScanResult.Empty) {
                            showRedFlash = true
                            vibrateError(context)
                            viewModel.soundManager.playErrorBeep()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PedidoLineRow(line: PedidoLine) {
    val pct = if (line.qty > 0) (line.scanned.toFloat() / line.qty * 100).toInt() else 0
    val lineColor = when {
        line.scanned >= line.qty -> DispatchGreen
        line.scanned > 0 -> StandByAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${line.model} · T${line.size}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${line.scanned}/${line.qty}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = lineColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        RoundedCornerShape(3.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = pct / 100f)
                        .background(lineColor, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
fun CameraScannerDialog(
    onDismiss: () -> Unit,
    onScanSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escanear Código QR / Barras", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
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
                                            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                barcodeScanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            barcode.rawValue?.let { qrValue ->
                                                                if (qrValue.isNotBlank()) {
                                                                    onScanSuccess(qrValue)
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
                                    Log.e("CameraScanner", "Camera error", exc)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Solicitando permiso de cámara...", color = Color.White, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CERRAR")
            }
        }
    )
}

// ═══ SCAN VALIDATION ENGINE (SMART STOCK-INTEGRATED) ═══

private suspend fun processScan(
    uuid: String,
    pedido: Pedido,
    currentUserName: String,
    viewModel: com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
): ScanResult {
    if (uuid.isBlank()) return ScanResult.Empty

    // 1. Check if already scanned in this order (prevents reuse of the same physical item)
    if (pedido.scannedPairs.any { it.uuid == uuid }) {
        return ScanResult.Duplicate(uuid)
    }

    // 2. Query product from DB
    val product = viewModel.dao.getProductByUuid(uuid)
    if (product == null) {
        return ScanResult.NotFound(uuid)
    }

    // 3. Verify availability in stock
    if (product.status != "AVAILABLE") {
        return ScanResult.NotAvailable(uuid, product.status)
    }

    val scannedModel = product.model
    val scannedSize = product.size

    // 4. Find matching line in order
    val matchingLine = pedido.lines.find { it.model == scannedModel && it.size == scannedSize }

    if (matchingLine == null) {
        val modelExists = pedido.lines.find { it.model == scannedModel }
        return if (modelExists != null) {
            val expected = pedido.lines.filter { it.model == scannedModel }.joinToString(", ") { "T${it.size}" }
            ScanResult.WrongSize(scannedSize, expected)
        } else {
            val expected = pedido.lines.map { it.model }.distinct().joinToString(", ")
            ScanResult.WrongModel(scannedModel, expected)
        }
    }

    // 5. Check if quantity is already full
    if (matchingLine.scanned >= matchingLine.qty) {
        return ScanResult.AlreadyComplete(matchingLine.model, matchingLine.size, matchingLine.qty)
    }

    // SUCCESS — update local progress
    matchingLine.scanned++
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    pedido.scannedPairs.add(0, ScannedPair(uuid, timeStr, currentUserName))

    if (pedido.status == "pendiente") {
        pedido.status = "en-proceso"
    }

    // Check if entire order is finished
    val totalQty = pedido.lines.sumOf { it.qty }
    val totalSc = pedido.lines.sumOf { it.scanned }
    if (totalSc >= totalQty) {
        pedido.status = "completo"
    }

    return ScanResult.Success(scannedModel, scannedSize, matchingLine.scanned, matchingLine.qty, currentUserName)
}

@Composable
private fun ScanResultBanner(result: ScanResult) {
    val (bgColor, iconText, title, detail) = when (result) {
        is ScanResult.Success -> Quadruple(
            DispatchGreen,
            "✅",
            "¡Correcto! ${result.model} T${result.size}",
            "${result.scanned}/${result.total} pares · Escaneado por ${result.user}"
        )
        is ScanResult.WrongSize -> Quadruple(
            CriticalRed,
            "❌",
            "Talla ${result.scannedSize} no coincide",
            "Se esperaba: ${result.expected}"
        )
        is ScanResult.WrongModel -> Quadruple(
            CriticalRed,
            "❌",
            "Modelo ${result.scannedModel} NO corresponde",
            "Este pedido requiere: ${result.expected}"
        )
        is ScanResult.AlreadyComplete -> Quadruple(
            StandByAmber,
            "⚠️",
            "Cantidad completa para T${result.size}",
            "Ya se escanearon ${result.total}/${result.total} pares de ${result.model}"
        )
        is ScanResult.NotFound -> Quadruple(
            CriticalRed,
            "❌",
            "Código QR no registrado",
            "El código ${result.uuid} no existe en la base de datos."
        )
        is ScanResult.NotAvailable -> Quadruple(
            CriticalRed,
            "⚠️",
            "Calzado no disponible",
            "El código ${result.uuid} no está disponible (Estado: ${result.status})."
        )
        is ScanResult.Duplicate -> Quadruple(
            StandByAmber,
            "⚠️",
            "Lectura duplicada",
            "El código ${result.uuid} ya fue escaneado en este pedido."
        )
        ScanResult.Empty -> return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, bgColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(iconText, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = bgColor)
                Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Log.e("PedidoDetail", "Vibrate error", e)
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
        Log.e("PedidoDetail", "Vibrate error", e)
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
