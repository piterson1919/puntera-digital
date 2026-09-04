package com.punteradigital.inventory.presentation.components

import android.content.res.Configuration
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.ui.theme.*

data class KineticNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val emoji: String
)

@Composable
fun KineticBottomNavBar(
    items: List<KineticNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    val surfaceHighColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceDimColor = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Determine if using a dark theme by checking background luminance
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.3f

    // Landscape detection for compact mode
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val barHeight = if (isLandscape) 52.dp else 72.dp
    val barCorner = if (isLandscape) 26.dp else 36.dp
    val bottomPadding = if (isLandscape) 8.dp else 20.dp
    val horizontalPadding = if (isLandscape) 32.dp else 16.dp
    val iconSize = if (isLandscape) 20.dp else 24.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ═══ GLOW LAYER (behind the bar) ═══
        // Animated glow that follows the selected tab
        val glowOffsetX by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
            label = "glowX"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight + 8.dp)
                .drawBehind {
                    val tabWidth = size.width / items.size
                    val centerX = tabWidth * glowOffsetX + tabWidth / 2
                    val glowRadius = tabWidth * 0.6f

                    // Radial-like glow under active tab
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = if (isDark) 0.35f else 0.20f),
                                primaryColor.copy(alpha = if (isDark) 0.08f else 0.04f),
                                Color.Transparent
                            ),
                            center = Offset(centerX, size.height * 0.3f),
                            radius = glowRadius
                        ),
                        cornerRadius = CornerRadius(42f, 42f),
                        size = size
                    )
                }
        )

        // ═══ MAIN BAR ═══
        // Animated bar colors for smooth dark↔light transition
        val barTopColor by animateColorAsState(
            if (isDark) Color(0xFF2A2A2E) else Color(0xFFF5F5F3),
            tween(180), label = "barTop"
        )
        val barMidColor by animateColorAsState(
            if (isDark) Color(0xFF1C1C1E) else Color(0xFFEDEDEB),
            tween(180), label = "barMid"
        )
        val barBotColor by animateColorAsState(
            if (isDark) Color(0xFF161618) else Color(0xFFE5E5E3),
            tween(180), label = "barBot"
        )
        val highlightAlpha = if (isDark) 0.08f else 0.4f
        val highlightPeakAlpha = if (isDark) 0.12f else 0.6f
        val shadowAmbient = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.08f)
        val shadowSpot = if (isDark) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.04f)
        val inactiveIconColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .shadow(
                    elevation = if (isLandscape) 16.dp else 24.dp,
                    shape = RoundedCornerShape(barCorner),
                    ambientColor = shadowAmbient,
                    spotColor = shadowSpot
                )
                .clip(RoundedCornerShape(barCorner))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(barTopColor, barMidColor, barBotColor)
                    )
                )
                .drawBehind {
                    // Subtle top highlight line (glassmorphism edge)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                Color.White.copy(alpha = highlightAlpha),
                                Color.White.copy(alpha = highlightPeakAlpha),
                                Color.White.copy(alpha = highlightAlpha),
                                Color.White.copy(alpha = 0f)
                            )
                        ),
                        cornerRadius = CornerRadius(barCorner.value, barCorner.value),
                        size = Size(size.width, 1.5f)
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index

                // Consolidate all per-tab animations into a single Transition.
                // Previously 5 separate animateXxxAsState calls = 5 independent snapshot
                // subscriptions per tab = 25 total. Now: 1 Transition per tab = 5 total.
                val transition = updateTransition(targetState = isSelected, label = "tab_$index")

                val scale by transition.animateFloat(
                    transitionSpec = { tween(120, easing = FastOutSlowInEasing) },
                    label = "scale_$index"
                ) { selected ->
                    if (selected) { if (isLandscape) 1.06f else 1.1f }
                    else { if (isLandscape) 0.96f else 0.92f }
                }

                val iconAlpha by transition.animateFloat(
                    transitionSpec = { tween(140) },
                    label = "alpha_$index"
                ) { selected -> if (selected) 1f else 0.45f }

                val labelAlpha by transition.animateFloat(
                    transitionSpec = { tween(120) },
                    label = "labelAlpha_$index"
                ) { selected -> if (selected) 1f else 0f }

                val iconColor by transition.animateColor(
                    transitionSpec = { tween(160) },
                    label = "color_$index"
                ) { selected -> if (selected) primaryColor else inactiveIconColor }

                val verticalOffset by transition.animateFloat(
                    transitionSpec = { tween(120, easing = FastOutSlowInEasing) },
                    label = "offset_$index"
                ) { selected ->
                    if (selected) { if (isLandscape) -2f else -4f }
                    else 0f
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = verticalOffset
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        // Active indicator dot
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(if (isLandscape) 3.dp else 4.dp)
                                    .background(primaryColor, CircleShape)
                                    .graphicsLayer { alpha = iconAlpha }
                            )
                            Spacer(Modifier.height(if (isLandscape) 2.dp else 4.dp))
                        }

                        // Icon
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            tint = iconColor,
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer { alpha = iconAlpha }
                        )

                        // Label (only visible when selected)
                        if (isSelected) {
                            Spacer(Modifier.height(if (isLandscape) 1.dp else 2.dp))
                            Text(
                                text = item.label,
                                fontSize = if (isLandscape) 8.sp else 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                fontFamily = SpaceGrotesk,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.graphicsLayer { alpha = labelAlpha }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Extension to estimate luminance
private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
