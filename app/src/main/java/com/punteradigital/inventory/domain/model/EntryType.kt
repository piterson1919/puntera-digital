package com.punteradigital.inventory.domain.model

/**
 * Type of inventory entry.
 */
enum class EntryType(val displayName: String) {
    PRODUCCION("Producción"),
    AJUSTE("Ajuste"),
    TRASLADO("Traslado");
}
