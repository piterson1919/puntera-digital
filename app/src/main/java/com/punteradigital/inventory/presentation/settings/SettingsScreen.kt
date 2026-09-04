package com.punteradigital.inventory.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.data.local.ThemePreferences
import com.punteradigital.inventory.data.local.entity.UserEntity
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToCatalog: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToQRHistory: () -> Unit = {},
    onNavigateToRackMap: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToSizeTable: () -> Unit = {},
    onNavigateToBoxing: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onLogout: () -> Unit = {},
    themePreferences: ThemePreferences? = null,
    currentUser: UserEntity? = null
) {
    if (onBack != null) {
        // Standalone mode (navigated via NavController) — own Scaffold with back button
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Configuración", fontWeight = FontWeight.Bold)
                            Text(
                                "Panel de Administración",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
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
            SettingsContent(
                modifier = Modifier.padding(padding),
                onNavigateToCatalog = onNavigateToCatalog,
                onNavigateToPrinter = onNavigateToPrinter,
                onNavigateToQRHistory = onNavigateToQRHistory,
                onNavigateToRackMap = onNavigateToRackMap,
                onNavigateToUsers = onNavigateToUsers,
                onNavigateToSizeTable = onNavigateToSizeTable,
                onNavigateToBoxing = onNavigateToBoxing,
                onNavigateToSync = onNavigateToSync,
                onLogout = onLogout,
                themePreferences = themePreferences,
                currentUser = currentUser
            )
        }
    } else {
        // Inline mode (bottom nav tab) — no Scaffold, just content with header
        Column(modifier = Modifier.fillMaxSize()) {
            // Inline header matching HTML
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    "Configuración",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Panel de Administración",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            SettingsContent(
                onNavigateToCatalog = onNavigateToCatalog,
                onNavigateToPrinter = onNavigateToPrinter,
                onNavigateToQRHistory = onNavigateToQRHistory,
                onNavigateToRackMap = onNavigateToRackMap,
                onNavigateToUsers = onNavigateToUsers,
                onNavigateToSizeTable = onNavigateToSizeTable,
                onNavigateToBoxing = onNavigateToBoxing,
                onNavigateToSync = onNavigateToSync,
                onLogout = onLogout,
                themePreferences = themePreferences,
                currentUser = currentUser
            )
        }
    }
}

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onNavigateToCatalog: () -> Unit,
    onNavigateToPrinter: () -> Unit,
    onNavigateToQRHistory: () -> Unit,
    onNavigateToRackMap: () -> Unit,
    onNavigateToUsers: () -> Unit = {},
    onNavigateToSizeTable: () -> Unit = {},
    onNavigateToBoxing: () -> Unit = {},
    onNavigateToSync: () -> Unit = {},
    onLogout: () -> Unit,
    themePreferences: ThemePreferences? = null,
    currentUser: UserEntity? = null
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Secciones: Apariencia
        item {
            SettingsSection(title = "Apariencia") {
                if (themePreferences != null) {
                    val isDarkMode by themePreferences.isDarkMode.collectAsState()
                    ThemeToggleItem(
                        isDarkMode = isDarkMode,
                        onToggle = { themePreferences.setDarkMode(it) }
                    )
                }
            }
        }

        // Secciones: Catálogo
        item {
            SettingsSection(title = "Catálogo de Productos") {
                SettingsItem(
                    icon = Icons.Default.Category,
                    iconColor = Color(0xFFFFC107), // "gold"
                    label = "Gestión de Modelos",
                    desc = "Fotos, nombres, SKUs y tallas",
                    onClick = onNavigateToCatalog
                )
                SettingsItem(
                    icon = Icons.Default.Straighten,
                    iconColor = RefillBlue,
                    label = "Tabla de Tallas",
                    desc = "Rangos por modelo y conversiones",
                    onClick = onNavigateToSizeTable
                )
                SettingsItem(
                    icon = Icons.Default.Inventory,
                    iconColor = DispatchGreen,
                    label = "Pares por Caja Master",
                    desc = "Configuración de auto-boxing",
                    onClick = onNavigateToBoxing
                )
            }
        }

        // Secciones: Sistema
        item {
            SettingsSection(title = "Sistema") {
                SettingsItem(
                    icon = Icons.Default.Print,
                    iconColor = QualityPurple,
                    label = "Impresora QR",
                    desc = "BarTender Enterprise · TSC TE200",
                    onClick = onNavigateToPrinter
                )
                SettingsItem(
                    icon = Icons.Default.QrCode,
                    iconColor = Color(0xFF42A5F5),
                    label = "Historial de QR",
                    desc = "Búsqueda UUID · Reimpresión · PDF",
                    onClick = onNavigateToQRHistory
                )
                SettingsItem(
                    icon = Icons.Default.GridView,
                    iconColor = DispatchGreen,
                    label = "Mapa de Racks",
                    desc = "Visualización de ocupación del almacén",
                    onClick = onNavigateToRackMap
                )
                SettingsItem(
                    icon = Icons.Default.CloudSync,
                    iconColor = RefillBlue,
                    label = "Sincronización",
                    desc = "Google Sheets · Última: hace 5 min",
                    onClick = onNavigateToSync
                )
            }
        }

        // Secciones: Cuenta
        item {
            SettingsSection(title = "Cuenta") {
                // User Management — only visible for ADMIN role
                if (currentUser?.role == "ADMIN") {
                    SettingsItem(
                        icon = Icons.Default.People,
                        iconColor = Color(0xFF5B5BFF),
                        label = "Gestión de Usuarios",
                        desc = "Crear, ver y eliminar usuarios del sistema",
                        onClick = onNavigateToUsers
                    )
                }
                SettingsItem(
                    icon = Icons.Default.Lock,
                    iconColor = CriticalRed,
                    label = "Cerrar Sesión",
                    desc = currentUser?.let { "${it.name} (${it.role})" } ?: "admin@punteradigital.com",
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        // Version info footer
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Puntera Digital v3.1.0", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 11.sp)
                Text("Kinetic Architect Engine", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = CriticalRed,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro que deseas cerrar sesión? Tendrás que ingresar tu PIN nuevamente.") },
            confirmButton = {
                KineticButton(
                    text = "🚪 CERRAR SESIÓN",
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    type = ButtonType.DANGER
                )
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}

@Composable
fun ThemeToggleItem(
    isDarkMode: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!isDarkMode) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isDarkMode) Color(0xFF5B5BFF).copy(alpha = 0.15f) else StandByAmber.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = null,
                tint = if (isDarkMode) Color(0xFF9B9BFF) else StandByAmber,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isDarkMode) "Modo Oscuro" else "Modo Claro",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                if (isDarkMode) "Optimizado para entornos de almacén" else "Alto contraste para exteriores",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
        Switch(
            checked = isDarkMode,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold
        )
        KineticCard(padding = 0.dp) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    desc: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = iconColor.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.padding(10.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}
