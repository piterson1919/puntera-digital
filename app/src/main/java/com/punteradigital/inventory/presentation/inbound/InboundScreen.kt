package com.punteradigital.inventory.presentation.inbound

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.inbound.components.*
import com.punteradigital.inventory.presentation.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    viewModel: InventoryViewModel,
    onNavigateToScanner: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val origin by viewModel.currentOrigin.collectAsState()

    val isRackLocked by viewModel.isRackLocked.collectAsState()
    val lockedRack by viewModel.lockedRack.collectAsState()

    LaunchedEffect(isRackLocked) {
        if (isRackLocked && viewModel.lockedRack.value == null) {
            viewModel.setLockedRack("A1")
        }
    }

    var selectedOrigin by remember { mutableStateOf(Origin.FOOT_SAFE) }
    var selectedEntryType by remember { mutableStateOf("Producción") }
    val entryTypes = listOf("Producción", "Ajuste", "Traslado")
    var selectedModel by remember { mutableStateOf("") }
    val catalogModels by viewModel.catalogModels.collectAsState(initial = emptyList())
    val models = catalogModels.filter { it.isActive }.map { it.code }
    var selectedSize by remember { mutableStateOf("") }
    val sizes = (34..48).map { it.toString() }
    var lot by remember { mutableStateOf("") }
    var isMasterBox by remember { mutableStateOf(true) }
    var childCount by remember { mutableStateOf(BusinessRules.DEFAULT_MASTER_QTY.toString()) }
    var totalQuantity by remember { mutableStateOf("") }
    var remainderMode by remember { mutableStateOf(RemainderMode.LOOSE) }
    var selectedRack by remember { mutableStateOf("A1") }
    val rackLocations = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "PISO")
    var showConfirmDialog by remember { mutableStateOf(false) }

    val totalQty = totalQuantity.toIntOrNull() ?: 0
    val pairsPerBox = childCount.toIntOrNull() ?: BusinessRules.DEFAULT_MASTER_QTY
    val autoBox = if (isMasterBox && totalQty > 0 && pairsPerBox > 0)
        BusinessRules.calculateAutoBoxing(totalQty, pairsPerBox) else null

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(selectedOrigin) {
        viewModel.setOrigin(selectedOrigin)
    }

    LaunchedEffect(models) {
        if (selectedModel.isNotBlank() && selectedModel !in models) {
            selectedModel = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Registro de Nacimiento", fontWeight = FontWeight.Bold)
                    }
                },
                windowInsets = WindowInsets.statusBars,
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
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isLandscape) 12.dp else 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 16.dp)
                ) {
                    LockedRackSettings(viewModel, isRackLocked, lockedRack, rackLocations)
                    
                    OriginSelectorCard(selectedOrigin) { selectedOrigin = it }

                    DataCaptureForm(
                        isLandscape = isLandscape,
                        selectedEntryType = selectedEntryType,
                        onEntryTypeChange = { selectedEntryType = it },
                        entryTypes = entryTypes,
                        selectedModel = selectedModel,
                        onModelChange = { selectedModel = it },
                        models = models,
                        selectedSize = selectedSize,
                        onSizeChange = { selectedSize = it },
                        sizes = sizes,
                        lot = lot,
                        onLotChange = { lot = it },
                        selectedRack = selectedRack,
                        onRackChange = { selectedRack = it },
                        rackLocations = rackLocations,
                        totalQuantity = totalQuantity,
                        onTotalQuantityChange = { totalQuantity = it },
                        isMasterBox = isMasterBox,
                        onMasterBoxChange = { isMasterBox = it },
                        childCount = childCount,
                        onChildCountChange = { childCount = it },
                        remainderMode = remainderMode,
                        onRemainderModeChange = { remainderMode = it }
                    )

                    val isButtonEnabled = selectedModel.isNotBlank() && selectedSize.isNotBlank() && lot.isNotBlank() && totalQty > 0 && uiState !is InventoryUiState.Loading
                    
                    KineticButton(
                        text = if (uiState is InventoryUiState.Loading) "PROCESANDO..." else "🖨 REGISTRAR E IMPRIMIR",
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        enabled = isButtonEnabled,
                        isLoading = uiState is InventoryUiState.Loading,
                        isSuccess = uiState is InventoryUiState.SuccessEntry,
                        type = ButtonType.PRIMARY
                    )

                    Spacer(Modifier.height(80.dp))
                }

                if (showConfirmDialog) {
                    EntryConfirmationDialog(
                        selectedOrigin = selectedOrigin,
                        selectedModel = selectedModel,
                        selectedSize = selectedSize,
                        lot = lot,
                        selectedRack = selectedRack,
                        totalQty = totalQty,
                        isMasterBox = isMasterBox,
                        pairsPerBox = pairsPerBox,
                        autoBox = autoBox,
                        onConfirm = {
                            showConfirmDialog = false
                            viewModel.evaluateSmartEntry(
                                origin = selectedOrigin,
                                model = selectedModel,
                                size = selectedSize,
                                lot = lot,
                                entryType = selectedEntryType,
                                totalQuantity = if (totalQty > 0) totalQty else pairsPerBox,
                                isMasterBox = isMasterBox,
                                childCount = pairsPerBox,
                                remainderMode = remainderMode,
                                userId = user?.id ?: "UNKNOWN"
                            )
                        },
                        onDismiss = { showConfirmDialog = false }
                    )
                }
            }

            if (uiState is InventoryUiState.SuccessEntry) {
                val state = uiState as InventoryUiState.SuccessEntry
                EntrySuccessOverlay(
                    model = state.model,
                    uuids = state.uuids,
                    origin = state.origin,
                    message = state.message,
                    warning = state.warning,
                    onDismiss = { 
                        viewModel.resetUiState()
                        lot = ""
                        totalQuantity = ""
                    }
                )
            }

            if (uiState is InventoryUiState.SmartEntrySuggestion) {
                val suggestion = uiState as InventoryUiState.SmartEntrySuggestion
                SmartSuggestionOverlay(
                    suggestion = suggestion,
                    onAddToBox = { boxUuid ->
                        viewModel.confirmAddToExistingBox(suggestion, boxUuid)
                    },
                    onLoose = {
                        viewModel.confirmLooseEntry(suggestion)
                    },
                    onNewBox = {
                        viewModel.confirmNewIncompleteBox(suggestion)
                    },
                    onDismiss = { viewModel.resetUiState() }
                )
            }

            if (uiState is InventoryUiState.Error) {
                LaunchedEffect(uiState) {
                    kotlinx.coroutines.delay(4000)
                    viewModel.resetUiState()
                }
                val state = uiState as InventoryUiState.Error
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        action = {
                            TextButton(onClick = { viewModel.resetUiState() }) {
                                Text("OK", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    ) {
                        Text(state.message)
                    }
                }
            }
        }
    }
}
