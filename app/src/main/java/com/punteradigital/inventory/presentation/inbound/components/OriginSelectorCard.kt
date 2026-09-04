package com.punteradigital.inventory.presentation.inbound.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.ui.theme.FootSafeBlack
import com.punteradigital.inventory.ui.theme.FootSafeYellow
import com.punteradigital.inventory.ui.theme.SafetyCobalt
import com.punteradigital.inventory.ui.theme.SafetyWhite

@Composable
fun OriginSelectorCard(
    selectedOrigin: Origin,
    onOriginSelected: (Origin) -> Unit
) {
    KineticCard(padding = 20.dp) {
        Column {
            Text(
                "Origen de Mercancía",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OriginChip(
                    label = "Foot Safe",
                    isSelected = selectedOrigin == Origin.FOOT_SAFE,
                    selectedColor = FootSafeYellow,
                    textColor = FootSafeBlack,
                    onClick = { onOriginSelected(Origin.FOOT_SAFE) },
                    modifier = Modifier.weight(1f)
                )
                OriginChip(
                    label = "Safety",
                    isSelected = selectedOrigin == Origin.SAFETY,
                    selectedColor = SafetyCobalt,
                    textColor = SafetyWhite,
                    onClick = { onOriginSelected(Origin.SAFETY) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OriginChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) selectedColor else MaterialTheme.colorScheme.surface,
        onClick = onClick,
        tonalElevation = if (isSelected) 0.dp else 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) textColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
