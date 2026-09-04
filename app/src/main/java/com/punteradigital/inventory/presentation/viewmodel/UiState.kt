package com.punteradigital.inventory.presentation.viewmodel

import com.punteradigital.inventory.data.local.entity.LabelEntity
import com.punteradigital.inventory.data.local.entity.MasterBoxEntity
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.domain.model.Origin

/**
 * Remainder mode when total quantity doesn't divide evenly into boxes.
 */
enum class RemainderMode {
    /** Generate individual pairs without a master box parent */
    LOOSE,
    /** Create an incomplete master box marked as PENDIENTE_POR_RELLENAR */
    FILL_LATER
}

sealed class ScannedInfo {
    data class UnitInfo(val entity: ProductEntity) : ScannedInfo()
    data class Master(val entity: MasterBoxEntity) : ScannedInfo()
    data class Label(val entity: LabelEntity) : ScannedInfo()
}

sealed class InventoryUiState {
    object Idle : InventoryUiState()
    data class Loading(val message: String) : InventoryUiState()
    data class SuccessEntry(
        val uuids: List<String>,
        val model: String,
        val lot: String,
        val size: String,
        val origin: Origin,
        val message: String = "",
        val warning: String? = null
    ) : InventoryUiState()
    data class SuccessMovement(val message: String) : InventoryUiState()
    data class SuccessSync(val message: String) : InventoryUiState()
    data class SuccessRefill(
        val parentUuid: String,
        val addedCount: Int,
        val totalActive: Int,
        val totalCapacity: Int
    ) : InventoryUiState()
    data class PokayokeAlert(val message: String) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
    data class SmartEntrySuggestion(
        val compatibleBoxes: List<MasterBoxEntity>,
        val requestedQuantity: Int,
        val origin: Origin,
        val model: String,
        val size: String,
        val lot: String,
        val entryType: String,
        val isMasterBox: Boolean,
        val childCount: Int,
        val remainderMode: RemainderMode,
        val userId: String
    ) : InventoryUiState()
}
