package com.punteradigital.inventory.presentation.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.data.local.dao.BatchStatus
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.flow.Flow

enum class ChartType { BAR, PIE }

@Composable
fun DashboardScreen(batchStatusFlow: Flow<List<BatchStatus>>) {
    val batchList by batchStatusFlow.collectAsState(initial = emptyList())
    var selectedChart by remember { mutableStateOf(ChartType.BAR) }

    val chartColors = listOf(
        FootSafeYellow,
        DispatchGreen,
        QualityPurple,
        StandByAmber,
        SafetyCobalt,
        CriticalRed
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = selectedChart == ChartType.BAR,
                    onClick = { selectedChart = ChartType.BAR },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.BarChart, null, modifier = Modifier.size(16.dp)) }
                ) { Text("Barras") }
                SegmentedButton(
                    selected = selectedChart == ChartType.PIE,
                    onClick = { selectedChart = ChartType.PIE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.PieChart, null, modifier = Modifier.size(16.dp)) }
                ) { Text("Pastel") }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
                if (batchList.isEmpty()) {
                    Text("Sin datos en inventario", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    when (selectedChart) {
                        ChartType.BAR -> AnimatedBarChart(data = batchList, colors = chartColors)
                        ChartType.PIE -> AnimatedPieChart(data = batchList, colors = chartColors)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Detalle de Stock",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(batchList) { status ->
                StockItemRow(status, chartColors)
            }
        }
    }
}

@Composable
fun AnimatedBarChart(data: List<BatchStatus>, colors: List<Color>) {
    val maxCount = data.maxOfOrNull { it.count }?.toFloat() ?: 1f
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = 40f
        val barWidth = (size.width - (spacing * (data.size + 1))) / data.size

        data.forEachIndexed { index, batch ->
            val barHeight = (batch.count.toFloat() / maxCount) * size.height * animationProgress.value
            val x = spacing + index * (barWidth + spacing)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors[index % colors.size], colors[index % colors.size].copy(alpha = 0.5f))
                ),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
            )
        }
    }
}

@Composable
fun AnimatedPieChart(data: List<BatchStatus>, colors: List<Color>) {
    val total = data.sumOf { it.count }.toFloat()
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = -90f
            data.forEachIndexed { index, batch ->
                val sweepAngle = (batch.count.toFloat() / total) * 360f * animationProgress.value
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 40f, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${total.toInt()}", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Total", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StockItemRow(status: BatchStatus, colors: List<Color>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(status.size, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(status.model, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Lote: ${status.batch}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text("${status.count}", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(" uds", style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
