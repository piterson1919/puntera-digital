package com.punteradigital.inventory.presentation.viewmodel

import com.punteradigital.inventory.data.remote.InventoryMovementDto
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import java.util.Date

fun InventoryViewModel.hashPin(pin: String): String {
    val salted = "punteradigital_v1_salt:${pin}"
    return java.security.MessageDigest.getInstance("SHA-256")
        .digest(salted.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

suspend fun InventoryViewModel.updateMasterBoxCompleteness(parentUuid: String) {
    val children = dao.getChildrenOfMasterBox(parentUuid)
    val activeCount = children.count { it.status == "AVAILABLE" || it.status == "STB" }
    val masterBox = dao.getMasterBoxByUuid(parentUuid) ?: return
    val result = BusinessRules.calculateBoxCompleteness(
        masterBox.childCount, masterBox.activeChildCount, 
        masterBox.activeChildCount - activeCount
    )
    dao.updateMasterBoxChildCount(parentUuid, result.activeCount, result.isComplete)
}

fun InventoryViewModel.buildMovementDto(
    uuid: String, type: String, reason: String,
    model: String, size: String, lot: String,
    origin: Origin, timestamp: Long, userId: String
): InventoryMovementDto {
    return InventoryMovementDto(
        date = getDateFormat().format(Date(timestamp)),
        time = getTimeFormat().format(Date(timestamp)),
        userId = userId, type = type, model = model,
        size = size, lot = lot, uuid = uuid,
        origin = origin.name, status = "AVAILABLE", reason = reason
    )
}
