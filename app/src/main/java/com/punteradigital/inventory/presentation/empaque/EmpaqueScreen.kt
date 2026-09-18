package com.punteradigital.inventory.presentation.empaque

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.ui.theme.DispatchGreen
import com.punteradigital.inventory.ui.theme.StandByAmber
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpaqueScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToQRHistory: () -> Unit,
    onNavigateToScanner: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Generar", "Vista Previa", "Mis Lotes")
    
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingBatches by viewModel.pendingLabelBatches.collectAsState()

    // We store the last generated batch ID to show it in the Preview tab
    var lastGeneratedBatchId by remember { mutableStateOf<String?>(null) }
    val labelsOfLastBatch by produceState(initialValue = emptyList<com.punteradigital.inventory.data.local.entity.LabelEntity>(), lastGeneratedBatchId) {
        if (lastGeneratedBatchId != null) {
            viewModel.dao.getLabelsByBatch(lastGeneratedBatchId!!).collect { value = it }
        } else {
            value = emptyList()
        }
    }

    // Handle state resets and navigation on success
    LaunchedEffect(uiState) {
        if (uiState is InventoryUiState.SuccessMovement) {
            val msg = (uiState as InventoryUiState.SuccessMovement).message
            if (msg.contains("Se generaron")) {
                // Extract batchId roughly or just jump to preview
                val batchIdStr = msg.substringAfter("Lote: ").substringBefore(")")
                lastGeneratedBatchId = batchIdStr
                selectedTab = 1 // Go to preview
                viewModel.resetUiState()
            }
        }
    }

    val isEmpaqueRole = currentUser?.role == "OPERADOR_EMPAQUE" || currentUser?.role == "SUPERVISOR_EMPAQUE"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Empaque (Pre-Almacén)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (!isEmpaqueRole) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    if (isEmpaqueRole) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión")
                        }
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
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> GenerateForm(
                        viewModel = viewModel,
                        userId = currentUser?.id ?: "unknown",
                        isEmpaqueRole = isEmpaqueRole,
                        onNavigateToPrinter = onNavigateToPrinter,
                        onNavigateToQRHistory = onNavigateToQRHistory,
                        onNavigateToScanner = onNavigateToScanner
                    )
                    1 -> PreviewTab(labelsOfLastBatch, viewModel)
                    2 -> HistoryTab(pendingBatches, viewModel)
                }

                if (uiState is InventoryUiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                if (uiState is InventoryUiState.Error) {
                    Snackbar(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text((uiState as InventoryUiState.Error).message)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerateForm(
    viewModel: InventoryViewModel,
    userId: String,
    isEmpaqueRole: Boolean,
    onNavigateToPrinter: () -> Unit,
    onNavigateToQRHistory: () -> Unit,
    onNavigateToScanner: (String, String) -> Unit
) {
    var selectedOrigin by remember { mutableStateOf(Origin.fromString(viewModel.empaquePreferences.lastOrigin) ?: Origin.FOOT_SAFE) }
    val catalogModels by viewModel.catalogModels.collectAsState(initial = emptyList())
    val models = catalogModels.filter { it.isActive }.map { it.code }
    var selectedModel by remember { mutableStateOf(viewModel.empaquePreferences.lastModel) }
    var modelExpanded by remember { mutableStateOf(false) }

    val sizes = com.punteradigital.inventory.domain.model.Catalog.sizes
    var selectedSize by remember { mutableStateOf("") }
    var sizeExpanded by remember { mutableStateOf(false) }

    var lot by remember { mutableStateOf(viewModel.empaquePreferences.lastLot) }

    LaunchedEffect(models) {
        if (selectedModel.isNotBlank() && selectedModel !in models) {
            selectedModel = ""
        }
    }
    
    val formats = listOf("Estándar", "Pequeña", "QR Grande")
    var selectedFormat by remember { mutableStateOf(viewModel.empaquePreferences.lastFormat.ifBlank { "Estándar" }) }
    var formatExpanded by remember { mutableStateOf(false) }

    var isMasterBox by remember { mutableStateOf(viewModel.empaquePreferences.lastIsMaster) }
    var childCount by remember { mutableStateOf(viewModel.empaquePreferences.lastChildCount.toString()) }
    var totalQuantity by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        KineticCard(
            padding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Herramientas de Operador",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToPrinter,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Impresora", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onNavigateToQRHistory,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Historial", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onNavigateToScanner("VALIDATE_LABEL", "MANUAL") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Validar QR", fontSize = 11.sp)
                    }
                }
            }
        }

        // Origin Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedOrigin == Origin.FOOT_SAFE,
                onClick = {
                    selectedOrigin = Origin.FOOT_SAFE
                    viewModel.empaquePreferences.lastOrigin = Origin.FOOT_SAFE.name
                },
                label = { Text("Foot Safe") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedOrigin == Origin.SAFETY,
                onClick = {
                    selectedOrigin = Origin.SAFETY
                    viewModel.empaquePreferences.lastOrigin = Origin.SAFETY.name
                },
                label = { Text("Safety") },
                modifier = Modifier.weight(1f)
            )
        }

        // Model Dropdown
        ExposedDropdownMenuBox(
            expanded = modelExpanded,
            onExpandedChange = { modelExpanded = !modelExpanded }
        ) {
            OutlinedTextField(
                value = selectedModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Modelo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = modelExpanded,
                onDismissRequest = { modelExpanded = false }
            ) {
                models.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            selectedModel = model
                            viewModel.empaquePreferences.lastModel = model
                            modelExpanded = false
                        }
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Size Dropdown
            ExposedDropdownMenuBox(
                expanded = sizeExpanded,
                onExpandedChange = { sizeExpanded = !sizeExpanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedSize,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Talla") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = sizeExpanded,
                    onDismissRequest = { sizeExpanded = false }
                ) {
                    sizes.forEach { size ->
                        DropdownMenuItem(
                            text = { Text(size) },
                            onClick = {
                                selectedSize = size
                                sizeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = lot,
                onValueChange = {
                    lot = it.uppercase()
                    viewModel.empaquePreferences.lastLot = it.uppercase()
                },
                label = { Text("Lote") },
                modifier = Modifier.weight(1f)
            )
        }

        // Format Dropdown
        ExposedDropdownMenuBox(
            expanded = formatExpanded,
            onExpandedChange = { formatExpanded = !formatExpanded }
        ) {
            OutlinedTextField(
                value = selectedFormat,
                onValueChange = {},
                readOnly = true,
                label = { Text("Formato de Etiqueta") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = formatExpanded,
                onDismissRequest = { formatExpanded = false }
            ) {
                formats.forEach { f ->
                    DropdownMenuItem(
                        text = { Text(f) },
                        onClick = {
                            selectedFormat = f
                            viewModel.empaquePreferences.lastFormat = f
                            formatExpanded = false
                        }
                    )
                }
            }
        }

        // Packaging Mode
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tipo de Empaque", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isMasterBox,
                        onClick = {
                            isMasterBox = true
                            viewModel.empaquePreferences.lastIsMaster = true
                        },
                        label = { Text("Caja Master") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isMasterBox,
                        onClick = {
                            isMasterBox = false
                            viewModel.empaquePreferences.lastIsMaster = false
                        },
                        label = { Text("Pares Sueltos") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = totalQuantity,
                        onValueChange = { totalQuantity = it },
                        label = { Text("Pares en total") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    if (isMasterBox) {
                        OutlinedTextField(
                            value = childCount,
                            onValueChange = {
                                childCount = it
                                it.toIntOrNull()?.let { count ->
                                    viewModel.empaquePreferences.lastChildCount = count
                                }
                            },
                            label = { Text("Pares/Caja") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val isValid = selectedModel.isNotBlank() && selectedSize.isNotBlank() && lot.isNotBlank() && totalQuantity.toIntOrNull() ?: 0 > 0
        
        Button(
            onClick = {
                val qty = totalQuantity.toIntOrNull() ?: 0
                val pairsPerBox = childCount.toIntOrNull() ?: BusinessRules.DEFAULT_MASTER_QTY
                viewModel.generateLabels(
                    origin = selectedOrigin,
                    model = selectedModel,
                    size = selectedSize,
                    lot = lot,
                    labelType = if (isMasterBox) "MASTER_BOX" else "INDIVIDUAL",
                    labelFormat = selectedFormat,
                    isMasterBox = isMasterBox,
                    childCount = pairsPerBox,
                    totalQuantity = qty,
                    userId = userId
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = isValid
        ) {
            Icon(Icons.Default.Print, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("GENERAR E IMPRIMIR", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PreviewTab(
    labels: List<com.punteradigital.inventory.data.local.entity.LabelEntity>,
    viewModel: InventoryViewModel
) {
    if (labels.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay etiquetas generadas recientemente", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val batchId = labels.first().batchId
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Lote de Producción", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(batchId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Text("${labels.size} etiquetas generadas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { viewModel.reprintLabelBatch(batchId) }) {
                    Icon(Icons.Default.Print, "Reimprimir lote")
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(labels) { label ->
                KineticCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = label.uuid) {
                            value = withContext(Dispatchers.Default) {
                                generateQrBitmap(label.uuid, 120)
                            }
                        }
                        val bitmap = qrBitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(80.dp)
                            )
                        } else {
                            Box(modifier = Modifier.size(80.dp).background(Color.LightGray))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(label.model, fontWeight = FontWeight.Bold)
                            Text("Talla ${label.size} | ${label.lot}")
                            Text(
                                if (label.labelType == "MASTER_BOX") "CAJA MASTER" else "PAR INDIVIDUAL",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (label.labelType == "MASTER_BOX") StandByAmber else DispatchGreen
                            )
                            Text("Formato: ${label.labelFormat}", style = MaterialTheme.typography.bodySmall)
                            Text(label.uuid, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(
    batches: List<com.punteradigital.inventory.data.local.entity.LabelBatchSummary>,
    viewModel: InventoryViewModel
) {
    if (batches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay historial de etiquetas", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val df = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(batches) { batch ->
            KineticCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${batch.model} - Talla ${batch.size}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val isEntered = batch.enteredCount == batch.totalCount
                        val color = if (isEntered) DispatchGreen else StandByAmber
                        Text(
                            if (isEntered) "INGRESADO" else "PENDIENTE",
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text("Lote: ${batch.lot} | ${batch.totalCount} uds", style = MaterialTheme.typography.bodyMedium)
                    Text("Creado: ${df.format(Date(batch.createdAt))} por ${batch.createdBy}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Progress bars
                    LinearProgressIndicator(
                        progress = { batch.printedCount.toFloat() / batch.totalCount.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    Text("${batch.printedCount}/${batch.totalCount} Impresas", fontSize = 10.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    LinearProgressIndicator(
                        progress = { batch.enteredCount.toFloat() / batch.totalCount.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = DispatchGreen,
                        trackColor = DispatchGreen.copy(alpha = 0.2f)
                    )
                    Text("${batch.enteredCount}/${batch.totalCount} Ingresadas al Almacén", fontSize = 10.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.reprintLabelBatch(batch.batchId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reimprimir", fontSize = 12.sp)
                        }
                        
                        // Can only delete if NO items have been entered into the warehouse
                        if (batch.enteredCount == 0) {
                            OutlinedButton(
                                onClick = { viewModel.deleteLabelBatch(batch.batchId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Eliminar", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Helper to generate QR code bitmap via ZXing core */
private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val bitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bmp
    } catch (e: Exception) {
        null
    }
}
