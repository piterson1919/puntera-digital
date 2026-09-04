package com.punteradigital.inventory.presentation.scanner

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.punteradigital.inventory.domain.model.BajaReason
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.viewmodel.ScannedInfo
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Unified scanner screen supporting Manual and Rapid (Burst) scan modes
 * for Stand-By, Quality, and Verification modules.
 * Now includes mandatory Cliente/Observaciones modal for Stand-By.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedScannerScreen(
    viewModel: InventoryViewModel,
    moduleName: String, // STANDBY, QUALITY, VERIFY, INBOUND_EMPAQUE, VALIDATE_LABEL
    scanType: String,   // MANUAL, RAPID
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val origin by viewModel.currentOrigin.collectAsState()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Manual mode state
    var scannedResult by remember { mutableStateOf<ScannedInfo?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var currentQrCode by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var manualUuid by remember { mutableStateOf("") }

    // Rapid mode state
    val scannedUuids = remember { mutableStateListOf<String>() }
    var scanCount by remember { mutableIntStateOf(0) }

    // Quality mode state
    var selectedBajaReason by remember { mutableStateOf<BajaReason?>(null) }

    // Stand-By Cliente/Observaciones modal
    var showClienteModal by remember { mutableStateOf(false) }
    var pendingStandByUuid by remember { mutableStateOf("") }
    var clienteInput by remember { mutableStateOf("") }
    var observacionesInput by remember { mutableStateOf("") }

    // Verify mode state
    var verifyResult by remember { mutableStateOf<String?>(null) }

    // UUID-not-found feedback
    var notFoundUuid by remember { mutableStateOf<String?>(null) }

    // VALIDATE_LABEL validation success dialog
    var showValidationSuccessDialog by remember { mutableStateOf(false) }

    // Sensory feedback flash states
    var showGreenFlash by remember { mutableStateOf(false) }
    var showRedFlash by remember { mutableStateOf(false) }

    LaunchedEffect(showGreenFlash) {
        if (showGreenFlash) {
            kotlinx.coroutines.delay(250)
            showGreenFlash = false
        }
    }

    LaunchedEffect(showRedFlash) {
        if (showRedFlash) {
            kotlinx.coroutines.delay(250)
            showRedFlash = false
        }
    }

    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val moduleTitle = when (moduleName) {
        "STANDBY" -> "Stand-By"
        "QUALITY" -> "Calidad / Bajas"
        "VERIFY" -> "Verificar UUID"
        "INBOUND_EMPAQUE" -> "Ingreso QR Empaque"
        "VALIDATE_LABEL" -> "Validar Etiqueta"
        else -> moduleName
    }
    val moduleColor = when (moduleName) {
        "STANDBY" -> StandByAmber
        "QUALITY" -> QualityPurple
        "VERIFY" -> RefillBlue
        "INBOUND_EMPAQUE" -> DispatchGreen
        "VALIDATE_LABEL" -> MuestraTeal
        else -> MaterialTheme.colorScheme.primary
    }

    val isRapid = scanType == "RAPID" || (moduleName == "INBOUND_EMPAQUE" && viewModel.isRackLocked.value)

    fun processScannedUuid(uuid: String) {
        Log.d("Scanner", "processScannedUuid called with: $uuid, module=$moduleName, isRapid=$isRapid")

        if (moduleName == "VALIDATE_LABEL") {
            isPaused = true
            currentQrCode = uuid
            viewModel.soundManager.playSuccessBeep()
            showGreenFlash = true
            vibrateSuccess(context)
            showValidationSuccessDialog = true
            return
        }

        if (moduleName == "INBOUND_EMPAQUE") {
            val isLocked = viewModel.isRackLocked.value
            val lockedRackStr = viewModel.lockedRack.value ?: "A1"
            if (isLocked) {
                if (uuid in scannedUuids) return
                isPaused = true
                scope.launch {
                    val label = viewModel.dao.getLabelByUuid(uuid)
                    if (label == null) {
                        viewModel.soundManager.playErrorBeep()
                        showRedFlash = true
                        vibrateError(context)
                        viewModel.setUiError("Etiqueta no encontrada: $uuid")
                        isPaused = false
                    } else if (label.status == "ENTERED") {
                        viewModel.soundManager.playErrorBeep()
                        showRedFlash = true
                        vibrateError(context)
                        viewModel.setUiError("Etiqueta ya ingresada: $uuid")
                        isPaused = false
                    } else {
                        // Confirm immediately
                        viewModel.confirmLabelEntry(uuid, lockedRackStr, user?.id ?: "UNKNOWN")
                        scannedUuids.add(uuid)
                        scanCount++
                        showGreenFlash = true
                        vibrateSuccess(context)
                        // Wait a tiny bit and resume camera
                        kotlinx.coroutines.delay(800)
                        isPaused = false
                    }
                }
                return
            }

            // Normal Inbound Mode
            isPaused = true
            currentQrCode = uuid
            scope.launch {
                scannedResult = viewModel.getScannedInfo(uuid)
                if (scannedResult == null) {
                    Log.w("Scanner", "INBOUND_EMPAQUE: UUID not found: $uuid")
                    viewModel.soundManager.playErrorBeep()
                    showRedFlash = true
                    vibrateError(context)
                    notFoundUuid = uuid
                    isPaused = false
                } else if (scannedResult is ScannedInfo.Label) {
                    val label = (scannedResult as ScannedInfo.Label).entity
                    if (label.status == "ENTERED") {
                        viewModel.soundManager.playErrorBeep()
                        showRedFlash = true
                        vibrateError(context)
                        viewModel.setUiError("Esta etiqueta ya fue ingresada al almacén.")
                        isPaused = false
                    } else {
                        showDetailSheet = true
                    }
                } else {
                    viewModel.soundManager.playErrorBeep()
                    showRedFlash = true
                    vibrateError(context)
                    viewModel.setUiError("Este código ya está registrado en el inventario activo.")
                    isPaused = false
                }
            }
            return
        }

        if (moduleName == "VERIFY") {
            isPaused = true
            currentQrCode = uuid
            scope.launch {
                scannedResult = viewModel.getScannedInfo(uuid)
                if (scannedResult == null) {
                    Log.w("Scanner", "VERIFY: UUID not found in DB: $uuid")
                    viewModel.soundManager.playErrorBeep()
                    showRedFlash = true
                    vibrateError(context)
                    notFoundUuid = uuid
                    isPaused = false
                } else {
                    showGreenFlash = true
                    vibrateSuccess(context)
                    showDetailSheet = true
                }
            }
            return
        }

        if (isRapid) {
            if (uuid in scannedUuids) return

            when (moduleName) {
                "STANDBY" -> {
                    viewModel.processStandBy(uuid, user?.id ?: "UNKNOWN")
                    scannedUuids.add(uuid)
                    scanCount++
                    showGreenFlash = true
                    vibrateSuccess(context)
                }
                "QUALITY" -> {
                    if (selectedBajaReason != null) {
                        viewModel.processQualityBaja(uuid, selectedBajaReason!!, user?.id ?: "UNKNOWN")
                        scannedUuids.add(uuid)
                        scanCount++
                        showGreenFlash = true
                        vibrateSuccess(context)
                    }
                }
            }
            isPaused = false
        } else {
            // Manual mode: pause and show details
            isPaused = true
            currentQrCode = uuid
            scope.launch {
                scannedResult = viewModel.getScannedInfo(uuid)
                if (scannedResult == null) {
                    Log.w("Scanner", "MANUAL: UUID not found in DB: $uuid")
                    viewModel.soundManager.playErrorBeep()
                    showRedFlash = true
                    vibrateError(context)
                    notFoundUuid = uuid
                    isPaused = false
                } else {
                    Log.d("Scanner", "UUID found: $uuid -> ${scannedResult!!::class.simpleName}")
                    showGreenFlash = true
                    vibrateSuccess(context)
                    showDetailSheet = true
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("$moduleTitle · ${if (isRapid) "Ráfaga" else "Manual"}", fontWeight = FontWeight.Bold)
                        Text("Modo: ${origin.displayName}", style = MaterialTheme.typography.bodySmall, color = moduleColor)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Camera Preview
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
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview, imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                            } catch (exc: Exception) {
                                Log.e("Scanner", "Camera error", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Green/Red sensory feedback overlays
            AnimatedVisibility(
                visible = showGreenFlash,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.4f)))
            }

            AnimatedVisibility(
                visible = showRedFlash,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.4f)))
            }

            // ═══ RAPID / BURST MODE OVERLAY ═══
            if (isRapid) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = moduleColor,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("$scanCount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (moduleName == "INBOUND_EMPAQUE") "ingresados" else "escaneados",
                                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Quality reason selector (for rapid quality mode)
            if (moduleName == "QUALITY" && isRapid) {
                KineticCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .fillMaxWidth(0.9f),
                    padding = 16.dp
                ) {
                    Column {
                        Text("Motivo de Baja", style = MaterialTheme.typography.titleSmall, color = QualityPurple)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BajaReason.entries.forEach { reason ->
                                FilterChip(
                                    selected = selectedBajaReason == reason,
                                    onClick = { selectedBajaReason = reason },
                                    label = { Text(reason.displayName, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = QualityPurple,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Floating controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { isFlashOn = !isFlashOn; cameraControl?.enableTorch(isFlashOn) },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff, "Flash")
                }

                if (!isRapid) {
                    FloatingActionButton(
                        onClick = { showManualInput = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Keyboard, "Entrada manual")
                    }
                }
            }

            // ═══ POKA-YOKE ALERT ═══
            if (uiState is InventoryUiState.PokayokeAlert) {
                val alert = uiState as InventoryUiState.PokayokeAlert
                AlertDialog(
                    onDismissRequest = { viewModel.resetUiState() },
                    icon = { Icon(Icons.Default.Warning, null, tint = CriticalRed, modifier = Modifier.size(48.dp)) },
                    title = { Text("⚠ ALERTA POKA-YOKE", color = CriticalRed, fontWeight = FontWeight.Bold) },
                    text = { Text(alert.message, textAlign = TextAlign.Center) },
                    confirmButton = {
                        KineticButton(
                            text = "ENTENDIDO",
                            onClick = { viewModel.resetUiState() },
                            type = ButtonType.DANGER
                        )
                    }
                )
            }

            // Manual input dialog
            if (showManualInput) {
                AlertDialog(
                    onDismissRequest = { showManualInput = false },
                    title = { Text("Entrada Manual") },
                    text = {
                        Column {
                            Text("Ingrese el UUID del producto:")
                            Spacer(Modifier.height(8.dp))
                            KineticTextField(
                                value = manualUuid,
                                onValueChange = { manualUuid = it.uppercase() },
                                label = "UUID (FS-xxxx / SF-xxxx)",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        KineticButton(
                            text = "BUSCAR",
                            onClick = {
                                if (manualUuid.isNotEmpty()) {
                                    processScannedUuid(manualUuid)
                                    showManualInput = false
                                    manualUuid = ""
                                }
                            }
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showManualInput = false }) { Text("CANCELAR") }
                    }
                )
            }

            // Validation success dialog for VALIDATE_LABEL
            if (showValidationSuccessDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showValidationSuccessDialog = false
                        isPaused = false
                    },
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = DispatchGreen, modifier = Modifier.size(48.dp)) },
                    title = { Text("Etiqueta Válida", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("El código QR fue leído con éxito y es completamente legible.")
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    currentQrCode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        KineticButton(
                            text = "LEER OTRO",
                            onClick = {
                                showValidationSuccessDialog = false
                                isPaused = false
                            }
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showValidationSuccessDialog = false
                            onBack()
                        }) { Text("SALIR") }
                    }
                )
            }

            // Manual mode bottom sheet
            if (showDetailSheet && !isRapid) {
                ModalBottomSheet(
                    onDismissRequest = { showDetailSheet = false; isPaused = false },
                    sheetState = sheetState
                ) {
                    ManualScanDetailSheet(
                        viewModel = viewModel,
                        scannedInfo = scannedResult,
                        qrCode = currentQrCode,
                        moduleName = moduleName,
                        onConfirm = { reason, checkedChildUuids ->
                            when (moduleName) {
                                "STANDBY", "MUESTRA_LOOKUP" -> {
                                    pendingStandByUuid = currentQrCode
                                    showClienteModal = true
                                }
                                "QUALITY" -> {
                                    val bajaReason = BajaReason.entries.find { it.displayName == reason }
                                    if (bajaReason != null) {
                                        viewModel.processQualityBaja(currentQrCode, bajaReason, user?.id ?: "UNKNOWN")
                                    }
                                }
                                "INBOUND_EMPAQUE" -> {
                                    viewModel.confirmLabelEntry(currentQrCode, reason, user?.id ?: "UNKNOWN", checkedChildUuids)
                                    onBack()
                                }
                                "VERIFY" -> {
                                    // Just close
                                }
                            }
                            showDetailSheet = false
                            if (moduleName != "STANDBY" && moduleName != "MUESTRA_LOOKUP" && moduleName != "INBOUND_EMPAQUE") isPaused = false
                        },
                        onCancel = { showDetailSheet = false; isPaused = false }
                    )
                }
            }

            // ═══ CLIENTE/OBSERVACIONES MODAL (for Stand-By / Muestras) ═══
            if (showClienteModal) {
                val modalIcon = if (moduleName == "MUESTRA_LOOKUP") Icons.Default.Storefront else Icons.Default.Person
                val modalIconColor = if (moduleName == "MUESTRA_LOOKUP") MuestraTeal else StandByAmber
                val modalTitle = if (moduleName == "MUESTRA_LOOKUP") "Registrar Muestra" else "Datos de Stand-By"
                val modalButtonText = if (moduleName == "MUESTRA_LOOKUP") "REGISTRAR MUESTRA" else "CONFIRMAR STAND-BY"
                val modalButtonType = if (moduleName == "MUESTRA_LOOKUP") ButtonType.PRIMARY else ButtonType.WARNING

                AlertDialog(
                    onDismissRequest = {
                        showClienteModal = false
                        isPaused = false
                    },
                    icon = { Icon(modalIcon, null, tint = modalIconColor, modifier = Modifier.size(40.dp)) },
                    title = { Text(modalTitle, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "UUID: $pendingStandByUuid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            KineticTextField(
                                value = clienteInput,
                                onValueChange = { clienteInput = it },
                                label = "Cliente *",
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            KineticTextField(
                                value = observacionesInput,
                                onValueChange = { observacionesInput = it },
                                label = "Observaciones",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        KineticButton(
                            text = modalButtonText,
                            onClick = {
                                if (moduleName == "MUESTRA_LOOKUP") {
                                    viewModel.processMuestra(
                                        uuid = pendingStandByUuid,
                                        cliente = clienteInput,
                                        observaciones = observacionesInput,
                                        userId = user?.id ?: "UNKNOWN"
                                    )
                                } else {
                                    viewModel.processStandBy(
                                        uuid = pendingStandByUuid,
                                        userId = user?.id ?: "UNKNOWN",
                                        cliente = clienteInput,
                                        observaciones = observacionesInput
                                    )
                                }
                                showClienteModal = false
                                isPaused = false
                                clienteInput = ""
                                observacionesInput = ""
                            },
                            enabled = clienteInput.isNotBlank(),
                            type = modalButtonType
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showClienteModal = false
                            isPaused = false
                        }) { Text("CANCELAR") }
                    }
                )
            }

            // ═══ UUID NOT FOUND FEEDBACK ═══
            if (notFoundUuid != null) {
                AlertDialog(
                    onDismissRequest = { notFoundUuid = null },
                    icon = {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = CriticalRed,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            "UUID No Registrado",
                            fontWeight = FontWeight.Bold,
                            color = CriticalRed
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "El código escaneado no fue encontrado en la base de datos del sistema.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    notFoundUuid ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Text(
                                "Posibles causas:\n• El QR pertenece a otro sistema\n• El producto fue eliminado\n• Error de lectura del código",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        KineticButton(
                            text = "REINTENTAR",
                            onClick = { notFoundUuid = null },
                            type = ButtonType.PRIMARY
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { notFoundUuid = null }) {
                            Text("CERRAR")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScanDetailSheet(
    viewModel: InventoryViewModel,
    scannedInfo: ScannedInfo?,
    qrCode: String,
    moduleName: String,
    onConfirm: (String, List<String>?) -> Unit,
    onCancel: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var reasonExpanded by remember { mutableStateOf(false) }

    // Child breakdown query lists
    val childLabelsState = produceState<List<com.punteradigital.inventory.data.local.entity.LabelEntity>>(initialValue = emptyList(), scannedInfo) {
        if (scannedInfo is ScannedInfo.Label && scannedInfo.entity.labelType == "MASTER_BOX") {
            value = viewModel.dao.getChildrenLabels(scannedInfo.entity.uuid)
        }
    }

    val childProductsState = produceState<List<com.punteradigital.inventory.data.local.entity.ProductEntity>>(initialValue = emptyList(), scannedInfo) {
        if (scannedInfo is ScannedInfo.Master) {
            value = viewModel.dao.getChildrenOfMasterBox(scannedInfo.entity.uuid)
        }
    }

    val checkedUuids = remember { mutableStateListOf<String>() }

    LaunchedEffect(childLabelsState.value, childProductsState.value) {
        checkedUuids.clear()
        if (childLabelsState.value.isNotEmpty()) {
            checkedUuids.addAll(childLabelsState.value.map { it.uuid })
        }
        if (childProductsState.value.isNotEmpty()) {
            checkedUuids.addAll(childProductsState.value.map { it.uuid })
        }
    }

    val title = when (moduleName) {
        "STANDBY" -> "Confirmar Stand-By"
        "QUALITY" -> "Registrar Baja"
        "VERIFY" -> "Información del UUID"
        else -> "Confirmar Acción"
    }

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Product detail card
        KineticCard(
            padding = 16.dp
        ) {
            when (scannedInfo) {
                is ScannedInfo.Master -> {
                    Column {
                        Text("📦 Caja Master", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("UUID: $qrCode", style = MaterialTheme.typography.bodyMedium)
                        Text("Modelo: ${scannedInfo.entity.model}", style = MaterialTheme.typography.bodyMedium)
                        Text("Talla: ${scannedInfo.entity.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Unidades: ${scannedInfo.entity.activeChildCount}/${scannedInfo.entity.childCount}",
                            style = MaterialTheme.typography.bodyMedium)
                        if (scannedInfo.entity.status == "PENDIENTE_POR_RELLENAR") {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = WarningOrange.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "⚠ PENDIENTE POR RELLENAR",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningOrange
                                )
                            }
                        }
                        Text("Origen: ${scannedInfo.entity.origin}", style = MaterialTheme.typography.bodyMedium,
                            color = if (scannedInfo.entity.origin == "FOOT_SAFE") FootSafeYellow else SafetyCobalt)
                    }
                }
                is ScannedInfo.UnitInfo -> {
                    Column {
                        Text("👟 Unidad Individual", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("UUID: $qrCode", style = MaterialTheme.typography.bodyMedium)
                        Text("Modelo: ${scannedInfo.entity.model}", style = MaterialTheme.typography.bodyMedium)
                        Text("Talla: ${scannedInfo.entity.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Lote: ${scannedInfo.entity.lot}", style = MaterialTheme.typography.bodyMedium)
                        Text("Estado: ${scannedInfo.entity.status}", style = MaterialTheme.typography.bodyMedium)
                        Text("Origen: ${scannedInfo.entity.origin}", style = MaterialTheme.typography.bodyMedium,
                            color = if (scannedInfo.entity.origin == "FOOT_SAFE") FootSafeYellow else SafetyCobalt)
                        if (scannedInfo.entity.parentUuid != null) {
                            Text("Caja Master: ${scannedInfo.entity.parentUuid}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                is ScannedInfo.Label -> {
                    val label = scannedInfo.entity
                    Column {
                        Text("🏷 Etiqueta de Empaque", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("UUID: $qrCode", style = MaterialTheme.typography.bodyMedium)
                        Text("Modelo: ${label.model}", style = MaterialTheme.typography.bodyMedium)
                        Text("Talla: ${label.size}", style = MaterialTheme.typography.bodyMedium)
                        Text("Lote: ${label.lot}", style = MaterialTheme.typography.bodyMedium)
                        Text("Tipo: ${if (label.labelType == "MASTER_BOX") "CAJA MASTER" else "UNIDAD INDIVIDUAL"}", style = MaterialTheme.typography.bodyMedium)
                        Text("Origen: ${label.origin}", style = MaterialTheme.typography.bodyMedium,
                            color = if (label.origin == "FOOT_SAFE") FootSafeYellow else SafetyCobalt)
                    }
                }
                null -> {
                    Column {
                        Text("❌ No encontrado", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        Text("UUID: $qrCode", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // Checklist breakdown of Master Box
        if (childLabelsState.value.isNotEmpty() || childProductsState.value.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Desglose de Caja Master",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Desmarque los pares que falten físicamente:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            val totalChildCount = childLabelsState.value.size + childProductsState.value.size
            Text(
                "Contenido Verificado: ${checkedUuids.size} / $totalChildCount pares",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (checkedUuids.size == totalChildCount) DispatchGreen else WarningOrange
            )
            Spacer(Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (childLabelsState.value.isNotEmpty()) {
                        items(childLabelsState.value) { label ->
                            val isChecked = label.uuid in checkedUuids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .kineticClick {
                                        if (isChecked) checkedUuids.remove(label.uuid)
                                        else checkedUuids.add(label.uuid)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked == true) {
                                            if (label.uuid !in checkedUuids) checkedUuids.add(label.uuid)
                                        } else {
                                            checkedUuids.remove(label.uuid)
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(label.uuid, style = MaterialTheme.typography.bodyMedium, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold)
                                    Text("Talla ${label.size} | Lote ${label.lot}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(childProductsState.value) { product ->
                            val isChecked = product.uuid in checkedUuids
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .kineticClick {
                                        if (isChecked) checkedUuids.remove(product.uuid)
                                        else checkedUuids.add(product.uuid)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked == true) {
                                            if (product.uuid !in checkedUuids) checkedUuids.add(product.uuid)
                                        } else {
                                            checkedUuids.remove(product.uuid)
                                        }
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(product.uuid, style = MaterialTheme.typography.bodyMedium, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold)
                                    Text("Talla ${product.size} | Lote ${product.lot} | Estado ${product.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quality reason selector
        if (moduleName == "QUALITY") {
            Spacer(Modifier.height(16.dp))
            ExposedDropdownMenuBox(
                expanded = reasonExpanded,
                onExpandedChange = { reasonExpanded = it }
            ) {
                KineticTextField(
                    value = selectedReason,
                    onValueChange = {},
                    readOnly = true,
                    label = "Motivo de Baja *",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                    BajaReason.entries.forEach { reason ->
                        DropdownMenuItem(
                            text = { Text(reason.displayName) },
                            onClick = { selectedReason = reason.displayName; reasonExpanded = false }
                        )
                    }
                }
            }
        }

        // Rack selection for INBOUND_EMPAQUE
        if (moduleName == "INBOUND_EMPAQUE") {
            Spacer(Modifier.height(16.dp))
            var rackExpanded by remember { mutableStateOf(false) }
            val rackLocations = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "PISO")
            
            ExposedDropdownMenuBox(
                expanded = rackExpanded,
                onExpandedChange = { rackExpanded = it }
            ) {
                KineticTextField(
                    value = if (selectedReason.isEmpty()) "📍 Seleccionar Rack (ej: A1)" else "📍 Rack: $selectedReason",
                    onValueChange = {},
                    readOnly = true,
                    label = "Ubicación en Rack *",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rackExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = rackExpanded, onDismissRequest = { rackExpanded = false }) {
                    rackLocations.forEach { rack ->
                        DropdownMenuItem(
                            text = { Text(if (rack == "PISO") "📦 PISO (Sin rack)" else "📍 $rack") },
                            onClick = { 
                                selectedReason = rack
                                rackExpanded = false 
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        KineticButton(
            text = when (moduleName) {
                "VERIFY" -> "CERRAR"
                else -> "CONFIRMAR"
            },
            onClick = {
                val hasChildren = childLabelsState.value.isNotEmpty() || childProductsState.value.isNotEmpty()
                val checklistResult = if (hasChildren) checkedUuids.toList() else null
                
                if (moduleName == "QUALITY" || moduleName == "INBOUND_EMPAQUE") {
                    onConfirm(selectedReason, checklistResult)
                } else {
                    onConfirm("", checklistResult)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = scannedInfo != null && (
                moduleName == "VERIFY" || 
                (moduleName == "QUALITY" && selectedReason.isNotEmpty()) || 
                (moduleName == "INBOUND_EMPAQUE" && selectedReason.isNotEmpty()) ||
                (moduleName != "QUALITY" && moduleName != "INBOUND_EMPAQUE")
            ),
            type = if (moduleName == "VERIFY") ButtonType.SECONDARY else ButtonType.PRIMARY
        )

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun vibrateSuccess(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator?.vibrate(150)
        }
    } catch (e: Exception) {
        Log.e("Scanner", "Vibrate error", e)
    }
}
fun vibrateError(context: android.content.Context) {
    try {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300), -1))
        } else {
            vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
        }
    } catch (e: Exception) {
        Log.e("Scanner", "Vibrate error", e)
    }
}
