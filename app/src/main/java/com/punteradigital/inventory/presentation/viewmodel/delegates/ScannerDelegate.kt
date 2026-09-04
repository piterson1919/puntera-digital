package com.punteradigital.inventory.presentation.viewmodel

import android.util.Log
import com.punteradigital.inventory.data.local.entity.LabelEntity
import com.punteradigital.inventory.data.local.entity.MasterBoxEntity
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.ScannedInfo

suspend fun InventoryViewModel.getScannedInfo(uuid: String): ScannedInfo? {
    Log.d("InventoryVM", "getScannedInfo lookup: '$uuid'")

    val masterBox = dao.getMasterBoxByUuid(uuid)
    if (masterBox != null) {
        Log.d("InventoryVM", "Found as MasterBox: ${masterBox.uuid} (${masterBox.model} T.${masterBox.size})")
        return ScannedInfo.Master(masterBox)
    }

    val product = dao.getProductByUuid(uuid)
    if (product != null) {
        Log.d("InventoryVM", "Found as Product: ${product.uuid} (${product.model} T.${product.size} status=${product.status})")
        return ScannedInfo.UnitInfo(product)
    }

    val label = dao.getLabelByUuid(uuid)
    if (label != null) {
        Log.d("InventoryVM", "Found as Label: ${label.uuid} (${label.model} T.${label.size} status=${label.status})")
        return ScannedInfo.Label(label)
    }

    val trimmedUuid = uuid.trim()
    if (trimmedUuid != uuid) {
        Log.d("InventoryVM", "Retrying with trimmed UUID: '$trimmedUuid'")
        val trimmedProduct = dao.getProductByUuid(trimmedUuid)
        if (trimmedProduct != null) return ScannedInfo.UnitInfo(trimmedProduct)
        
        val trimmedBox = dao.getMasterBoxByUuid(trimmedUuid)
        if (trimmedBox != null) return ScannedInfo.Master(trimmedBox)
        
        val trimmedLabel = dao.getLabelByUuid(trimmedUuid)
        if (trimmedLabel != null) return ScannedInfo.Label(trimmedLabel)
    }

    Log.w("InventoryVM", "UUID NOT FOUND in database: '$uuid' (length=${uuid.length})")
    return null
}
