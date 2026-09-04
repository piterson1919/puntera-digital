package com.punteradigital.inventory.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.ui.theme.*

private data class BoxingModel(val sku: String, val name: String, val defaultPairs: Int)

private val boxingModels = listOf(
    BoxingModel("FS300CMFFPBL", "Foot Safe 300 Comp", 8),
    BoxingModel("FS302CMN", "Foot Safe 302", 8),
    BoxingModel("FS400BK", "Foot Safe 400 Black", 6),
    BoxingModel("SF200LT", "Safety 200 Lite", 10)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxingConfigScreen(onBack: () -> Unit) {
    val pairsMap = remember {
        mutableStateMapOf<String, String>().apply {
            boxingModels.forEach { put(it.sku, it.defaultPairs.toString()) }
        }
    }
    var autoCreate by remember { mutableStateOf(true) }
    var allowIncomplete by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pares por Caja Master", fontWeight = FontWeight.Bold)
                        Text(
                            "Configuración de auto-boxing",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pairs per model
            item {
                KineticCard(padding = 20.dp) {
                    Text(
                        "Pares por Modelo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    boxingModels.forEachIndexed { index, model ->
                        if (index > 0) Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(model.sku, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(model.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            KineticTextField(
                                value = pairsMap[model.sku] ?: model.defaultPairs.toString(),
                                onValueChange = { pairsMap[model.sku] = it },
                                label = "Pares",
                                modifier = Modifier.width(80.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Toggles
            item {
                KineticCard(padding = 16.dp) {
                    Text(
                        "Opciones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-crear Caja Master", fontWeight = FontWeight.SemiBold)
                            Text("Crear automáticamente al alcanzar el límite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoCreate,
                            onCheckedChange = { autoCreate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = KineticPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Permitir Cajas Incompletas", fontWeight = FontWeight.SemiBold)
                            Text("Despachar cajas sin completar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = allowIncomplete,
                            onCheckedChange = { allowIncomplete = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = KineticPrimary)
                        )
                    }
                }
            }

            // Save button
            item {
                KineticButton(
                    text = if (showSaved) "✅ ¡GUARDADO!" else "💾 GUARDAR CONFIGURACIÓN",
                    onClick = { showSaved = true },
                    type = if (showSaved) ButtonType.SECONDARY else ButtonType.PRIMARY
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
