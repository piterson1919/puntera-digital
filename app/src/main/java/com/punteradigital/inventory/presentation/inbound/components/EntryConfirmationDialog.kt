package com.punteradigital.inventory.presentation.inbound.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.viewmodel.RemainderMode

@Composable
fun EntryConfirmationDialog(
    selectedOrigin: Origin,
    selectedModel: String,
    selectedSize: String,
    lot: String,
    selectedRack: String,
    totalQty: Int,
    isMasterBox: Boolean,
    pairsPerBox: Int,
    autoBox: BusinessRules.AutoBoxResult?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Checklist,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = { Text("Confirmar Registro", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Verifica los datos antes de registrar:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                ConfirmRow("Origen", selectedOrigin.displayName)
                ConfirmRow("Modelo", selectedModel)
                ConfirmRow("Talla", selectedSize)
                ConfirmRow("Lote", lot)
                ConfirmRow("Rack", selectedRack)
                ConfirmRow("Cantidad", "$totalQty unidades")
                if (isMasterBox && autoBox != null) {
                    ConfirmRow("Cajas", "${autoBox.fullBoxes} × $pairsPerBox pares")
                }
            }
        },
        confirmButton = {
            KineticButton(
                text = "✅ CONFIRMAR",
                onClick = onConfirm,
                type = ButtonType.PRIMARY
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun ConfirmRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
