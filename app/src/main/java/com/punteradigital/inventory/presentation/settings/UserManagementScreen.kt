package com.punteradigital.inventory.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.data.local.entity.UserEntity
import com.punteradigital.inventory.presentation.components.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    viewModel: InventoryViewModel,
    onBack: () -> Unit
) {
    val users by viewModel.allUsers.collectAsState(initial = emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<UserEntity?>(null) }

    // Feedback snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (uiState) {
            is InventoryUiState.SuccessMovement -> {
                snackbarHostState.showSnackbar((uiState as InventoryUiState.SuccessMovement).message)
                viewModel.resetUiState()
            }
            is InventoryUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as InventoryUiState.Error).message)
                viewModel.resetUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Gestión de Usuarios", fontWeight = FontWeight.Bold)
                        Text(
                            "Solo Administradores",
                            style = MaterialTheme.typography.bodySmall,
                            color = CriticalRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.PersonAdd, null)
                Spacer(Modifier.width(8.dp))
                Text("CREAR USUARIO", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val adminCount = users.count { it.role == "ADMIN" }
                    val operatorCount = users.count { it.role == "OPERADOR" }

                    // Admin count card
                    KineticCard(modifier = Modifier.weight(1f), padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CriticalRed.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🛡", fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "$adminCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                                Text(
                                    "Admins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Operator count card
                    KineticCard(modifier = Modifier.weight(1f), padding = 16.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = RefillBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👷", fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "$operatorCount",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = SpaceGrotesk
                                )
                                Text(
                                    "Operadores",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Section header
            item {
                Text(
                    "USUARIOS REGISTRADOS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }

            // User list
            if (users.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("👤", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No hay usuarios registrados",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(users, key = { it.id }) { user ->
                    UserCard(
                        user = user,
                        isCurrentUser = currentUser?.id == user.id,
                        onDelete = { showDeleteDialog = user }
                    )
                }
            }

            // Bottom padding for FAB
            item { Spacer(Modifier.height(80.dp)) }
        }

        // ═══ CREATE USER DIALOG ═══
        if (showCreateDialog) {
            CreateUserDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, pin, role ->
                    viewModel.createUser(name, pin, role)
                    showCreateDialog = false
                }
            )
        }

        // ═══ DELETE USER DIALOG ═══
        if (showDeleteDialog != null) {
            val userToDelete = showDeleteDialog!!
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                icon = {
                    Icon(
                        Icons.Default.PersonRemove,
                        null,
                        tint = CriticalRed,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text("Eliminar Usuario", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("¿Estás seguro que deseas eliminar a este usuario?")
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CriticalRed.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (userToDelete.role == "ADMIN") "🛡" else "👷",
                                    fontSize = 20.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        userToDelete.name,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        userToDelete.role,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Text(
                            "Esta acción no se puede deshacer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CriticalRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    KineticButton(
                        text = "🗑 ELIMINAR",
                        onClick = {
                            viewModel.deleteUser(userToDelete.id)
                            showDeleteDialog = null
                        },
                        type = ButtonType.DANGER
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("CANCELAR")
                    }
                }
            )
        }
    }
}

@Composable
private fun UserCard(
    user: UserEntity,
    isCurrentUser: Boolean,
    onDelete: () -> Unit
) {
    val isAdmin = user.role == "ADMIN"
    val roleColor = if (isAdmin) CriticalRed else RefillBlue
    val roleIcon = if (isAdmin) "🛡" else "👷"
    val roleLabel = if (isAdmin) "Administrador" else "Operador"

    KineticCard(padding = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = roleColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(roleIcon, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        user.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DispatchGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "TÚ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = DispatchGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    roleLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = roleColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "PIN: ●●●●  ·  ID: ${user.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Delete button (not for self)
            if (!isCurrentUser) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Eliminar",
                        tint = CriticalRed.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, pin: String, role: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("OPERADOR") }
    var pinVisible by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && pin.length == 4

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.PersonAdd,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text("Crear Nuevo Usuario", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // PIN
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("PIN (4 dígitos)") },
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null
                            )
                        }
                    },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Text(
                            "${pin.length}/4 dígitos",
                            color = if (pin.length == 4) DispatchGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                // Role selector
                Text(
                    "Rol",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // OPERADOR chip
                    FilterChip(
                        selected = selectedRole == "OPERADOR",
                        onClick = { selectedRole = "OPERADOR" },
                        label = { Text("👷 Operador", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RefillBlue.copy(alpha = 0.15f),
                            selectedLabelColor = RefillBlue
                        )
                    )
                    // ADMIN chip
                    FilterChip(
                        selected = selectedRole == "ADMIN",
                        onClick = { selectedRole = "ADMIN" },
                        label = { Text("🛡 Admin", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CriticalRed.copy(alpha = 0.15f),
                            selectedLabelColor = CriticalRed
                        )
                    )
                }

                if (selectedRole == "ADMIN") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StandByAmber.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ Los administradores pueden crear y eliminar usuarios y acceder a todas las configuraciones.",
                            style = MaterialTheme.typography.bodySmall,
                            color = StandByAmber,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            KineticButton(
                text = "✅ CREAR USUARIO",
                onClick = { onCreate(name, pin, selectedRole) },
                enabled = isValid,
                type = ButtonType.PRIMARY
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}
