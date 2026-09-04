package com.punteradigital.inventory.presentation.refill

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.punteradigital.inventory.data.local.entity.MasterBoxEntity
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import kotlinx.coroutines.launch
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.ui.theme.*

/**
 * Refill Master Box screen.
 * Step 1: Scan/enter master box UUID → shows capacity info
 * Step 2: Scan/enter individual pairs → validates model/size compatibility
 * Step 3: Confirm refill
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillMasterBoxScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    var parentUuid by remember { mutableStateOf("") }
    var masterBoxInfo by remember { mutableStateOf<MasterBoxEntity?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    val childUuids = remember { mutableStateListOf<String>() }
    var newChildUuid by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1 = scan parent, 2 = add children

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rellenar Caja Master", fontWeight = FontWeight.Bold)
                        Text(
                            if (step == 1) "Paso 1: Escanear Caja" else "Paso 2: Agregar Pares",
                            style = MaterialTheme.typography.bodySmall,
                            color = RefillBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == 2 && childUuids.isEmpty()) {
                            step = 1
                            masterBoxInfo = null
                            parentUuid = ""
                        } else {
                            onBack()
                        }
                    }) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ═══ STEP 1: Find master box ═══
                AnimatedVisibility(visible = step == 1) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        KineticCard(
                            padding = 20.dp
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = RefillBlue.copy(alpha = 0.15f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Inventory, null, tint = RefillBlue, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Buscar Caja Master",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                KineticTextField(
                                    value = parentUuid,
                                    onValueChange = { parentUuid = it.uppercase() },
                                    label = "UUID de Caja Master",
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                KineticButton(
                                    text = if (isSearching) "BUSCANDO..." else "BUSCAR CAJA",
                                    onClick = {
                                        isSearching = true
                                        scope.launch {
                                            val info = viewModel.getScannedInfo(parentUuid)
                                            isSearching = false
                                            if (info is com.punteradigital.inventory.presentation.viewmodel.ScannedInfo.Master) {
                                                masterBoxInfo = info.entity
                                                step = 2
                                            } else {
                                                viewModel.resetUiState()
                                                masterBoxInfo = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = parentUuid.isNotBlank() && !isSearching,
                                    type = ButtonType.PRIMARY,
                                    icon = if (!isSearching) { { Icon(Icons.Default.Search, null, tint = KineticOnPrimary, modifier = Modifier.size(24.dp)) } } else null
                                )
                            }
                        }

                        // Show info if no box found
                        if (masterBoxInfo == null && !isSearching && parentUuid.isNotBlank()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CriticalRed.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, null, tint = CriticalRed)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Caja Master no encontrada. Verifique el UUID.",
                                        color = CriticalRed,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // ═══ STEP 2: Master box info + add children ═══
                AnimatedVisibility(visible = step == 2 && masterBoxInfo != null) {
                    val box = masterBoxInfo ?: return@AnimatedVisibility
                    val remaining = box.childCount - box.activeChildCount

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Master box info card
                        KineticCard(
                            padding = 20.dp
                        ) {
                            Column {
                                Text("📦 Caja Master", style = MaterialTheme.typography.titleSmall, color = RefillBlue)
                                Spacer(Modifier.height(8.dp))
                                Text(box.uuid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("${box.model} · Talla ${box.size} · Lote ${box.lot}", style = MaterialTheme.typography.bodySmall)

                                Spacer(Modifier.height(12.dp))

                                // Capacity indicator
                                LinearProgressIndicator(
                                    progress = { (box.activeChildCount + childUuids.size).toFloat() / box.childCount },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = if (remaining - childUuids.size <= 0) DispatchGreen else RefillBlue,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tiene ${box.activeChildCount} de ${box.childCount} pares. Faltan ${remaining}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining > 0) WarningOrange else DispatchGreen
                                )
                                if (childUuids.isNotEmpty()) {
                                    Text(
                                        "Pares a agregar: ${childUuids.size} → Total: ${box.activeChildCount + childUuids.size}/${box.childCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RefillBlue
                                    )
                                }
                            }
                        }

                        // Add child input
                        if (childUuids.size < remaining) {
                            KineticCard(
                                padding = 16.dp
                            ) {
                                Column {
                                    Text(
                                        "Agregar Par Individual",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        KineticTextField(
                                            value = newChildUuid,
                                            onValueChange = { newChildUuid = it.uppercase() },
                                            label = "UUID del Par",
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        FilledIconButton(
                                            onClick = {
                                                if (newChildUuid.isNotBlank() && newChildUuid !in childUuids) {
                                                    childUuids.add(newChildUuid)
                                                    newChildUuid = ""
                                                }
                                            },
                                            enabled = newChildUuid.isNotBlank(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = RefillBlue
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // List of added children
                        if (childUuids.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(childUuids) { uuid ->
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(uuid, style = MaterialTheme.typography.bodyMedium)
                                            IconButton(
                                                onClick = { childUuids.remove(uuid) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, null, tint = CriticalRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            // Confirm button
                            KineticButton(
                                text = "CONFIRMAR RELLENADO (${childUuids.size} par${if (childUuids.size > 1) "es" else ""})",
                                onClick = {
                                    viewModel.refillMasterBox(
                                        parentUuid = box.uuid,
                                        childUuids = childUuids.toList(),
                                        userId = user?.id ?: "UNKNOWN"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                type = ButtonType.PRIMARY,
                                icon = { Icon(Icons.Default.CheckCircle, null, tint = KineticOnPrimary, modifier = Modifier.size(24.dp)) }
                            )
                        }
                    }
                }
            }

            // Success overlay for refill
            if (uiState is InventoryUiState.SuccessRefill) {
                val state = uiState as InventoryUiState.SuccessRefill
                AlertDialog(
                    onDismissRequest = {
                        viewModel.resetUiState()
                        step = 1
                        masterBoxInfo = null
                        parentUuid = ""
                        childUuids.clear()
                    },
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = DispatchGreen, modifier = Modifier.size(48.dp)) },
                    title = { Text("¡Caja Rellenada!", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.parentUuid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Agregados: ${state.addedCount} par(es)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Total ahora: ${state.totalActive}/${state.totalCapacity}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (state.totalActive == state.totalCapacity) DispatchGreen else RefillBlue
                            )
                            if (state.totalActive == state.totalCapacity) {
                                Spacer(Modifier.height(8.dp))
                                Text("✅ Caja completa", color = DispatchGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetUiState()
                                step = 1
                                masterBoxInfo = null
                                parentUuid = ""
                                childUuids.clear()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DispatchGreen)
                        ) { Text("CERRAR") }
                    }
                )
            }

            // Error snackbar
            if (uiState is InventoryUiState.Error) {
                androidx.compose.runtime.LaunchedEffect(uiState) {
                    kotlinx.coroutines.delay(4000)
                    viewModel.resetUiState()
                }
                val state = uiState as InventoryUiState.Error
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = CriticalRed,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.resetUiState() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) { Text(state.message) }
            }
        }
    }
}
