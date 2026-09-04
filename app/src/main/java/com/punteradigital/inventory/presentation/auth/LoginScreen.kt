package com.punteradigital.inventory.presentation.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.data.local.entity.UserEntity
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: InventoryViewModel,
    onLoginSuccess: (UserEntity) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // Seed default users on first launch
    LaunchedEffect(Unit) {
        viewModel.seedDefaultAdminIfNeeded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(FootSafeBlack, FootSafeSurface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top spacer to push card toward center
            Spacer(Modifier.height((screenHeight * 0.15f).coerceAtLeast(40.dp)))

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FootSafeSurfaceHigh)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = FootSafeYellow.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = FootSafeYellow,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = "Puntera Digital",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = FootSafeYellow
                    )
                    Text(
                        text = "Industria 5.0 · Trazabilidad Total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = FootSafeOnSurface.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4) { pin = it; error = null } },
                        label = { Text("PIN de 4 dígitos", color = FootSafeOnSurface.copy(alpha = 0.6f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = error != null,
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            letterSpacing = 8.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FootSafeYellow,
                            unfocusedBorderColor = FootSafeOutline,
                            cursorColor = FootSafeYellow,
                            focusedTextColor = FootSafeOnSurface,
                            unfocusedTextColor = FootSafeOnSurface
                        )
                    )

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = FootSafeError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            scope.launch {
                                val authenticatedUser = viewModel.authenticateByPin(pin)
                                isLoading = false
                                if (authenticatedUser != null) {
                                    onLoginSuccess(authenticatedUser)
                                } else {
                                    error = "PIN incorrecto — usuario no encontrado"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FootSafeYellow,
                            contentColor = FootSafeBlack
                        ),
                        enabled = pin.length == 4 && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = FootSafeBlack
                            )
                        } else {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ACCEDER", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Text(
                        text = "Foot Safe · Control de Inventario",
                        style = MaterialTheme.typography.labelSmall,
                        color = FootSafeOnSurface.copy(alpha = 0.3f)
                    )
                }
            }

            // Bottom spacer for scroll room when keyboard is visible
            Spacer(Modifier.height(200.dp))
        }
    }
}
