package com.punteradigital.inventory.presentation.scanner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.data.local.entity.LabelEntity
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.domain.model.BajaReason
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.ScannedInfo
import com.punteradigital.inventory.ui.theme.DispatchGreen
import com.punteradigital.inventory.ui.theme.FootSafeYellow
import com.punteradigital.inventory.ui.theme.SafetyCobalt
import com.punteradigital.inventory.ui.theme.WarningOrange
import com.punteradigital.inventory.presentation.components.kineticClick

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
    val childLabels by produceState<List<LabelEntity>>(emptyList(), scannedInfo) {
        if (scannedInfo is ScannedInfo.Label && scannedInfo.entity.labelType == "MASTER_BOX") {
            value = viewModel.dao.getChildrenLabels(scannedInfo.entity.uuid)
        }
    }
    val childProducts by produceState<List<ProductEntity>>(emptyList(), scannedInfo) {
        if (scannedInfo is ScannedInfo.Master) {
            value = viewModel.dao.getChildrenOfMasterBox(scannedInfo.entity.uuid)
        }
    }
    val checkedUuids = remember { mutableStateListOf<String>() }

    LaunchedEffect(childLabels, childProducts) {
        checkedUuids.clear()
        checkedUuids += childLabels.map(LabelEntity::uuid)
        checkedUuids += childProducts.map(ProductEntity::uuid)
    }

    val title = when (moduleName) {
        "STANDBY" -> "Confirmar Stand-By"
        "QUALITY" -> "Registrar Baja"
        "VERIFY" -> "Información del UUID"
        else -> "Confirmar Acción"
    }
    val childItems = when (scannedInfo) {
        is ScannedInfo.Label -> childLabels
        is ScannedInfo.Master -> childProducts
        else -> emptyList<Any>()
    }
    val hasChildren = childItems.isNotEmpty()

    Column(Modifier.padding(24.dp).fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        KineticCard(padding = 16.dp) {
            when (scannedInfo) {
                is ScannedInfo.Master -> MasterDetails(scannedInfo, qrCode)
                is ScannedInfo.UnitInfo -> UnitDetails(scannedInfo, qrCode)
                is ScannedInfo.Label -> LabelDetails(scannedInfo, qrCode)
                null -> {
                    Text("No encontrado", color = MaterialTheme.colorScheme.error)
                    Text("UUID: $qrCode")
                }
            }
        }

        if (hasChildren) {
            Spacer(Modifier.height(16.dp))
            Text("Desglose de Caja Master", style = MaterialTheme.typography.titleSmall)
            Text("Desmarque los pares que falten físicamente:", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Contenido Verificado: ${checkedUuids.size} / ${childItems.size} pares",
                color = if (checkedUuids.size == childItems.size) DispatchGreen else WarningOrange
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                Modifier.fillMaxWidth().heightIn(max = 180.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (childLabels.isNotEmpty()) {
                        items(childLabels, key = LabelEntity::uuid) { label ->
                            ChildRow(label.uuid, "Talla ${label.size} | Lote ${label.lot}", checkedUuids)
                        }
                    } else {
                        items(childProducts, key = ProductEntity::uuid) { product ->
                            ChildRow(product.uuid, "Talla ${product.size} | Lote ${product.lot} | Estado ${product.status}", checkedUuids)
                        }
                    }
                }
            }
        }

        when (moduleName) {
            "QUALITY" -> ReasonSelector(selectedReason, reasonExpanded, { reasonExpanded = it }) { selectedReason = it }
            "INBOUND_EMPAQUE" -> RackSelector(selectedReason, reasonExpanded, { reasonExpanded = it }) { selectedReason = it }
        }

        Spacer(Modifier.height(24.dp))
        KineticButton(
            text = if (moduleName == "VERIFY") "CERRAR" else "CONFIRMAR",
            onClick = {
                val checklist = if (hasChildren) checkedUuids.toList() else null
                onConfirm(
                    if (moduleName == "QUALITY" || moduleName == "INBOUND_EMPAQUE") selectedReason else "",
                    checklist
                )
            },
            Modifier.fillMaxWidth(),
            enabled = scannedInfo != null && (
                moduleName == "VERIFY" ||
                    (moduleName == "QUALITY" && selectedReason.isNotEmpty()) ||
                    (moduleName == "INBOUND_EMPAQUE" && selectedReason.isNotEmpty()) ||
                    (moduleName != "QUALITY" && moduleName != "INBOUND_EMPAQUE")
            ),
            type = if (moduleName == "VERIFY") ButtonType.SECONDARY else ButtonType.PRIMARY
        )
        TextButton(onClick = onCancel, Modifier.fillMaxWidth()) {
            Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MasterDetails(info: ScannedInfo.Master, qrCode: String) {
    Column {
        Text("Caja Master", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("UUID: $qrCode")
        Text("Modelo: ${info.entity.model}")
        Text("Talla: ${info.entity.size}")
        Text("Unidades: ${info.entity.activeChildCount}/${info.entity.childCount}")
        if (info.entity.status == "PENDIENTE_POR_RELLENAR") {
            Text("PENDIENTE POR RELLENAR", color = WarningOrange)
        }
        OriginText(info.entity.origin)
    }
}

@Composable
private fun UnitDetails(info: ScannedInfo.UnitInfo, qrCode: String) {
    Column {
        Text("Unidad Individual", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("UUID: $qrCode")
        Text("Modelo: ${info.entity.model}")
        Text("Talla: ${info.entity.size}")
        Text("Lote: ${info.entity.lot}")
        Text("Estado: ${info.entity.status}")
        OriginText(info.entity.origin)
        info.entity.parentUuid?.let { Text("Caja Master: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun LabelDetails(info: ScannedInfo.Label, qrCode: String) {
    val label = info.entity
    Column {
        Text("Etiqueta de Empaque", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("UUID: $qrCode")
        Text("Modelo: ${label.model}")
        Text("Talla: ${label.size}")
        Text("Lote: ${label.lot}")
        Text("Tipo: ${if (label.labelType == "MASTER_BOX") "CAJA MASTER" else "UNIDAD INDIVIDUAL"}")
        OriginText(label.origin)
    }
}

@Composable
private fun OriginText(origin: String) {
    Text("Origen: $origin", color = if (origin == "FOOT_SAFE") FootSafeYellow else SafetyCobalt)
}

@Composable
private fun ChildRow(uuid: String, details: String, checkedUuids: MutableList<String>) {
    val isChecked = uuid in checkedUuids
    Row(
        Modifier.fillMaxWidth().kineticClick {
            if (isChecked) checkedUuids.remove(uuid) else checkedUuids.add(uuid)
        }.padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(isChecked, { checked -> if (checked) checkedUuids.add(uuid) else checkedUuids.remove(uuid) })
        Spacer(Modifier.width(8.dp))
        Column {
            Text(uuid, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonSelector(value: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSelected: (String) -> Unit) {
    Spacer(Modifier.height(16.dp))
    ExposedDropdownMenuBox(expanded, onExpandedChange) {
        KineticTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = "Motivo de Baja *",
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { onExpandedChange(false) }) {
            BajaReason.entries.forEach { reason ->
                DropdownMenuItem(text = { Text(reason.displayName) }, onClick = {
                    onSelected(reason.displayName)
                    onExpandedChange(false)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RackSelector(value: String, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onSelected: (String) -> Unit) {
    val racks = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3", "PISO")
    Spacer(Modifier.height(16.dp))
    ExposedDropdownMenuBox(expanded, onExpandedChange) {
        KineticTextField(
            value = if (value.isEmpty()) "Seleccionar Rack..." else "Rack: $value",
            onValueChange = {},
            readOnly = true,
            label = "Rack de Destino *",
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { onExpandedChange(false) }) {
            racks.forEach { rack ->
                DropdownMenuItem(text = { Text(rack) }, onClick = {
                    onSelected(rack)
                    onExpandedChange(false)
                })
            }
        }
    }
}
