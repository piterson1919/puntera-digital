package com.punteradigital.inventory.presentation.traceability

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.data.local.dao.*
import com.punteradigital.inventory.data.local.entity.MovementEntity
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════
// MAIN TRACEABILITY SCREEN — 3 Sub-Tabs
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceabilityScreen(viewModel: InventoryViewModel) {
    val tabTitles = listOf("Resumen", "Analytics", "Movimientos")
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ═══ HEADER ═══
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            Text(
                "Trazabilidad",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Inteligencia Operativa",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // ═══ SUB-TABS ═══
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            title.uppercase(),
                            fontFamily = SpaceGrotesk,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                )
            }
        }

        // beyondViewportPageCount = 0 ensures only the visible tab is composed.
        // Without this, all 3 tabs compose simultaneously, wasting resources on
        // heavy Flow collections and chart rendering for off-screen content.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0
        ) { page ->
            when (page) {
                0 -> ResumenTab(viewModel)
                1 -> AnalyticsTab(viewModel)
                2 -> MovimientosTab(viewModel)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB 1: RESUMEN — KPIs + Top Charts
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ResumenTab(viewModel: InventoryViewModel) {
    val totalAvailable by viewModel.totalAvailable.collectAsState(initial = 0)
    val totalStandBy by viewModel.totalStandBy.collectAsState(initial = 0)
    val totalDispatched by viewModel.totalDispatched.collectAsState(initial = 0)
    val totalMovements by viewModel.totalMovements.collectAsState(initial = 0)
    val topModels by viewModel.topDispatchedModels.collectAsState(initial = emptyList())
    val topSizes by viewModel.topDispatchedSizes.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═══ KPI CARDS ═══
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard("📦", "Stock", "$totalAvailable", DispatchGreen, Modifier.weight(1f))
                KpiCard("🚚", "Despachados", "$totalDispatched", KineticWarning, Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard("⏸", "Stand-By", "$totalStandBy", StandByAmber, Modifier.weight(1f))
                KpiCard("📋", "Movimientos", "$totalMovements", RefillBlue, Modifier.weight(1f))
            }
        }

        // ═══ TOP MODELOS DESPACHADOS ═══
        item {
            ChartSection(
                title = "📊 Top Modelos Despachados",
                subtitle = "Ranking por unidades enviadas"
            ) {
                if (topModels.isEmpty()) {
                    EmptyChartPlaceholder("Sin despachos registrados")
                } else {
                    val maxCount = topModels.maxOf { it.count }
                    topModels.take(5).forEachIndexed { index, model ->
                        HorizontalBarItem(
                            rank = index + 1,
                            label = model.model,
                            value = model.count,
                            maxValue = maxCount,
                            color = barChartColor(index)
                        )
                        if (index < minOf(topModels.size, 5) - 1) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // ═══ DISTRIBUCIÓN DE TALLAS ═══
        item {
            ChartSection(
                title = "👟 Tallas Más Despachadas",
                subtitle = "Distribución por talla"
            ) {
                if (topSizes.isEmpty()) {
                    EmptyChartPlaceholder("Sin despachos registrados")
                } else {
                    val maxCount = topSizes.maxOf { it.count }
                    // Show as a grid of size bubbles
                    val rows = topSizes.take(12).chunked(4)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { size ->
                                SizeBubble(
                                    size = size.size,
                                    count = size.count,
                                    maxCount = maxCount,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining slots
                            repeat(4 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB 2: ANALYTICS — Deep Insights
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AnalyticsTab(viewModel: InventoryViewModel) {
    val topClients by viewModel.topClients.collectAsState(initial = emptyList())
    val modelBreakdown by viewModel.modelStatusBreakdown.collectAsState(initial = emptyList())
    val allModels by viewModel.allModelsInventory.collectAsState(initial = emptyList())
    val allSizes by viewModel.allSizesInventory.collectAsState(initial = emptyList())
    val weeklyMovements by viewModel.weeklyMovements.collectAsState(initial = emptyList())

    // Weekly activity (calculated client-side)
    val weeklyActivity = remember(weeklyMovements) {
        calculateWeeklyActivity(weeklyMovements)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═══ TOP CLIENTES ═══
        item {
            ChartSection(
                title = "🏆 Top Clientes por Pedidos",
                subtitle = "Volumen de despachos por cliente"
            ) {
                if (topClients.isEmpty()) {
                    EmptyChartPlaceholder("Sin clientes registrados en despachos")
                } else {
                    val maxCount = topClients.maxOf { it.count }
                    topClients.take(5).forEachIndexed { index, client ->
                        ClientRankItem(
                            rank = index + 1,
                            name = client.cliente,
                            count = client.count,
                            maxCount = maxCount,
                            color = clientChartColor(index)
                        )
                        if (index < minOf(topClients.size, 5) - 1) {
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // ═══ INVENTARIO POR MODELO (Breakdown) ═══
        item {
            ChartSection(
                title = "📦 Inventario por Modelo",
                subtitle = "Desglose por estado de cada modelo"
            ) {
                if (modelBreakdown.isEmpty()) {
                    EmptyChartPlaceholder("Sin productos registrados")
                } else {
                    val grouped = modelBreakdown.groupBy { it.model }
                    grouped.entries.forEachIndexed { index, (model, statuses) ->
                        ModelBreakdownCard(model = model, statuses = statuses)
                        if (index < grouped.size - 1) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // ═══ ACTIVIDAD SEMANAL ═══
        item {
            ChartSection(
                title = "📈 Actividad Últimos 7 Días",
                subtitle = "Movimientos por día"
            ) {
                if (weeklyActivity.isEmpty() || weeklyActivity.all { it.second == 0 }) {
                    EmptyChartPlaceholder("Sin actividad reciente")
                } else {
                    val maxCount = weeklyActivity.maxOf { it.second }.coerceAtLeast(1)
                    weeklyActivity.forEach { (dayLabel, count) ->
                        WeeklyBarItem(
                            dayLabel = dayLabel,
                            count = count,
                            maxCount = maxCount
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        // ═══ TODOS LOS MODELOS EN INVENTARIO ═══
        item {
            ChartSection(
                title = "🏷 Modelos en Inventario",
                subtitle = "Stock disponible por modelo"
            ) {
                if (allModels.isEmpty()) {
                    EmptyChartPlaceholder("Sin stock disponible")
                } else {
                    allModels.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                model.model,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SpaceGrotesk,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DispatchGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "${model.count} pares",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DispatchGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }

        // ═══ TODAS LAS TALLAS EN INVENTARIO ═══
        item {
            ChartSection(
                title = "📏 Tallas en Inventario",
                subtitle = "Stock disponible por talla"
            ) {
                if (allSizes.isEmpty()) {
                    EmptyChartPlaceholder("Sin tallas en stock")
                } else {
                    val maxCount = allSizes.maxOf { it.count }.coerceAtLeast(1)
                    val rows = allSizes.chunked(3)
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { size ->
                                SizeInventoryChip(
                                    size = size.size,
                                    count = size.count,
                                    maxCount = maxCount,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ═══════════════════════════════════════════════════════════════
// TAB 3: MOVIMIENTOS — Search + Filter + Timeline
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MovimientosTab(viewModel: InventoryViewModel) {
    val movements by viewModel.traceabilityMovements.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }

    val filterTypes = listOf("Todos", "Entrada", "Stand-By", "Despacho", "Calidad", "Muestra", "Refill")

    val filteredMovements = remember(movements, searchQuery, selectedFilter) {
        movements.filter { movement ->
            val matchesSearch = if (searchQuery.isBlank()) true
            else movement.uuid.contains(searchQuery, ignoreCase = true) ||
                    movement.reason.contains(searchQuery, ignoreCase = true) ||
                    movement.observation.contains(searchQuery, ignoreCase = true) ||
                    movement.cliente.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Todos" -> true
                "Entrada" -> movement.type == "IN"
                "Stand-By" -> movement.type == "STB"
                "Despacho" -> movement.type == "OUT"
                "Calidad" -> movement.type == "BAJA"
                "Muestra" -> movement.type == "MUESTRA"
                "Refill" -> movement.type == "REFILL"
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ═══ SEARCH BAR ═══
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Buscar UUID, modelo, lote, cliente...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        )

        // ═══ FILTER CHIPS ═══
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterTypes) { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                        } else {
                            Text(filterEmoji(filter), fontSize = 12.sp)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ═══ RESULTS COUNT ═══
        Text(
            "${filteredMovements.size} movimiento${if (filteredMovements.size != 1) "s" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // ═══ TIMELINE ═══
        if (filteredMovements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📭", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isNotEmpty()) "Sin resultados para \"$searchQuery\""
                        else "Sin movimientos registrados",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp, bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(filteredMovements) { index, movement ->
                    TimelineMovementItem(
                        movement = movement,
                        isFirst = index == 0,
                        isLast = index == filteredMovements.lastIndex
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun KpiCard(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    KineticCard(modifier = modifier, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGrotesk,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChartSection(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    KineticCard(padding = 20.dp) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

// ═══ HORIZONTAL BAR CHART ITEM ═══
@Composable
private fun HorizontalBarItem(
    rank: Int,
    label: String,
    value: Int,
    maxValue: Int,
    color: Color
) {
    val fraction = if (maxValue > 0) value.toFloat() / maxValue else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank badge
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "$rank",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(Modifier.width(8.dp))

        // Label
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            fontFamily = SpaceGrotesk,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(8.dp))

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = fraction.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.6f))
                        )
                    )
            )
        }
        Spacer(Modifier.width(8.dp))

        // Value
        Text(
            "$value",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk,
            color = color
        )
    }
}

// ═══ SIZE BUBBLE ═══
@Composable
private fun SizeBubble(
    size: String,
    count: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    val intensity = if (maxCount > 0) (count.toFloat() / maxCount).coerceIn(0.2f, 1f) else 0.3f
    val color = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = intensity * 0.25f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                size,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk,
                color = color
            )
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══ CLIENT RANK ITEM ═══
@Composable
private fun ClientRankItem(
    rank: Int,
    name: String,
    count: Int,
    maxCount: Int,
    color: Color
) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val medals = listOf("🥇", "🥈", "🥉")

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Medal or rank
        if (rank <= 3) {
            Text(medals[rank - 1], fontSize = 20.sp)
        } else {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$rank", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = fraction.coerceIn(0.03f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                "$count",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ═══ MODEL BREAKDOWN CARD ═══
@Composable
private fun ModelBreakdownCard(
    model: String,
    statuses: List<ModelStatusBreakdown>
) {
    val total = statuses.sumOf { it.count }
    val statusColors = mapOf(
        "AVAILABLE" to DispatchGreen,
        "STB" to StandByAmber,
        "DISPATCHED" to RefillBlue,
        "MUESTRA" to MuestraTeal,
        "BAJA_HIDROLIZADO" to QualityPurple,
        "BAJA_SEGUNDA" to QualityPurple,
        "BAJA_FISICO" to QualityPurple,
        "MUESTRA_VENDIDA" to KineticWarning
    )
    val statusLabels = mapOf(
        "AVAILABLE" to "Disponible",
        "STB" to "Stand-By",
        "DISPATCHED" to "Despachado",
        "MUESTRA" to "Muestra",
        "MUESTRA_VENDIDA" to "Vendida"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                model,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "$total total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))

        // Segmented progress bar
        if (total > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                statuses.sortedByDescending { it.count }.forEach { status ->
                    val fraction = status.count.toFloat() / total
                    val color = statusColors[status.status] ?: MaterialTheme.colorScheme.outline
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(fraction.coerceAtLeast(0.01f))
                            .background(color)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            statuses.sortedByDescending { it.count }.take(4).forEach { status ->
                val color = statusColors[status.status] ?: MaterialTheme.colorScheme.outline
                val label = statusLabels[status.status] ?: status.status.lowercase()
                    .replaceFirstChar { it.uppercase() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "$label: ${status.count}",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═══ WEEKLY BAR ITEM ═══
@Composable
private fun WeeklyBarItem(dayLabel: String, count: Int, maxCount: Int) {
    val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
    val color = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = fraction.coerceIn(0.02f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color, color.copy(alpha = 0.5f))
                        )
                    )
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGrotesk,
            color = color,
            modifier = Modifier.width(28.dp)
        )
    }
}

// ═══ SIZE INVENTORY CHIP ═══
@Composable
private fun SizeInventoryChip(
    size: String,
    count: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    val intensity = if (maxCount > 0) (count.toFloat() / maxCount).coerceIn(0.15f, 1f) else 0.2f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DispatchGreen.copy(alpha = intensity * 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "T.$size",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DispatchGreen
            )
        }
    }
}

// ═══ TIMELINE MOVEMENT ITEM ═══
@Composable
fun TimelineMovementItem(
    movement: MovementEntity,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val dateStr = sdf.format(Date(movement.timestamp))
    val relativeTime = getRelativeTime(movement.timestamp)

    val (emoji, color, typeLabel) = remember(movement.type) {
        when (movement.type) {
            "IN" -> Triple("📥", DispatchGreen, "Entrada")
            "STB" -> Triple("⏸", StandByAmber, "Stand-By")
            "OUT" -> Triple("🚚", CriticalRed, "Despacho")
            "BAJA" -> Triple("⊖", QualityPurple, "Calidad")
            "MUESTRA" -> Triple("🏪", MuestraTeal, "Muestra")
            "REFILL" -> Triple("📦", RefillBlue, "Refill")
            else -> Triple("🔄", RefillBlue, movement.type)
        }
    }

    var isExpanded by remember { mutableStateOf(false) }
    val hasDetails = movement.cliente.isNotEmpty() || movement.observacionesExtra.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        // Timeline line + dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Top line
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            } else {
                Spacer(Modifier.height(12.dp))
            }

            // Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )

            // Bottom line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f, fill = true)
                        .defaultMinSize(minHeight = 40.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Content card
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.12f)
                    ) {
                        Text(
                            typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(4.dp))

            // UUID
            Text(
                movement.uuid,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = SpaceGrotesk,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Reason / observation
            if (movement.reason.isNotEmpty()) {
                Text(
                    movement.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            if (movement.observation.isNotEmpty()) {
                Text(
                    movement.observation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Date + location
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                if (movement.location.isNotEmpty()) {
                    Text(
                        " · ",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        movement.location,
                        fontSize = 10.sp,
                        color = color.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(visible = isExpanded && hasDetails) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (movement.cliente.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👤", fontSize = 13.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Cliente: ${movement.cliente}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (movement.observacionesExtra.isNotEmpty()) {
                            if (movement.cliente.isNotEmpty()) Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Top) {
                                Text("📝", fontSize = 13.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    movement.observacionesExtra,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔑", fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Operador: ${movement.userId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Expand hint
            if (hasDetails && !isExpanded) {
                Text(
                    "▾ Ver detalles",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════

private fun barChartColor(index: Int): Color = when (index) {
    0 -> Color(0xFFF2D16B)  // Gold
    1 -> Color(0xFF5B9BFF)  // Cobalt
    2 -> Color(0xFF30D158)  // Green
    3 -> Color(0xFFAF52DE)  // Purple
    4 -> Color(0xFFFF9500)  // Orange
    else -> Color(0xFF64B5F6)
}

private fun clientChartColor(index: Int): Color = when (index) {
    0 -> Color(0xFF30D158)
    1 -> Color(0xFF5B9BFF)
    2 -> Color(0xFFFF9500)
    3 -> Color(0xFFAF52DE)
    4 -> Color(0xFF009688)
    else -> Color(0xFF64B5F6)
}

private fun filterEmoji(filter: String): String = when (filter) {
    "Todos" -> "🔄"
    "Entrada" -> "📥"
    "Stand-By" -> "⏸"
    "Despacho" -> "🚚"
    "Calidad" -> "⊖"
    "Muestra" -> "🏪"
    "Refill" -> "📦"
    else -> "📋"
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun calculateWeeklyActivity(movements: List<MovementEntity>): List<Pair<String, Int>> {
    val cal = Calendar.getInstance()
    val dayLabels = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
    val now = System.currentTimeMillis()
    val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)

    val recentMovements = movements.filter { it.timestamp >= sevenDaysAgo }

    // Group by day of week
    val groupedByDay = mutableMapOf<Int, Int>()
    recentMovements.forEach { movement ->
        cal.timeInMillis = movement.timestamp
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday, 7=Saturday
        groupedByDay[dayOfWeek] = (groupedByDay[dayOfWeek] ?: 0) + 1
    }

    // Build ordered list starting from today going back 7 days
    val result = mutableListOf<Pair<String, Int>>()
    cal.timeInMillis = now
    for (i in 6 downTo 0) {
        val tempCal = Calendar.getInstance()
        tempCal.timeInMillis = now
        tempCal.add(Calendar.DAY_OF_YEAR, -i)
        val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val label = dayLabels[dayOfWeek - 1]
        val count = groupedByDay[dayOfWeek] ?: 0
        result.add(label to count)
    }
    return result
}
