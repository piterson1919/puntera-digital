package com.punteradigital.inventory.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.R
import com.punteradigital.inventory.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalEditPanelBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ═══ HEADER ═══
            Column {
                Text(
                    text = "⚡ Opciones de App",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceGrotesk
                )
                Text(
                    text = "Panel de Edición Rápida",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // ═══ APARIENCIA ═══
            EditSection(title = "Apariencia") {
                EditOptionRow(
                    icon = "🎨", iconColor = Color(0xFFF2D16B),
                    title = "Tema de Color", subtitle = "Carbon Black Industrial (Activo)"
                )
                EditOptionRow(
                    icon = "🔤", iconColor = Color(0xFF2196F3),
                    title = "Tipografía", subtitle = "Space Grotesk + Inter"
                )
                EditOptionRow(
                    icon = "📐", iconColor = Color(0xFFAF52DE),
                    title = "Densidad de Layout", subtitle = "Comfortable (para guantes)"
                )
            }

            // ═══ IMÁGENES DE PRODUCTO ═══
            EditSection(title = "Imágenes de Producto") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val products = listOf(
                        Triple(R.drawable.boot_black, "FS300CMFFPBL", "Foot Safe 300"),
                        Triple(R.drawable.boot_brown, "FS302CMN", "Foot Safe 302"),
                        Triple(R.drawable.boot_tactical, "FS400BK", "Foot Safe 400"),
                        Triple(R.drawable.shoe_lite, "SF200LT", "Safety 200 Lite")
                    )
                    items(products) { product ->
                        Surface(
                            modifier = Modifier
                                .width(120.dp)
                                .clickable { },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Color.White, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = product.first),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("✏️", fontSize = 10.sp)
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(product.second, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(product.third, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // ═══ FUNCIONALIDADES ═══
            EditSection(title = "Funcionalidades") {
                EditOptionRow(
                    icon = "📦", iconColor = Color(0xFF34C759),
                    title = "Módulos Activos", subtitle = "Entrada, Despacho, Calidad, Muestras"
                )
                EditOptionRow(
                    icon = "⚡", iconColor = Color(0xFFFFCC02),
                    title = "Modo Ráfaga", subtitle = "Próximamente"
                )
                EditOptionRow(
                    icon = "🔔", iconColor = Color(0xFFFF3B30),
                    title = "Alertas y Sonidos", subtitle = "Próximamente"
                )
            }

            // ═══ DATOS ═══
            EditSection(title = "Datos") {
                EditOptionRow(
                    icon = "☁️", iconColor = Color(0xFF2196F3),
                    title = "Sincronización", subtitle = "Próximamente"
                )
                EditOptionRow(
                    icon = "🖨️", iconColor = Color(0xFFAF52DE),
                    title = "Impresora QR", subtitle = "Próximamente"
                )
                EditOptionRow(
                    icon = "📊", iconColor = Color(0xFF009688),
                    title = "Exportar Reportes", subtitle = "Próximamente"
                )
            }

            // Footer
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Puntera Digital v3.0.0", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Text("Kinetic Architect Engine", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun EditSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun EditOptionRow(
    icon: String,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
