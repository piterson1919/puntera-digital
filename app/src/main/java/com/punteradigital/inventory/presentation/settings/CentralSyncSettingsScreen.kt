package com.punteradigital.inventory.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.data.local.SyncPreferences
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.ui.theme.DispatchGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralSyncSettingsScreen(
    prefs: SyncPreferences,
    onBack: () -> Unit
) {
    var baseUrl by remember { mutableStateOf(prefs.baseUrl) }
    var wsUrl by remember { mutableStateOf(prefs.wsUrl) }
    var enabled by remember { mutableStateOf(prefs.isEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sincronización central") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                KineticCard(padding = 20.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = DispatchGreen)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Sincronización en tiempo real",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    KineticTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = "Base URL del servidor",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    KineticTextField(
                        value = wsUrl,
                        onValueChange = { wsUrl = it },
                        label = "WebSocket URL",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activar sincronización central")
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }

                    Spacer(Modifier.height(20.dp))

                    KineticButton(
                        text = "Guardar configuración",
                        onClick = {
                            prefs.save(baseUrl, wsUrl, enabled)
                            onBack()
                        },
                        type = com.punteradigital.inventory.presentation.components.ButtonType.PRIMARY
                    )
                }
            }
        }
    }
}
