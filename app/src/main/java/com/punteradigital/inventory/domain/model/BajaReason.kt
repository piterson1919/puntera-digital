package com.punteradigital.inventory.domain.model

/**
 * Reasons for quality rejection (Baja/Calidad module).
 */
enum class BajaReason(val displayName: String) {
    HIDROLIZADO("Hidrolizado"),
    SEGUNDA("Segunda"),
    DANO_FISICO("Daño Físico");

    fun toProductStatus(): ProductStatus = when (this) {
        HIDROLIZADO -> ProductStatus.BAJA_HIDROLIZADO
        SEGUNDA     -> ProductStatus.BAJA_SEGUNDA
        DANO_FISICO -> ProductStatus.BAJA_DANO_FISICO
    }
}
