package com.punteradigital.inventory.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.data.local.dao.LocationCount
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RackMapScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val rackOccupancy by viewModel.rackOccupancy.collectAsState(initial = emptyList())
    var selectedRack by remember { mutableStateOf<String?>(null) }
    var rackProducts by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val rackNames = listOf("A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2", "C3")
    val rackCapacity = 50 // Estimated max per rack for color coding

    val occupancyMap = rackOccupancy.associate { it.location to it.count }
    val totalItems = rackOccupancy.sumOf { it.count }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mapa de Racks", fontWeight = FontWeight.Bold)
                        Text(
                            "$totalItems productos en almacén",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Rack Grid 3x3
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rackNames) { rack ->
                    val count = occupancyMap[rack] ?: 0
                    val fillPercent = (count.toFloat() / rackCapacity).coerceIn(0f, 1f)
                    val rackColor = when {
                        count == 0 -> MaterialTheme.colorScheme.surfaceContainerLow
                        fillPercent < 0.5f -> DispatchGreen
                        fillPercent < 0.8f -> StandByAmber
                        else -> CriticalRed
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = rackColor.copy(alpha = 0.12f),
                        onClick = {
                            selectedRack = rack
                            coroutineScope.launch {
                                rackProducts = viewModel.getProductsAtRack(rack)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                rack,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk,
                                color = if (count > 0) rackColor else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$count",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "pares",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Fill bar
                            if (count > 0) {
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fillPercent)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(rackColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // PISO area
            val pisoCount = occupancyMap["PISO"] ?: 0
            KineticCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    selectedRack = "PISO"
                    coroutineScope.launch {
                        rackProducts = viewModel.getProductsAtRack("PISO")
                    }
                },
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewInAr, null, tint = StandByAmber)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("PISO", fontWeight = FontWeight.Bold)
                            Text("Productos sin rack asignado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (pisoCount > 0) StandByAmber.copy(alpha = 0.15f) else
                            MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            "$pisoCount",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (pisoCount > 0) StandByAmber else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendDot(color = DispatchGreen, label = "< 50%")
                LegendDot(color = StandByAmber, label = "50-80%")
                LegendDot(color = CriticalRed, label = "> 80%")
                LegendDot(color = MaterialTheme.colorScheme.surfaceContainerLow, label = "Vacío")
            }
        }

        // Rack detail dialog
        if (selectedRack != null) {
            AlertDialog(
                onDismissRequest = { selectedRack = null },
                icon = {
                    Icon(Icons.Default.GridView, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp))
                },
                title = {
                    Text("Rack $selectedRack · ${rackProducts.size} productos",
                        fontWeight = FontWeight.Bold)
                },
                text = {
                    if (rackProducts.isEmpty()) {
                        Text("Sin productos en este rack",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(rackProducts) { product ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(product.uuid,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = SpaceGrotesk)
                                            Text("${product.model} · T.${product.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DispatchGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                product.status,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DispatchGreen,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedRack = null }) {
                        Text("CERRAR")
                    }
                }
            )
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
