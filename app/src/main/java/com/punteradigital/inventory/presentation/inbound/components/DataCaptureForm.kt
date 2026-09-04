package com.punteradigital.inventory.presentation.inbound.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.RemainderMode
import com.punteradigital.inventory.ui.theme.DispatchGreen
import com.punteradigital.inventory.ui.theme.RefillBlue
import com.punteradigital.inventory.ui.theme.WarningOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCaptureForm(
    isLandscape: Boolean,
    selectedEntryType: String,
    onEntryTypeChange: (String) -> Unit,
    entryTypes: List<String>,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    models: List<String>,
    selectedSize: String,
    onSizeChange: (String) -> Unit,
    sizes: List<String>,
    lot: String,
    onLotChange: (String) -> Unit,
    selectedRack: String,
    onRackChange: (String) -> Unit,
    rackLocations: List<String>,
    totalQuantity: String,
    onTotalQuantityChange: (String) -> Unit,
    isMasterBox: Boolean,
    onMasterBoxChange: (Boolean) -> Unit,
    childCount: String,
    onChildCountChange: (String) -> Unit,
    remainderMode: RemainderMode,
    onRemainderModeChange: (RemainderMode) -> Unit
) {
    var entryTypeExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }
    var rackExpanded by remember { mutableStateOf(false) }

    val totalQty = totalQuantity.toIntOrNull() ?: 0
    val pairsPerBox = childCount.toIntOrNull() ?: BusinessRules.DEFAULT_MASTER_QTY
    val autoBox = if (isMasterBox && totalQty > 0 && pairsPerBox > 0)
        BusinessRules.calculateAutoBoxing(totalQty, pairsPerBox) else null

    KineticCard(padding = if (isLandscape) 16.dp else 20.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(if (isLandscape) 10.dp else 16.dp)) {
            Text(
                "Datos del Producto",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = entryTypeExpanded,
                            onExpandedChange = { entryTypeExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KineticTextField(
                                value = selectedEntryType,
                                onValueChange = {},
                                readOnly = true,
                                label = "Tipo de Entrada",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entryTypeExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = entryTypeExpanded, onDismissRequest = { entryTypeExpanded = false }) {
                                entryTypes.forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { onEntryTypeChange(item); entryTypeExpanded = false })
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = modelExpanded,
                            onExpandedChange = { modelExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KineticTextField(
                                value = selectedModel,
                                onValueChange = {},
                                readOnly = true,
                                label = "Modelo",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                                models.forEach { item ->
                                    DropdownMenuItem(text = { Text(item) }, onClick = { onModelChange(item); modelExpanded = false })
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = sizeExpanded,
                                onExpandedChange = { sizeExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                KineticTextField(
                                    value = selectedSize,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = "Talla",
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                                    sizes.forEach { item ->
                                        DropdownMenuItem(text = { Text(item) }, onClick = { onSizeChange(item); sizeExpanded = false })
                                    }
                                }
                            }
                            KineticTextField(
                                value = lot,
                                onValueChange = onLotChange,
                                label = "Lote",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        ExposedDropdownMenuBox(
                            expanded = rackExpanded,
                            onExpandedChange = { rackExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KineticTextField(
                                value = "📍 Rack: $selectedRack",
                                onValueChange = {},
                                readOnly = true,
                                label = "Ubicación en Rack",
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rackExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = rackExpanded, onDismissRequest = { rackExpanded = false }) {
                                rackLocations.forEach { rack ->
                                    DropdownMenuItem(
                                        text = { Text(if (rack == "PISO") "📦 PISO (Sin rack)" else "📍 $rack") },
                                        onClick = { onRackChange(rack); rackExpanded = false }
                                    )
                                }
                            }
                        }

                        KineticTextField(
                            value = totalQuantity,
                            onValueChange = onTotalQuantityChange,
                            label = "🔢 Cantidad Total *",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = entryTypeExpanded,
                    onExpandedChange = { entryTypeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KineticTextField(
                        value = selectedEntryType,
                        onValueChange = {},
                        readOnly = true,
                        label = "Tipo de Entrada",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entryTypeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = entryTypeExpanded, onDismissRequest = { entryTypeExpanded = false }) {
                        entryTypes.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { onEntryTypeChange(item); entryTypeExpanded = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KineticTextField(
                        value = selectedModel,
                        onValueChange = {},
                        readOnly = true,
                        label = "Modelo",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                        models.forEach { item ->
                            DropdownMenuItem(text = { Text(item) }, onClick = { onModelChange(item); modelExpanded = false })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = sizeExpanded,
                        onExpandedChange = { sizeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        KineticTextField(
                            value = selectedSize,
                            onValueChange = {},
                            readOnly = true,
                            label = "Talla",
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                            sizes.forEach { item ->
                                DropdownMenuItem(text = { Text(item) }, onClick = { onSizeChange(item); sizeExpanded = false })
                            }
                        }
                    }
                    KineticTextField(
                        value = lot,
                        onValueChange = onLotChange,
                        label = "Lote",
                        modifier = Modifier.weight(1f)
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = rackExpanded,
                    onExpandedChange = { rackExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KineticTextField(
                        value = "📍 Rack: $selectedRack",
                        onValueChange = {},
                        readOnly = true,
                        label = "Ubicación en Rack",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rackExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = rackExpanded, onDismissRequest = { rackExpanded = false }) {
                        rackLocations.forEach { rack ->
                            DropdownMenuItem(
                                text = { Text(if (rack == "PISO") "📦 PISO (Sin rack)" else "📍 $rack") },
                                onClick = { onRackChange(rack); rackExpanded = false }
                            )
                        }
                    }
                }

                KineticTextField(
                    value = totalQuantity,
                    onValueChange = onTotalQuantityChange,
                    label = "🔢 Cantidad Total a Ingresar *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    KineticCard(padding = 20.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Caja Master", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (isMasterBox) {
                            if (autoBox != null)
                                "${autoBox.fullBoxes} caja${if (autoBox.fullBoxes != 1) "s" else ""} × $pairsPerBox pares"
                            else "1 UUID Padre + $childCount Hijos"
                        } else "Unidades individuales",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isMasterBox,
                    onCheckedChange = onMasterBoxChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }

            AnimatedVisibility(visible = isMasterBox) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    KineticTextField(
                        value = childCount,
                        onValueChange = onChildCountChange,
                        label = "Pares por Caja",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }

            if (isMasterBox && autoBox != null) {
                KineticCard(padding = 16.dp) {
                    Column {
                        Text(
                            "📦 Resumen de Ingreso",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Su ingreso resultará en:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "✅ ${autoBox.fullBoxes} Caja${if (autoBox.fullBoxes != 1) "s" else ""} Master completa${if (autoBox.fullBoxes != 1) "s" else ""} (${autoBox.pairsInFullBoxes} pares)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = DispatchGreen
                        )

                        if (autoBox.hasRemainder) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "+ ${autoBox.remainderPairs} par${if (autoBox.remainderPairs > 1) "es" else ""} adicional${if (autoBox.remainderPairs > 1) "es" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarningOrange
                            )

                            Spacer(Modifier.height(12.dp))
                            Text(
                                "¿Qué hacer con los pares sobrantes?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = remainderMode == RemainderMode.LOOSE,
                                    onClick = { onRemainderModeChange(RemainderMode.LOOSE) },
                                    label = { Text("Pares sueltos", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        if (remainderMode == RemainderMode.LOOSE)
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                FilterChip(
                                    selected = remainderMode == RemainderMode.FILL_LATER,
                                    onClick = { onRemainderModeChange(RemainderMode.FILL_LATER) },
                                    label = { Text("Caja incompleta", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        if (remainderMode == RemainderMode.FILL_LATER)
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RefillBlue,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }

                            Text(
                                when (remainderMode) {
                                    RemainderMode.LOOSE -> "Se generarán etiquetas individuales sin caja padre."
                                    RemainderMode.FILL_LATER -> "Se creará una caja marcada como 'Pendiente por Rellenar' (${autoBox.remainderPairs}/$pairsPerBox)."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            if (!isMasterBox && totalQty > 0) {
                KineticCard(padding = 16.dp) {
                    Column {
                        Text(
                            "📦 Resumen de Ingreso",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Su ingreso resultará en:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "✅ $totalQty unidades individuales",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = DispatchGreen
                        )
                        Text(
                            "Se generarán las etiquetas individuales, sin caja padre asignada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
