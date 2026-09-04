package com.punteradigital.inventory.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.punteradigital.inventory.ui.theme.*

// -----------------------------------------------------
// KINETIC CLICK - Spring-based scale & ripple feedback
// -----------------------------------------------------
@Composable
fun Modifier.kineticClick(
    enabled: Boolean = true,
    scaleOnPress: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleOnPress else 1.0f,
        animationSpec = tween(
            durationMillis = 90,
            easing = FastOutSlowInEasing
        ),
        label = "kineticClickScale"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

// -----------------------------------------------------
// KINETIC CARD - Glassmorphism Base Container
// -----------------------------------------------------
@Composable
fun KineticCard(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp, // --space-2xl
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp)) // --radius-2xl
        .background(MaterialTheme.colorScheme.surfaceVariant) // --surface-container-high

    val finalModifier = if (onClick != null) {
        baseModifier.kineticClick(onClick = onClick)
    } else {
        baseModifier
    }

    Column(
        modifier = finalModifier.padding(padding),
        content = content
    )
}

// -----------------------------------------------------
// KINETIC POKA-YOKE BUTTON
// -----------------------------------------------------
enum class ButtonType {
    PRIMARY, SUCCESS, WARNING, DANGER, SECONDARY
}

@Composable
fun KineticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    type: ButtonType = ButtonType.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    successText: String = "✅ ¡REGISTRADO CON ÉXITO!",
    icon: @Composable (() -> Unit)? = null
) {
    var localSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            localSuccess = true
            kotlinx.coroutines.delay(2000)
            localSuccess = false
        }
    }

    val activeType = if (localSuccess) ButtonType.SUCCESS else type
    val displayText = if (localSuccess) successText else if (isLoading) "PROCESANDO..." else text

    // Use theme-aware colors for PRIMARY so it works in both dark and light modes
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val gradientColors = when (activeType) {
        ButtonType.PRIMARY -> listOf(primary, primaryContainer)
        ButtonType.SUCCESS -> listOf(KineticSuccess, Color(0xFF66D97A))
        ButtonType.WARNING -> listOf(KineticWarning, Color(0xFFFFB74D))
        ButtonType.DANGER -> listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
        ButtonType.SECONDARY -> listOf(RefillBlue, Color(0xFF64B5F6))
    }
    
    val textColor = when (activeType) {
        ButtonType.PRIMARY -> onPrimaryContainer
        ButtonType.DANGER -> MaterialTheme.colorScheme.error
        else -> Color.White
    }
    
    val shadowColor = when (activeType) {
        ButtonType.PRIMARY -> primaryContainer.copy(alpha = 0.25f)
        ButtonType.SUCCESS -> KineticSuccess.copy(alpha = 0.25f)
        ButtonType.WARNING -> KineticWarning.copy(alpha = 0.25f)
        else -> Color.Transparent
    }

    val shadowModifier = if (shadowColor != Color.Transparent && enabled) {
        Modifier.shadow(
            elevation = 16.dp, // Ambient shadow --shadow-ambient
            shape = RoundedCornerShape(8.dp),
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
    } else {
        Modifier
    }

    val backgroundBrush = if (enabled) {
        Brush.linearGradient(colors = gradientColors)
    } else {
        Brush.linearGradient(colors = listOf(Color.Gray.copy(0.3f), Color.DarkGray.copy(0.3f)))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(shadowModifier)
            .clip(RoundedCornerShape(8.dp)) // --radius-md
            .background(backgroundBrush)
            .kineticClick(enabled = enabled, onClick = onClick)
            .padding(vertical = 18.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = displayText.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = if (enabled) textColor else Color.LightGray
            )
        }
    }
}

// -----------------------------------------------------
// KINETIC TEXT FIELD - "Carved" Input Layer
// -----------------------------------------------------
@Composable
fun KineticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(12.dp), // --radius-lg
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest, // The carved visual depth
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}
