package com.punteradigital.inventory.presentation.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.ui.theme.*

private val availableModels = com.punteradigital.inventory.domain.model.Catalog.models
private val availableSizes = com.punteradigital.inventory.domain.model.Catalog.sizes

private data class CreateLine(
    val modelIndex: Int = 0,
    val sizeIndex: Int = availableSizes.indexOf("42").coerceAtLeast(0),  // default to 42
    val qty: String = "1"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoCreateScreen(
    onBack: () -> Unit,
    currentUserName: String = "Carlos Martínez"
) {
    var pedidoNum by remember { mutableStateOf("#${4521 + PedidoRepository.pedidos.size}") }
    var client by remember { mutableStateOf("") }
    var obs by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf(CreateLine()) }
    var showCreated by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crear Pedido", fontWeight = FontWeight.Bold)
                        Text(
                            "Nuevo pedido de venta",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Order data
            item {
                KineticCard(padding = 20.dp) {
                    Text(
                        "Datos del Pedido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    KineticTextField(
                        value = pedidoNum,
                        onValueChange = { pedidoNum = it },
                        label = "Nro. Pedido *",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KineticTextField(
                        value = client,
                        onValueChange = { client = it },
                        label = "Cliente *",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KineticTextField(
                        value = obs,
                        onValueChange = { obs = it },
                        label = "Observaciones",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                }
            }

            // Lines
            item {
                KineticCard(padding = 16.dp) {
                    Text(
                        "Líneas del Pedido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    lines.forEachIndexed { index, line ->
                        LineBuilderRow(
                            line = line,
                            onModelChange = { lines[index] = lines[index].copy(modelIndex = it) },
                            onSizeChange = { lines[index] = lines[index].copy(sizeIndex = it) },
                            onQtyChange = { lines[index] = lines[index].copy(qty = it) },
                            onRemove = if (lines.size > 1) {{ lines.removeAt(index) }} else null
                        )
                        if (index < lines.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { lines.add(CreateLine()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("➕ Agregar Línea", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Preview
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📋 Resumen del Pedido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        lines.forEach { line ->
                            val model = availableModels[line.modelIndex]
                            val size = availableSizes[line.sizeIndex]
                            val qty = line.qty.toIntOrNull() ?: 0
                            Text(
                                "• $model T$size — $qty pares",
                                fontSize = 13.sp,
                                color = DispatchGreen,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val totalPairs = lines.sumOf { it.qty.toIntOrNull() ?: 0 }
                        Text(
                            "Total: $totalPairs pares",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Create button
            item {
                KineticButton(
                    text = if (showCreated) "✅ ¡PEDIDO CREADO!" else "✅ CREAR PEDIDO",
                    onClick = {
                        if (client.isNotBlank()) {
                            val newPedido = Pedido(
                                id = pedidoNum,
                                client = client,
                                status = "pendiente",
                                creator = currentUserName,
                                date = "06/05/2026",
                                obs = obs,
                                lines = lines.map { line ->
                                    PedidoLine(
                                        model = availableModels[line.modelIndex],
                                        size = availableSizes[line.sizeIndex],
                                        qty = line.qty.toIntOrNull() ?: 1
                                    )
                                }
                            )
                            PedidoRepository.pedidos.add(0, newPedido)
                            showCreated = true
                            onBack()
                        }
                    },
                    type = if (showCreated) ButtonType.SECONDARY else ButtonType.PRIMARY
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineBuilderRow(
    line: CreateLine,
    onModelChange: (Int) -> Unit,
    onSizeChange: (Int) -> Unit,
    onQtyChange: (String) -> Unit,
    onRemove: (() -> Unit)?
) {
    var modelExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Model dropdown
        ExposedDropdownMenuBox(
            expanded = modelExpanded,
            onExpandedChange = { modelExpanded = it },
            modifier = Modifier.weight(1.4f)
        ) {
            OutlinedTextField(
                value = availableModels[line.modelIndex],
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                availableModels.forEachIndexed { idx, model ->
                    DropdownMenuItem(
                        text = { Text(model, fontSize = 12.sp) },
                        onClick = { onModelChange(idx); modelExpanded = false }
                    )
                }
            }
        }

        // Size dropdown
        ExposedDropdownMenuBox(
            expanded = sizeExpanded,
            onExpandedChange = { sizeExpanded = it },
            modifier = Modifier.weight(0.6f)
        ) {
            OutlinedTextField(
                value = "T${availableSizes[line.sizeIndex]}",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                availableSizes.forEachIndexed { idx, size ->
                    DropdownMenuItem(
                        text = { Text("T$size", fontSize = 12.sp) },
                        onClick = { onSizeChange(idx); sizeExpanded = false }
                    )
                }
            }
        }

        // Qty
        OutlinedTextField(
            value = line.qty,
            onValueChange = onQtyChange,
            modifier = Modifier.weight(0.4f),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Remove
        if (onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, "Eliminar", tint = CriticalRed, modifier = Modifier.size(18.dp))
            }
        }
    }
}
