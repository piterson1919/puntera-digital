package com.punteradigital.inventory.domain.model

/**
 * Product lifecycle status following warehouse flow:
 * AVAILABLE → STB (Stand-By / Pre-despacho) → DISPATCHED
 *           → MUESTRA (Custodia Comercial) → AVAILABLE (retorno)
 *                                           → MUESTRA_VENDIDA
 *                                           → BAJA_*
 */
enum class ProductStatus(val displayName: String) {
    AVAILABLE("Disponible"),
    STB("Stand-By (Pre-despacho)"),
    DISPATCHED("Despachado"),
    MUESTRA("Muestra Retornable"),
    MUESTRA_VENDIDA("Muestra - Cliente se quedó"),
    BAJA_HIDROLIZADO("Baja - Hidrolizado"),
    BAJA_SEGUNDA("Baja - Segunda"),
    BAJA_DANO_FISICO("Baja - Daño Físico");

    fun isBaja(): Boolean = name.startsWith("BAJA_")
    fun isMuestra(): Boolean = name.startsWith("MUESTRA")
}
