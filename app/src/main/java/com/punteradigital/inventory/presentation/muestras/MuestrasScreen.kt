package com.punteradigital.inventory.presentation.muestras

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Muestras Retornables screen.
 * - Register a sample: UUID + Client + Observations
 * - List active samples with actions: "Return to Stock" or "Client kept"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuestrasScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onNavigateToScanner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val muestras by viewModel.muestrasActivas.collectAsState(initial = emptyList())

    var showNewMuestraDialog by remember { mutableStateOf(false) }
    var newUuid by remember { mutableStateOf("") }
    var newCliente by remember { mutableStateOf("") }
    var newObservaciones by remember { mutableStateOf("") }

    var confirmReturnUuid by remember { mutableStateOf<String?>(null) }
    var confirmSellUuid by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Muestras Retornables", fontWeight = FontWeight.Bold)
                        Text(
                            "${muestras.size} muestras activas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuestraTeal
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewMuestraDialog = true },
                containerColor = MuestraTeal,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nueva Muestra", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (muestras.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Sin muestras activas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Usa el botón + para registrar una muestra",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(muestras, key = { it.uuid }) { muestra ->
                        MuestraItemCard(
                            product = muestra,
                            onReturn = { confirmReturnUuid = muestra.uuid },
                            onSell = { confirmSellUuid = muestra.uuid }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }

            // New muestra dialog
            if (showNewMuestraDialog) {
                AlertDialog(
                    onDismissRequest = { showNewMuestraDialog = false },
                    icon = { Icon(Icons.Default.Storefront, null, tint = MuestraTeal, modifier = Modifier.size(40.dp)) },
                    title = { Text("Registrar Muestra", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // UUID with scanner button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                KineticTextField(
                                    value = newUuid,
                                    onValueChange = { newUuid = it.uppercase() },
                                    label = "UUID del Producto *",
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                FilledIconButton(
                                    onClick = {
                                        showNewMuestraDialog = false
                                        onNavigateToScanner()
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MuestraTeal,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, "Escanear QR", modifier = Modifier.size(24.dp))
                                }
                            }
                            Text(
                                "Escanea el QR del calzado o ingresa el UUID manualmente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            KineticTextField(
                                value = newCliente,
                                onValueChange = { newCliente = it },
                                label = "Cliente *",
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            KineticTextField(
                                value = newObservaciones,
                                onValueChange = { newObservaciones = it },
                                label = "Observaciones",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        KineticButton(
                            text = "REGISTRAR MUESTRA",
                            onClick = {
                                viewModel.processMuestra(
                                    uuid = newUuid,
                                    cliente = newCliente,
                                    observaciones = newObservaciones,
                                    userId = user?.id ?: "UNKNOWN"
                                )
                                showNewMuestraDialog = false
                                newUuid = ""
                                newCliente = ""
                                newObservaciones = ""
                            },
                            enabled = newUuid.isNotBlank() && newCliente.isNotBlank(),
                            type = ButtonType.PRIMARY
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewMuestraDialog = false }) {
                            Text("CANCELAR")
                        }
                    }
                )
            }

            // Confirm return dialog
            if (confirmReturnUuid != null) {
                AlertDialog(
                    onDismissRequest = { confirmReturnUuid = null },
                    icon = { Icon(Icons.AutoMirrored.Filled.Undo, null, tint = DispatchGreen) },
                    title = { Text("Retornar al Stock") },
                    text = { Text("¿Confirmar retorno de $confirmReturnUuid al stock disponible?") },
                    confirmButton = {
                        KineticButton(
                            text = "RETORNAR",
                            onClick = {
                                viewModel.returnMuestra(confirmReturnUuid!!, user?.id ?: "UNKNOWN")
                                confirmReturnUuid = null
                            },
                            type = ButtonType.PRIMARY
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmReturnUuid = null }) { Text("CANCELAR") }
                    }
                )
            }

            // Confirm sell dialog
            if (confirmSellUuid != null) {
                AlertDialog(
                    onDismissRequest = { confirmSellUuid = null },
                    icon = { Icon(Icons.Default.ShoppingCart, null, tint = WarningOrange) },
                    title = { Text("Cliente se quedó con el par") },
                    text = { Text("¿Confirmar que el cliente decidió quedarse con $confirmSellUuid? Esta acción no se puede deshacer.") },
                    confirmButton = {
                        KineticButton(
                            text = "CONFIRMAR VENTA",
                            onClick = {
                                viewModel.sellMuestra(confirmSellUuid!!, user?.id ?: "UNKNOWN")
                                confirmSellUuid = null
                            },
                            type = ButtonType.WARNING
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmSellUuid = null }) { Text("CANCELAR") }
                    }
                )
            }

            // Snackbar
            if (uiState is InventoryUiState.SuccessMovement) {
                androidx.compose.runtime.LaunchedEffect(uiState) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetUiState()
                }
                val state = uiState as InventoryUiState.SuccessMovement
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    containerColor = MuestraTeal,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { viewModel.resetUiState() }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) { Text(state.message) }
            }

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

@Composable
fun MuestraItemCard(
    product: ProductEntity,
    onReturn: () -> Unit,
    onSell: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = sdf.format(Date(product.updatedAt))

    KineticCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 16.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.uuid,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${product.model} · Talla ${product.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Desde: $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MuestraTeal.copy(alpha = 0.15f)
                ) {
                    Text(
                        "MUESTRA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MuestraTeal
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KineticButton(
                    text = "Retornar",
                    onClick = onReturn,
                    modifier = Modifier.weight(1f),
                    type = ButtonType.SECONDARY,
                    icon = { Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(18.dp), tint = KineticOnSurface) }
                )
                KineticButton(
                    text = "Se quedó",
                    onClick = onSell,
                    modifier = Modifier.weight(1f),
                    type = ButtonType.WARNING,
                    icon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp), tint = Color.White) }
                )
            }
        }
    }
}
