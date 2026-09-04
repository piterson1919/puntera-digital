package com.punteradigital.inventory.presentation.scanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.domain.model.BajaReason
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.ScannedInfo
import com.punteradigital.inventory.ui.theme.DispatchGreen
import com.punteradigital.inventory.ui.theme.MuestraTeal
import com.punteradigital.inventory.ui.theme.QualityPurple
import com.punteradigital.inventory.ui.theme.StandByAmber

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
        "VERIFY" -> "Detalles del UUID"
        "INBOUND_EMPAQUE" -> "Ingresar al Rack"
        "MUESTRA_LOOKUP" -> "Registrar Muestra"
        else -> "Detalles"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        if (scannedInfo != null) {
            val (model, size, status, typeDesc, originStr) = when (scannedInfo) {
                is ScannedInfo.Master -> listOf(scannedInfo.entity.model, scannedInfo.entity.size, scannedInfo.entity.status, "Caja Master", scannedInfo.entity.origin)
                is ScannedInfo.UnitInfo -> listOf(scannedInfo.entity.model, scannedInfo.entity.size, scannedInfo.entity.status, "Unidad Individual", scannedInfo.entity.origin)
                is ScannedInfo.Label -> listOf(scannedInfo.entity.model, scannedInfo.entity.size, scannedInfo.entity.status, if (scannedInfo.entity.labelType == "MASTER_BOX") "Etiqueta Master (Pre-registro)" else "Etiqueta Individual (Pre-registro)", scannedInfo.entity.origin)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("UUID: $qrCode", fontWeight = FontWeight.Bold)
                    Text("Tipo: $typeDesc", color = MaterialTheme.colorScheme.secondary)
                    Text("Origen: $originStr", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Modelo: $model")
                        Text("Talla: $size")
                    }
                    Text("Estado actual: $status", fontWeight = FontWeight.Medium)
                }
            }

            if ((scannedInfo is ScannedInfo.Label && scannedInfo.entity.labelType == "MASTER_BOX") ||
                scannedInfo is ScannedInfo.Master
            ) {
                Text("Contenido de la Caja (${checkedUuids.size} unidades)", fontWeight = FontWeight.Bold)
                
                val itemsList = if (scannedInfo is ScannedInfo.Label) childLabelsState.value else childProductsState.value
                
                if (itemsList.isEmpty()) {
                    Text("Buscando contenido...", style = MaterialTheme.typography.bodySmall)
                } else {
                    Box(modifier = Modifier.heightIn(max = 200.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(itemsList) { item ->
                                val itemUuid = when(item) {
                                    is com.punteradigital.inventory.data.local.entity.LabelEntity -> item.uuid
                                    is com.punteradigital.inventory.data.local.entity.ProductEntity -> item.uuid
                                    else -> ""
                                }
                                val itemStatus = when(item) {
                                    is com.punteradigital.inventory.data.local.entity.LabelEntity -> item.status
                                    is com.punteradigital.inventory.data.local.entity.ProductEntity -> item.status
                                    else -> ""
                                }
                                
                                val isChecked = checkedUuids.contains(itemUuid)
                                val canUncheck = moduleName == "INBOUND_EMPAQUE"
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(itemUuid, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text(itemStatus, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (canUncheck) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) checkedUuids.add(itemUuid) else checkedUuids.remove(itemUuid)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Text("UUID no encontrado en la base de datos.", color = MaterialTheme.colorScheme.error)
        }

        if (moduleName == "QUALITY") {
            ExposedDropdownMenuBox(
                expanded = reasonExpanded,
                onExpandedChange = { reasonExpanded = it },
                modifier = Modifier.fillMaxWidth()
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
        } else if (moduleName == "INBOUND_EMPAQUE") {
            val rackLocations = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "PISO")
            ExposedDropdownMenuBox(
                expanded = reasonExpanded,
                onExpandedChange = { reasonExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                KineticTextField(
                    value = if (selectedReason.isEmpty()) "Seleccionar Rack..." else "📍 $selectedReason",
                    onValueChange = {},
                    readOnly = true,
                    label = "Rack de Destino *",
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = reasonExpanded, onDismissRequest = { reasonExpanded = false }) {
                    rackLocations.forEach { rack ->
                        DropdownMenuItem(
                            text = { Text(if (rack == "PISO") "📦 PISO (Sin rack)" else "📍 $rack") },
                            onClick = { selectedReason = rack; reasonExpanded = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val checklistResult = if (moduleName == "INBOUND_EMPAQUE" && 
                                  ((scannedInfo is ScannedInfo.Label && scannedInfo.entity.labelType == "MASTER_BOX") ||
                                   scannedInfo is ScannedInfo.Master)) {
            checkedUuids.toList()
        } else null

        KineticButton(
            text = when (moduleName) {
                "STANDBY" -> "MOVER A STAND-BY"
                "QUALITY" -> "REGISTRAR BAJA"
                "INBOUND_EMPAQUE" -> "INGRESAR AL RACK"
                "MUESTRA_LOOKUP" -> "CONTINUAR"
                "VERIFY" -> "CERRAR"
                else -> "CONFIRMAR"
            },
            onClick = {
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
