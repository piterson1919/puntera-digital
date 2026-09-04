package com.punteradigital.inventory.presentation.inbound.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedRackSettings(
    viewModel: InventoryViewModel,
    isRackLocked: Boolean,
    lockedRack: String?,
    rackLocations: List<String>
) {
    KineticCard(padding = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "🔒 Fijar Rack para Ingresos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "El escáner ingresará los QR en este rack directamente en modo ráfaga.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isRackLocked,
                    onCheckedChange = { viewModel.setRackLocked(it) }
                )
            }

            if (isRackLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                var rackDropExpanded by remember { mutableStateOf(false) }
                val currentLocked = lockedRack ?: "A1"
                
                ExposedDropdownMenuBox(
                    expanded = rackDropExpanded,
                    onExpandedChange = { rackDropExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KineticTextField(
                        value = "📍 Rack Fijado: $currentLocked",
                        onValueChange = {},
                        readOnly = true,
                        label = "Rack de Destino Fijo",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rackDropExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = rackDropExpanded,
                        onDismissRequest = { rackDropExpanded = false }
                    ) {
                        rackLocations.filter { it != "PISO" }.forEach { rack ->
                            DropdownMenuItem(
                                text = { Text("📍 $rack") },
                                onClick = {
                                    viewModel.setLockedRack(rack)
                                    rackDropExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
