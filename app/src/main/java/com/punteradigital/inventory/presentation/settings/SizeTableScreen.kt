package com.punteradigital.inventory.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.ui.theme.*

private data class SizeRange(val model: String, val name: String, val min: Int, val max: Int, val color: Color)
private data class SizeConversion(val veCo: String, val us: String, val uk: String, val eu: String, val cm: String)

private val sizeRanges = listOf(
    SizeRange("FS300", "Foot Safe 300 Comp", 36, 46, FootSafeYellow),
    SizeRange("FS302", "Foot Safe 302", 38, 45, StandByAmber),
    SizeRange("FS400", "Foot Safe 400 Black", 39, 46, QualityPurple),
    SizeRange("SF200", "Safety 200 Lite", 36, 44, SafetyCobalt)
)

private val sizeConversions = listOf(
    SizeConversion("36", "4", "3.5", "36", "22.5"),
    SizeConversion("37", "5", "4.5", "37", "23.5"),
    SizeConversion("38", "6", "5.5", "38", "24.0"),
    SizeConversion("39", "6.5", "6", "39", "24.5"),
    SizeConversion("40", "7.5", "7", "40", "25.5"),
    SizeConversion("41", "8", "7.5", "41", "26.0"),
    SizeConversion("42", "9", "8.5", "42", "27.0"),
    SizeConversion("43", "9.5", "9", "43", "27.5"),
    SizeConversion("44", "10.5", "10", "44", "28.5"),
    SizeConversion("45", "11", "10.5", "45", "29.0"),
    SizeConversion("46", "12", "11.5", "46", "30.0")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SizeTableScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tabla de Tallas", fontWeight = FontWeight.Bold)
                        Text(
                            "Rangos por modelo y conversiones",
                            style = MaterialTheme.typography.bodySmall,
                            color = RefillBlue
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Size Ranges per Model
            item {
                KineticCard(padding = 16.dp) {
                    Text(
                        "Rangos por Modelo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("Modelo", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Text("Mín", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                        Text("Máx", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                        Text("Total", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
                    }

                    sizeRanges.forEachIndexed { index, range ->
                        val bgColor = if (index % 2 == 0)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else Color.Transparent

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(range.model, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = range.color)
                                Text(range.name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${range.min}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                            Text("${range.max}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                            Text("${range.max - range.min + 1}", modifier = Modifier.weight(0.5f), fontSize = 13.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = DispatchGreen)
                        }
                    }
                }
            }

            // International Conversion Table
            item {
                KineticCard(padding = 16.dp) {
                    Text(
                        "Conversiones Internacionales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val scrollState = rememberScrollState()
                    Row(modifier = Modifier.horizontalScroll(scrollState)) {
                        Column {
                            // Header
                            Row(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                    )
                            ) {
                                listOf("VE/CO", "US", "UK", "EU", "CM").forEach { header ->
                                    Box(
                                        modifier = Modifier
                                            .width(64.dp)
                                            .padding(horizontal = 4.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(header, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // Data rows
                            sizeConversions.forEachIndexed { index, conv ->
                                val bgColor = if (index % 2 == 0)
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else Color.Transparent

                                Row(modifier = Modifier.background(bgColor)) {
                                    listOf(conv.veCo, conv.us, conv.uk, conv.eu, conv.cm).forEachIndexed { colIdx, value ->
                                        Box(
                                            modifier = Modifier
                                                .width(64.dp)
                                                .padding(horizontal = 4.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                value,
                                                fontSize = 13.sp,
                                                fontWeight = if (colIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                color = if (colIdx == 0) FootSafeYellow else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}
