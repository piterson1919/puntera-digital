package com.punteradigital.inventory.presentation.scanner

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.punteradigital.inventory.domain.model.BajaReason
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.viewmodel.ScannedInfo
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.presentation.scanner.components.ManualScanDetailSheet
import com.punteradigital.inventory.presentation.scanner.components.ScannerCameraPreview
import com.punteradigital.inventory.presentation.scanner.components.vibrateError
import com.punteradigital.inventory.presentation.scanner.components.vibrateSuccess
import com.punteradigital.inventory.ui.theme.*
import com.punteradigital.inventory.presentation.viewmodel.confirmLabelEntry
import com.punteradigital.inventory.presentation.viewmodel.getScannedInfo
import com.punteradigital.inventory.presentation.viewmodel.processMuestra
import com.punteradigital.inventory.presentation.viewmodel.processQualityBaja
import com.punteradigital.inventory.presentation.viewmodel.processStandBy
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
    val isRackLocked by viewModel.isRackLocked.collectAsState()

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

    val isRapid = scanType == "RAPID" || (moduleName == "INBOUND_EMPAQUE" && isRackLocked)

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
            val isLocked = isRackLocked
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
            if (hasCameraPermission) {
                ScannerCameraPreview(
                    lifecycleOwner = lifecycleOwner,
                    isPaused = isPaused,
                    onQrScanned = ::processScannedUuid,
                    onCameraControlReady = { cameraControl = it }
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