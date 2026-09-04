package com.punteradigital.inventory.domain.rules

import com.punteradigital.inventory.data.local.entity.MasterBoxEntity
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.domain.model.Origin

/**
 * Poka-Yoke Business Rules Engine — Industry 5.0.
 * Prevents human errors through systematic validation.
 */
object BusinessRules {

    /** Default units per Master Box, configurable per batch */
    const val DEFAULT_MASTER_QTY = 8

    /**
     * POKA-YOKE #1: Origin Mixing Block
     * Cannot finalize a dispatch batch if it contains UUIDs from different origins.
     */
    fun validateNoMixedOrigins(products: List<ProductEntity>): ValidationResult {
        if (products.isEmpty()) return ValidationResult(true, null)
        
        val origins = products.map { it.origin }.distinct()
        return if (origins.size > 1) {
            val footSafeCount = products.count { it.origin == Origin.FOOT_SAFE.name }
            val safetyCount = products.count { it.origin == Origin.SAFETY.name }
            ValidationResult(
                isValid = false,
                errorMessage = "⚠ ALERTA CRÍTICA: Mezcla de orígenes detectada.\n" +
                    "Foot Safe: $footSafeCount unidades\n" +
                    "Safety: $safetyCount unidades\n" +
                    "No se puede despachar un lote con productos de empresas distintas."
            )
        } else {
            ValidationResult(true, null)
        }
    }

    /**
     * POKA-YOKE #2: Master Box Completeness
     * If a child is removed from a Master Box, flag the box as incomplete.
     */
    fun calculateBoxCompleteness(
        totalChildren: Int,
        currentActiveChildren: Int,
        removedCount: Int = 1
    ): BoxCompletenessResult {
        val newActive = maxOf(0, currentActiveChildren - removedCount)
        return BoxCompletenessResult(
            activeCount = newActive,
            totalCount = totalChildren,
            isComplete = newActive == totalChildren,
            message = if (newActive < totalChildren) {
                "Caja Master incompleta: $newActive/$totalChildren unidades restantes"
            } else null
        )
    }

    /**
     * Validates that a product can transition to the target status.
     * Prevents invalid state transitions (e.g., DISPATCHED → AVAILABLE).
     *
     * POKA-YOKE: Despacho solo desde STB (debe pasar por Stand-By primero).
     */
    fun validateStatusTransition(currentStatus: String, targetStatus: String): ValidationResult {
        val validTransitions = mapOf(
            "AVAILABLE" to listOf("STB", "BAJA_HIDROLIZADO", "BAJA_SEGUNDA", "BAJA_DANO_FISICO", "MUESTRA"),
            "STB" to listOf("DISPATCHED", "AVAILABLE"), // Can return to rack or dispatch
            "MUESTRA" to listOf("AVAILABLE", "MUESTRA_VENDIDA"), // Return or sold
        )
        
        val allowed = validTransitions[currentStatus] ?: emptyList()
        return if (targetStatus in allowed) {
            ValidationResult(true, null)
        } else {
            val hint = when {
                currentStatus == "AVAILABLE" && targetStatus == "DISPATCHED" ->
                    "Debe pasar por Stand-By antes de despachar."
                else -> ""
            }
            ValidationResult(
                isValid = false,
                errorMessage = "Transición no permitida: $currentStatus → $targetStatus. $hint".trim()
            )
        }
    }

    /**
     * POKA-YOKE #3: Refill Compatibility
     * Validates that a loose product matches the master box's model and size.
     */
    fun validateRefillCompatibility(
        masterBox: MasterBoxEntity,
        product: ProductEntity
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (product.model != masterBox.model) {
            errors.add("Modelo distinto: producto '${product.model}' vs caja '${masterBox.model}'")
        }
        if (product.size != masterBox.size) {
            errors.add("Talla distinta: producto '${product.size}' vs caja '${masterBox.size}'")
        }
        if (product.origin != masterBox.origin) {
            errors.add("Origen distinto: producto '${product.origin}' vs caja '${masterBox.origin}'")
        }
        if (product.parentUuid != null) {
            errors.add("El producto ya pertenece a otra caja master: ${product.parentUuid}")
        }
        if (product.status != "AVAILABLE") {
            errors.add("El producto no está disponible. Estado actual: ${product.status}")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult(true, null)
        } else {
            ValidationResult(false, "⚠ Incompatibilidad:\n" + errors.joinToString("\n• ", prefix = "• "))
        }
    }

    /**
     * POKA-YOKE #4: Refill Capacity
     * Ensures we don't overfill a master box.
     */
    fun validateRefillCapacity(masterBox: MasterBoxEntity, addCount: Int): ValidationResult {
        val remaining = masterBox.childCount - masterBox.activeChildCount
        return if (addCount <= remaining) {
            ValidationResult(true, null)
        } else {
            ValidationResult(
                isValid = false,
                errorMessage = "La caja solo tiene espacio para $remaining par(es) más. Intentas agregar $addCount."
            )
        }
    }

    /**
     * Calculates auto-boxing breakdown for a total quantity entry.
     */
    fun calculateAutoBoxing(totalQuantity: Int, pairsPerBox: Int): AutoBoxResult {
        val fullBoxes = totalQuantity / pairsPerBox
        val remainder = totalQuantity % pairsPerBox
        return AutoBoxResult(
            fullBoxes = fullBoxes,
            pairsInFullBoxes = fullBoxes * pairsPerBox,
            remainderPairs = remainder,
            totalPairs = totalQuantity
        )
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )

    data class BoxCompletenessResult(
        val activeCount: Int,
        val totalCount: Int,
        val isComplete: Boolean,
        val message: String?
    )

    data class AutoBoxResult(
        val fullBoxes: Int,
        val pairsInFullBoxes: Int,
        val remainderPairs: Int,
        val totalPairs: Int
    ) {
        val hasRemainder: Boolean get() = remainderPairs > 0
    }
}
