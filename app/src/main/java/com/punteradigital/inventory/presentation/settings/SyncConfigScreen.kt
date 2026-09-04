package com.punteradigital.inventory.presentation.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncConfigScreen(onBack: () -> Unit) {
    var sheetId by remember { mutableStateOf("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs7") }
    var sheetName by remember { mutableStateOf("Inventario") }
    var serviceAccount by remember { mutableStateOf("puntera-digital@appspot.gserviceaccount.com") }
    var autoSync by remember { mutableStateOf(true) }
    var syncOnEntry by remember { mutableStateOf(true) }
    var syncOnDispatch by remember { mutableStateOf(true) }
    var syncInterval by remember { mutableStateOf("5") }
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncStatus by remember { mutableStateOf("Sincronizado") }
    var lastSyncDetail by remember { mutableStateOf("Última sincronización: hace 5 min") }

    val coroutineScope = rememberCoroutineScope()

    // Rotation animation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sincronización", fontWeight = FontWeight.Bold)
                        Text(
                            "Google Sheets",
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
            // Status banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSyncing) StandByAmber.copy(alpha = 0.1f)
                            else DispatchGreen.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSyncing) StandByAmber.copy(alpha = 0.2f)
                                    else DispatchGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSyncing) {
                                    Icon(
                                        Icons.Default.Sync,
                                        null,
                                        tint = StandByAmber,
                                        modifier = Modifier.padding(8.dp).rotate(rotation)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = DispatchGreen,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                lastSyncStatus,
                                fontWeight = FontWeight.Bold,
                                color = if (isSyncing) StandByAmber else DispatchGreen
                            )
                            Text(
                                lastSyncDetail,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Connection config
            item {
                KineticCard(padding = 20.dp) {
                    Text(
                        "Conexión Google Sheets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    KineticTextField(
                        value = sheetId,
                        onValueChange = { sheetId = it },
                        label = "ID de Hoja de Cálculo *",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    KineticTextField(
                        value = sheetName,
                        onValueChange = { sheetName = it },
                        label = "Nombre de la Hoja *",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    KineticTextField(
                        value = serviceAccount,
                        onValueChange = { serviceAccount = it },
                        label = "Cuenta de Servicio",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Sync toggles
            item {
                KineticCard(padding = 16.dp) {
                    Text(
                        "Configuración de Sincronización",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SyncToggle(
                        title = "Sincronización Automática",
                        subtitle = "Cada $syncInterval minutos cuando hay conexión",
                        checked = autoSync,
                        onCheckedChange = { autoSync = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SyncToggle(
                        title = "Sync al Registrar Entrada",
                        subtitle = "Sincronizar inmediatamente tras cada ingreso",
                        checked = syncOnEntry,
                        onCheckedChange = { syncOnEntry = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SyncToggle(
                        title = "Sync al Despachar",
                        subtitle = "Sincronizar inmediatamente al confirmar salida",
                        checked = syncOnDispatch,
                        onCheckedChange = { syncOnDispatch = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    KineticTextField(
                        value = syncInterval,
                        onValueChange = { syncInterval = it },
                        label = "Intervalo (minutos)",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Sync button
            item {
                KineticButton(
                    text = if (isSyncing) "⏳ SINCRONIZANDO..." else "🔄 SINCRONIZAR AHORA",
                    onClick = {
                        if (!isSyncing) {
                            isSyncing = true
                            lastSyncStatus = "Sincronizando..."
                            lastSyncDetail = "Conectando con Google Sheets..."
                            coroutineScope.launch {
                                delay(1500)
                                lastSyncDetail = "Enviando datos de inventario..."
                                delay(1500)
                                isSyncing = false
                                lastSyncStatus = "Sincronizado"
                                lastSyncDetail = "Última sincronización: ahora mismo"
                            }
                        }
                    },
                    type = if (isSyncing) ButtonType.SECONDARY else ButtonType.PRIMARY,
                    icon = {
                        Icon(
                            Icons.Default.CloudSync,
                            null,
                            tint = Color.White,
                            modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier
                        )
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SyncToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = KineticPrimary)
        )
    }
}
