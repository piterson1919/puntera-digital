package com.punteradigital.inventory.presentation.home

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.R
import com.punteradigital.inventory.presentation.traceability.TimelineMovementItem
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: InventoryViewModel,
    onNavigateToEntry: () -> Unit,
    onNavigateToMovements: () -> Unit,
    onNavigateToRefill: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val origin by viewModel.currentOrigin.collectAsState()
    val batchStatus by viewModel.inventoryStatus.collectAsState(initial = emptyList())
    val movements by viewModel.recentMovements.collectAsState(initial = emptyList())
    val totalAvailable by viewModel.totalAvailable.collectAsState(initial = 0)
    val totalStandBy by viewModel.totalStandBy.collectAsState(initial = 0)
    val totalMasterBoxes by viewModel.totalMasterBoxes.collectAsState(initial = 0)
    val pendingSync by viewModel.pendingSyncCount.collectAsState(initial = 0)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Single LazyColumn for the entire screen — avoids nested scroll conflicts
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 16.dp)
    ) {
        // ═══ HEADER ═══
        item(key = "header") {
            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Hola, ${user?.name ?: "Operador"} \uD83D\uDC4B",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Modo: ${origin.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Sync indicator
                if (pendingSync > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StandByAmber.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SyncProblem, null, tint = StandByAmber, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("$pendingSync", style = MaterialTheme.typography.labelSmall, color = StandByAmber)
                        }
                    }
                }
            }
        }

        // ═══ PRODUCT HERO CARD ═══
        item(key = "hero") {
            val heroHeight = if (isLandscape) 120.dp else 180.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.boot_black),
                    contentDescription = "Producto Destacado",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    val topModel = androidx.compose.runtime.remember(batchStatus) {
                        batchStatus.maxByOrNull { it.count }?.model ?: "FS300CMFFPBL"
                    }
                    Text("Selección Activa", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(topModel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = KineticSuccess.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "LOTE ACTIVO: 2026A",
                            color = KineticSuccess,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // ═══ COUNTER GRID ═══
        if (isLandscape) {
            // Landscape: all 4 counters in a single row
            item(key = "counters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmojiCounterCard("📦", "Stock", "$totalAvailable", DispatchGreen, Modifier.weight(1f))
                    EmojiCounterCard("⏸", "Stand-By", "$totalStandBy", StandByAmber, Modifier.weight(1f))
                    EmojiCounterCard("📦", "Cajas Master", "$totalMasterBoxes", QualityPurple, Modifier.weight(1f))
                    val distinctModelCount = remember(batchStatus) { batchStatus.map { it.model }.distinct().size }
                    EmojiCounterCard("🏷", "Modelos", "$distinctModelCount", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
        } else {
            // Portrait: 2×2 grid
            item(key = "counters_row1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmojiCounterCard("📦", "Stock", "$totalAvailable", DispatchGreen, Modifier.weight(1f))
                    EmojiCounterCard("⏸", "Stand-By", "$totalStandBy", StandByAmber, Modifier.weight(1f))
                }
            }
            item(key = "counters_row2") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmojiCounterCard("📦", "Cajas Master", "$totalMasterBoxes", QualityPurple, Modifier.weight(1f))
                    val distinctModelCount = remember(batchStatus) { batchStatus.map { it.model }.distinct().size }
                    EmojiCounterCard("🏷", "Modelos", "$distinctModelCount", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                }
            }
        }

        // ═══ RECENT MOVEMENTS HEADER ═══
        item(key = "movements_header") {
            Text(
                "Movimientos Recientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ═══ RECENT MOVEMENTS LIST ═══
        if (movements.isEmpty()) {
            item(key = "movements_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin movimientos recientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(
                items = movements.take(10),
                key = { it.id }
            ) { movement ->
                TimelineMovementItem(movement)
            }
        }
    }
}

@Composable
fun EmojiCounterCard(
    emoji: String,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    KineticCard(
        modifier = modifier,
        padding = 16.dp
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
