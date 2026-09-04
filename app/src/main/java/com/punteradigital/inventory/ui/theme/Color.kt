package com.punteradigital.inventory.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════
// KINETIC ARCHITECT DESIGN SYSTEM — Tonal Palette
// ═══════════════════════════════════════════════════════════════

// Tonal Surface Hierarchy
val KineticSurface = Color(0xFF131315)
val KineticSurfaceDim = Color(0xFF0E0E10)
val KineticSurfaceContainerLowest = Color(0xFF1A1A1C)
val KineticSurfaceContainerLow = Color(0xFF1B1B1D)
val KineticSurfaceContainer = Color(0xFF252527)
val KineticSurfaceContainerHigh = Color(0xFF2E2E30)
val KineticSurfaceContainerHighest = Color(0xFF353437)

// Kinetic Primary (Safety Yellow)
val KineticPrimary = Color(0xFFFFEFC7)
val KineticPrimaryContainer = Color(0xFFF2D16B)
val KineticOnPrimary = Color(0xFF3C2F00)
val KineticOnPrimaryContainer = Color(0xFF1C1600)

// Kinetic Secondary (Cobalt)
val KineticSecondary = Color(0xFF5B9BFF)
val KineticSecondaryContainer = Color(0xFF0148AB)

// Text & Outline
val KineticOnSurface = Color(0xFFE6E1DC)
val KineticOnSurfaceVariant = Color(0xFFCAC5BE)
val KineticOutline = Color(0xFF48484A)
val KineticOutlineVariant = Color(0xFF4C4637)

// Feedback & Validation
val KineticError = Color(0xFFFFB4AB)
val KineticWarning = Color(0xFFFF9500)
val KineticSuccess = Color(0xFF30D158)

// Functional (Roles / Modules)
val DispatchGreen = Color(0xFF34C759)
val QualityPurple = Color(0xFFAF52DE)
val MuestraTeal = Color(0xFF009688)
val RefillBlue = Color(0xFF2196F3)
val StandByAmber = Color(0xFFFFCC02)

// ═══════════════════════════════════════════════════════════════
// KINETIC ARCHITECT DESIGN SYSTEM — Light Palette
// ═══════════════════════════════════════════════════════════════

// Light Tonal Surface Hierarchy
val KineticLightSurface = Color(0xFFFAFAF8)
val KineticLightSurfaceDim = Color(0xFFF2F2EF)
val KineticLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val KineticLightSurfaceContainerLow = Color(0xFFF7F7F5)
val KineticLightSurfaceContainer = Color(0xFFEFEFEC)
val KineticLightSurfaceContainerHigh = Color(0xFFE8E8E5)
val KineticLightSurfaceContainerHighest = Color(0xFFE0E0DD)

// Light Primary (Safety Yellow — darkened for contrast on white)
val KineticLightPrimary = Color(0xFF6D5E00)
val KineticLightPrimaryContainer = Color(0xFFF5E07A)
val KineticLightOnPrimary = Color(0xFFFFFFFF)
val KineticLightOnPrimaryContainer = Color(0xFF221B00)

// Light Secondary (Cobalt — darkened)
val KineticLightSecondary = Color(0xFF1960C0)
val KineticLightSecondaryContainer = Color(0xFFD4E3FF)

// Light Text & Outline
val KineticLightOnSurface = Color(0xFF1C1B1F)
val KineticLightOnSurfaceVariant = Color(0xFF49454F)
val KineticLightOutline = Color(0xFFC4C4C0)
val KineticLightOutlineVariant = Color(0xFFD6D1C7)

// ==========================================
// MIGRATION COMPATIBILITY CHIP ALIASES 
// ==========================================
val FootSafeBlack = KineticSurfaceDim
val FootSafeYellow = KineticPrimaryContainer
val FootSafeYellowDark = KineticPrimary
val FootSafeSurface = KineticSurface
val FootSafeSurfaceHigh = KineticSurfaceContainerHigh
val FootSafeOnSurface = KineticOnSurface
val FootSafeOutline = KineticOutline
val FootSafeError = KineticError
val FootSafeSuccess = KineticSuccess

val SafetyCobalt = KineticSecondary
val SafetyCobaltDark = KineticSecondaryContainer
val SafetyCobaltLight = KineticSecondary
val SafetyWhite = KineticOnSurface
val SafetySurface = KineticSurfaceDim
val SafetySurfaceHigh = KineticSurfaceContainerHigh
val SafetyOnSurface = KineticOnSurface
val SafetyOutline = KineticOutline
val SafetyError = KineticError
val SafetySuccess = KineticSuccess

val CriticalRed = KineticError
val WarningOrange = KineticWarning
