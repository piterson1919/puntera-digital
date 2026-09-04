package com.punteradigital.inventory.presentation.pedidos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.punteradigital.inventory.presentation.components.kineticClick
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.ui.theme.*

// ═══ MOCK DATA ═══
data class PedidoLine(
    val model: String,
    val size: String,
    val qty: Int,
    var scanned: Int = 0
)

data class ScannedPair(
    val uuid: String,
    val time: String,
    val user: String
)

data class Pedido(
    val id: String,
    val client: String,
    var status: String,           // pendiente, en-proceso, completo, entregado, retornado
    val creator: String,
    val date: String,
    val obs: String = "",
    val lines: List<PedidoLine>,
    val scannedPairs: MutableList<ScannedPair> = mutableListOf()
)

// Shared mock state
object PedidoRepository {
    val pedidos = mutableStateListOf(
        Pedido(
            id = "#4521", client = "Distribuidora Norte", status = "pendiente",
            creator = "Carlos Martínez", date = "05/05/2026 10:30", obs = "Pedido urgente",
            lines = listOf(
                PedidoLine("FS300CMFFFPBL", "42", 5, 0),
                PedidoLine("FS300CMFFFPBL", "43", 3, 0)
            )
        ),
        Pedido(
            id = "#4520", client = "Calzados Express", status = "en-proceso",
            creator = "Carlos Martínez", date = "04/05/2026 14:20",
            lines = listOf(PedidoLine("FS302CMN", "40", 6, 4)),
            scannedPairs = mutableListOf(
                ScannedPair("FS-2026B-40-001", "14:25", "José García"),
                ScannedPair("FS-2026B-40-002", "14:26", "José García"),
                ScannedPair("FS-2026B-40-003", "14:28", "José García"),
                ScannedPair("FS-2026B-40-004", "14:30", "José García")
            )
        ),
        Pedido(
            id = "#4519", client = "Tienda Central", status = "completo",
            creator = "Carlos Martínez", date = "03/05/2026 09:00",
            lines = listOf(PedidoLine("FS400BK", "44", 2, 2)),
            scannedPairs = mutableListOf(
                ScannedPair("FS-2026A-44-001", "09:15", "José García"),
                ScannedPair("FS-2026A-44-002", "09:17", "José García")
            )
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidoListScreen(
    onBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    currentUserRole: String = "ADMIN"
) {
    val pedidos = PedidoRepository.pedidos
    val canCreate = currentUserRole.equals("ADMIN", ignoreCase = true) || currentUserRole.equals("SUPERVISOR_ALMACEN", ignoreCase = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pedidos de Venta", fontWeight = FontWeight.Bold)
                        Text(
                            "${pedidos.size} pedidos activos",
                            style = MaterialTheme.typography.bodySmall,
                            color = DispatchGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(
                    onClick = onNavigateToCreate,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, "Nuevo Pedido")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            itemsIndexed(pedidos) { index, pedido ->
                PedidoCard(pedido = pedido, onClick = { onNavigateToDetail(index) })
            }
        }
    }
}

@Composable
fun PedidoCard(pedido: Pedido, onClick: () -> Unit) {
    val statusColor = when (pedido.status) {
        "pendiente" -> StandByAmber
        "en-proceso" -> RefillBlue
        "completo" -> DispatchGreen
        "entregado" -> Color(0xFF009688)
        "retornado" -> CriticalRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .kineticClick(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: ID + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pedido ${pedido.id}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(pedido.client, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        pedido.status.replace("-", " ").uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lines summary with progress bars
            pedido.lines.forEach { line ->
                val pct = if (line.qty > 0) (line.scanned.toFloat() / line.qty * 100).toInt() else 0
                val progressColor = when {
                    line.scanned >= line.qty -> DispatchGreen
                    line.scanned > 0 -> StandByAmber
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${line.model} · T${line.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${line.scanned}/${line.qty}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(6.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                RoundedCornerShape(3.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = pct / 100f)
                                .background(progressColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Meta
            Text(
                "👤 ${pedido.creator} · ${pedido.date}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
