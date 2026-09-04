package com.punteradigital.inventory.presentation.dispatch

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.ui.theme.*

/**
 * Dispatch screen that lists all items in Stand-By (ZONA_PREDESPACHO)
 * with checkboxes for selection. No re-scanning needed.
 * Includes an optional "panic button" scanner for UUID verification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchListScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onVerifyScan: () -> Unit
) {
    val standByItems by viewModel.standByItems.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    val selectedUuids = remember { mutableStateListOf<String>() }
    var showClienteModal by remember { mutableStateOf(false) }
    var cliente by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    // Group items by model+lot for visual clarity
    val groupedItems = standByItems.groupBy { "${it.model} · Lote: ${it.lot}" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Despacho", fontWeight = FontWeight.Bold)
                        Text(
                            "${standByItems.size} items en Pre-despacho",
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
                actions = {
                    // "Panic button" – verification scanner
                    IconButton(onClick = onVerifyScan) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Verificar UUID",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (standByItems.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Sin productos en Pre-despacho",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Los productos deben pasar por Stand-By primero",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Select all / Deselect buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedUuids.size} de ${standByItems.size} seleccionados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                selectedUuids.clear()
                                selectedUuids.addAll(standByItems.map { it.uuid })
                            }) {
                                Text("Seleccionar Todo")
                            }
                            TextButton(onClick = { selectedUuids.clear() }) {
                                Text("Deseleccionar")
                            }
                        }
                    }

                    // Item list grouped by model
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedItems.forEach { (groupTitle, items) ->
                            item {
                                Text(
                                    groupTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(items, key = { it.uuid }) { product ->
                                DispatchItemCard(
                                    product = product,
                                    isSelected = product.uuid in selectedUuids,
                                    onToggle = {
                                        if (product.uuid in selectedUuids) {
                                            selectedUuids.remove(product.uuid)
                                        } else {
                                            selectedUuids.add(product.uuid)
                                        }
                                    }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }

                    // Confirm button
                    AnimatedVisibility(
                        visible = selectedUuids.isNotEmpty(),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            KineticButton(
                                text = "CONFIRMAR SALIDA (${selectedUuids.size})",
                                onClick = { showClienteModal = true },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                type = ButtonType.PRIMARY,
                                icon = { Icon(Icons.Default.LocalShipping, null, tint = KineticOnPrimary, modifier = Modifier.size(24.dp)) }
                            )
                        }
                    }
                }
            }

            // Cliente/Observaciones modal
            if (showClienteModal) {
                AlertDialog(
                    onDismissRequest = { showClienteModal = false },
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            null,
                            tint = DispatchGreen,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    title = {
                        Text(
                            "Datos de Salida",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Complete los datos antes de confirmar el despacho de ${selectedUuids.size} unidades.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            KineticTextField(
                                value = cliente,
                                onValueChange = { cliente = it },
                                label = "Cliente *",
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            KineticTextField(
                                value = observaciones,
                                onValueChange = { observaciones = it },
                                label = "Observaciones",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        KineticButton(
                            text = "CONFIRMAR SALIDA DEFINITIVA",
                            onClick = {
                                viewModel.confirmDispatchFromList(
                                    selectedUuids = selectedUuids.toList(),
                                    cliente = cliente,
                                    observaciones = observaciones,
                                    userId = user?.id ?: "UNKNOWN"
                                )
                                showClienteModal = false
                                selectedUuids.clear()
                                cliente = ""
                                observaciones = ""
                            },
                            enabled = cliente.isNotBlank(),
                            type = ButtonType.PRIMARY
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showClienteModal = false }) {
                            Text("CANCELAR")
                        }
                    }
                )
            }

            // Success / Error overlays
            if (uiState is InventoryUiState.SuccessMovement) {
                LaunchedEffect(uiState) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetUiState()
                }
                val state = uiState as InventoryUiState.SuccessMovement
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = DispatchGreen,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.resetUiState() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) {
                    Text(state.message)
                }
            }

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
        }
    }
}

@Composable
fun DispatchItemCard(
    product: ProductEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    KineticCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        padding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = DispatchGreen,
                    checkmarkColor = Color.White
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.uuid,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Talla: ${product.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.parentUuid != null) {
                        Text(
                            "📦 ${product.parentUuid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (product.origin == "FOOT_SAFE") FootSafeYellow.copy(alpha = 0.2f) else SafetyCobalt.copy(alpha = 0.2f)
            ) {
                Text(
                    if (product.origin == "FOOT_SAFE") "FS" else "SF",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (product.origin == "FOOT_SAFE") FootSafeYellow else SafetyCobalt
                )
            }
        }
    }
}
