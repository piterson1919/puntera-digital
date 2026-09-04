package com.punteradigital.inventory.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.data.local.PrinterPreferences
import com.punteradigital.inventory.data.repository.PrintRepository
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterConfigScreen(
    printerPreferences: PrinterPreferences,
    printRepository: PrintRepository,
    onBack: () -> Unit
) {
    // Initialize fields from saved preferences
    var ipAddress by remember { mutableStateOf(printerPreferences.serverIp) }
    var port by remember { mutableStateOf(printerPreferences.serverPort.toString()) }
    var endpoint by remember { mutableStateOf("Integration/PunteraDigital_QR/Execute") }
    var resultText by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(printerPreferences.isConfigured) }
    var connectionSuccess by remember { mutableStateOf<Boolean?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Track if fields have been modified from saved values
    val hasChanges = remember(ipAddress, port) {
        ipAddress != printerPreferences.serverIp ||
                (port.toIntOrNull() ?: 0) != printerPreferences.serverPort
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Impresora QR", fontWeight = FontWeight.Bold)
                        Text(
                            "BarTender Enterprise",
                            style = MaterialTheme.typography.bodySmall,
                            color = QualityPurple
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic status banner
            val bannerColor = when {
                connectionSuccess == true -> DispatchGreen
                isSaved -> StandByAmber
                else -> CriticalRed
            }
            val bannerIcon = when {
                connectionSuccess == true -> Icons.Default.CheckCircle
                isSaved -> Icons.Default.Print
                else -> Icons.Default.Warning
            }
            val bannerTitle = when {
                connectionSuccess == true -> "Conectado"
                isSaved -> "Configurado"
                else -> "Sin Configurar"
            }
            val bannerSubtitle = when {
                connectionSuccess == true -> "Conexión verificada con BarTender"
                isSaved -> "Pendiente de verificar conexión"
                else -> "Ingrese IP y puerto del servidor"
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = bannerColor.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bannerColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(bannerIcon, null, tint = bannerColor, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(bannerTitle, fontWeight = FontWeight.Bold, color = bannerColor)
                        Text(bannerSubtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Server Config Card
            KineticCard(padding = 20.dp) {
                Text(
                    "Servidor BarTender",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                KineticTextField(
                    value = ipAddress,
                    onValueChange = {
                        ipAddress = it
                        connectionSuccess = null // Reset status on change
                    },
                    label = "Dirección IP del Servidor *",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KineticTextField(
                        value = port,
                        onValueChange = {
                            port = it
                            connectionSuccess = null // Reset status on change
                        },
                        label = "Puerto *",
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    KineticTextField(
                        value = "HTTP",
                        onValueChange = { },
                        label = "Protocolo",
                        modifier = Modifier.weight(1f),
                        readOnly = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                KineticTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = "Endpoint (BarTender Integration)",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save button
                KineticButton(
                    text = if (isSaved && !hasChanges) "GUARDADO ✓" else "GUARDAR",
                    onClick = {
                        val portInt = port.toIntOrNull()
                        if (ipAddress.isBlank()) {
                            resultText = "⚠ La dirección IP no puede estar vacía"
                            return@KineticButton
                        }
                        if (portInt == null || portInt !in 1..65535) {
                            resultText = "⚠ Puerto inválido (debe ser 1-65535)"
                            return@KineticButton
                        }

                        printerPreferences.saveConfig(
                            ip = ipAddress.trim(),
                            port = portInt,
                            protocol = "http"
                        )
                        isSaved = true
                        connectionSuccess = null
                        resultText = "✅ Configuración guardada: http://${ipAddress.trim()}:$portInt/"
                    },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.PRIMARY,
                    icon = { Icon(Icons.Default.Save, null, tint = Color.White) }
                )

                // Test connection button
                KineticButton(
                    text = if (isTesting) "PROBANDO..." else "PROBAR",
                    onClick = {
                        if (!isSaved) {
                            resultText = "⚠ Primero guarde la configuración"
                            return@KineticButton
                        }
                        if (isTesting) return@KineticButton // Prevent double-tap

                        isTesting = true
                        connectionSuccess = null
                        resultText = "🔄 Probando conexión a ${printerPreferences.getBaseUrl()}..."

                        coroutineScope.launch {
                            try {
                                val result = printRepository.testConnection()
                                when (result) {
                                    is PrintRepository.PrintResult.Success -> {
                                        connectionSuccess = true
                                        resultText = "✅ ¡Conexión exitosa! BarTender responde correctamente."
                                    }
                                    is PrintRepository.PrintResult.Error -> {
                                        connectionSuccess = false
                                        resultText = "❌ ${result.error.userMessage}"
                                    }
                                    is PrintRepository.PrintResult.Queued -> {
                                        connectionSuccess = false
                                        resultText = "⚠ ${result.message}"
                                    }
                                }
                            } catch (e: Exception) {
                                connectionSuccess = false
                                resultText = "❌ Error inesperado: ${e.message ?: "desconocido"}"
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    type = ButtonType.SECONDARY,
                    icon = { Icon(Icons.Default.Print, null, tint = Color.White) },
                    enabled = !isTesting
                )
            }

            // Result text
            if (resultText.isNotEmpty()) {
                KineticCard(padding = 16.dp) {
                    Text(
                        text = resultText,
                        color = when {
                            connectionSuccess == true -> DispatchGreen
                            connectionSuccess == false -> CriticalRed
                            resultText.startsWith("✅") -> DispatchGreen
                            resultText.startsWith("⚠") -> StandByAmber
                            resultText.startsWith("🔄") -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Connection info card
            if (isSaved) {
                KineticCard(padding = 16.dp) {
                    Text(
                        "Información de Conexión",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ConnectionInfoRow("URL Base", printerPreferences.getBaseUrl())
                    ConnectionInfoRow("Endpoint", endpoint)
                    ConnectionInfoRow("Timeout", "${printerPreferences.timeoutSeconds}s")
                    ConnectionInfoRow("Reintentos", if (printerPreferences.retryEnabled) "Sí (3 intentos)" else "No")
                    ConnectionInfoRow("Cola offline", if (printerPreferences.offlineQueueEnabled) "Activada" else "Desactivada")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ConnectionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
