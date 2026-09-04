package com.punteradigital.inventory.presentation.inbound.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.confirmPreEntryBatch
import com.punteradigital.inventory.ui.theme.DispatchGreen

@Composable
fun PreEntryTab(viewModel: InventoryViewModel, currentUserId: String) {
    val pendingBatches by viewModel.pendingLabelBatches.collectAsState()

    if (pendingBatches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No hay lotes de etiquetas pendientes por ingresar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val df = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(pendingBatches.filter { it.enteredCount < it.totalCount }) { batch ->
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
                        Text(
                            text = "${batch.model} - Talla ${batch.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${batch.totalCount - batch.enteredCount} uds",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Lote: ${batch.lot} | Origen: ${Origin.valueOf(batch.origin).displayName}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Generado el ${df.format(java.util.Date(batch.createdAt))} por ${batch.createdBy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.confirmPreEntryBatch(batch.batchId, currentUserId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DispatchGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CONFIRMAR ENTRADA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
