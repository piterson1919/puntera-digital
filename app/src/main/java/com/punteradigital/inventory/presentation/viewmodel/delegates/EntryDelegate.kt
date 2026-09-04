package com.punteradigital.inventory.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.punteradigital.inventory.data.local.entity.MasterBoxEntity
import com.punteradigital.inventory.data.local.entity.MovementEntity
import com.punteradigital.inventory.data.local.entity.ProductEntity
import com.punteradigital.inventory.data.remote.InventoryMovementDto
import com.punteradigital.inventory.data.remote.PrintLabelItem
import com.punteradigital.inventory.data.remote.SyncRequestDto
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import com.punteradigital.inventory.presentation.viewmodel.RemainderMode
import kotlinx.coroutines.launch
import java.util.Date

fun InventoryViewModel.processNewEntry(
    origin: Origin,
    model: String,
    size: String,
    lot: String,
    entryType: String,
    isMasterBox: Boolean,
    childCount: Int = BusinessRules.DEFAULT_MASTER_QTY,
    totalQuantity: Int = childCount,
    remainderMode: RemainderMode = RemainderMode.LOOSE,
    userId: String
) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Generando UUIDs y registrando..."))

        val timestamp = System.currentTimeMillis()
        val allUuids = mutableListOf<String>()
        val printItems = mutableListOf<PrintLabelItem>()
        val movementsDto = mutableListOf<InventoryMovementDto>()

        try {
            if (isMasterBox) {
                val autoBox = BusinessRules.calculateAutoBoxing(totalQuantity, childCount)
                var globalSeq = 1

                for (boxIdx in 1..autoBox.fullBoxes) {
                    val (parentUuid, childrenUuids) = uuidGenerator.generateMasterBoxBatch(
                        origin = origin, lot = lot, size = size,
                        childCount = childCount, boxSequence = boxIdx,
                        startSequence = globalSeq
                    )

                    dao.insertMasterBox(MasterBoxEntity(
                        uuid = parentUuid, origin = origin.name,
                        model = model, size = size, lot = lot,
                        childCount = childCount, activeChildCount = childCount,
                        isComplete = true, status = "COMPLETE", createdAt = timestamp
                    ))
                    allUuids.add(parentUuid)
                    printItems.add(PrintLabelItem(parentUuid, model, size, lot, origin.displayName))

                    val children = childrenUuids.map { uuid ->
                        ProductEntity(
                            uuid = uuid, parentUuid = parentUuid,
                            origin = origin.name, model = model, size = size,
                            lot = lot, entryType = entryType, status = "AVAILABLE",
                            location = "RACK", createdAt = timestamp, updatedAt = timestamp
                        )
                    }
                    dao.insertProducts(children)
                    allUuids.addAll(childrenUuids)
                    childrenUuids.forEach { uuid ->
                        printItems.add(PrintLabelItem(uuid, model, size, lot, origin.displayName))
                        syncInventoryChange(
                            entityType = "product",
                            action = "upsert",
                            payload = mapOf(
                                "uuid" to uuid,
                                "origin" to origin.name,
                                "model" to model,
                                "size" to size,
                                "lot" to lot,
                                "status" to "AVAILABLE",
                                "location" to "RACK",
                                "entryType" to entryType,
                                "parentUuid" to parentUuid
                            ),
                            userId = userId
                        )
                    }

                    dao.insertMovement(MovementEntity(
                        uuid = parentUuid, type = "IN", reason = entryType,
                        observation = "Caja Master completa con $childCount unidades",
                        location = "RACK", timestamp = timestamp, userId = userId
                    ))
                    movementsDto.add(buildMovementDto(parentUuid, "IN", entryType, model, size, lot, origin, timestamp, userId))
                    syncInventoryChange(
                        entityType = "movement",
                        action = "create",
                        payload = mapOf(
                            "uuid" to parentUuid,
                            "type" to "IN",
                            "reason" to entryType,
                            "model" to model,
                            "size" to size,
                            "lot" to lot,
                            "origin" to origin.name,
                            "location" to "RACK",
                            "userId" to userId,
                            "observation" to "Caja Master completa con $childCount unidades"
                        ),
                        userId = userId
                    )

                    globalSeq += childCount
                }

                if (autoBox.hasRemainder) {
                    when (remainderMode) {
                        RemainderMode.LOOSE -> {
                            val looseUuids = uuidGenerator.generateUnitBatch(
                                origin, lot, size, autoBox.remainderPairs, globalSeq
                            )
                            val loosePairs = looseUuids.map { uuid ->
                                ProductEntity(
                                    uuid = uuid, origin = origin.name, model = model,
                                    size = size, lot = lot, entryType = entryType,
                                    status = "AVAILABLE", location = "RACK",
                                    createdAt = timestamp, updatedAt = timestamp
                                )
                            }
                            dao.insertProducts(loosePairs)
                            allUuids.addAll(looseUuids)
                            looseUuids.forEach { uuid ->
                                printItems.add(PrintLabelItem(uuid, model, size, lot, origin.displayName))
                                dao.insertMovement(MovementEntity(
                                    uuid = uuid, type = "IN", reason = entryType,
                                    observation = "Par individual suelto",
                                    location = "RACK", timestamp = timestamp, userId = userId
                                ))
                                movementsDto.add(buildMovementDto(uuid, "IN", entryType, model, size, lot, origin, timestamp, userId))
                                syncInventoryChange(
                                    entityType = "product",
                                    action = "upsert",
                                    payload = mapOf(
                                        "uuid" to uuid,
                                        "origin" to origin.name,
                                        "model" to model,
                                        "size" to size,
                                        "lot" to lot,
                                        "status" to "AVAILABLE",
                                        "location" to "RACK",
                                        "entryType" to entryType,
                                        "parentUuid" to ""
                                    ),
                                    userId = userId
                                )
                                syncInventoryChange(
                                    entityType = "movement",
                                    action = "create",
                                    payload = mapOf(
                                        "uuid" to uuid,
                                        "type" to "IN",
                                        "reason" to entryType,
                                        "model" to model,
                                        "size" to size,
                                        "lot" to lot,
                                        "origin" to origin.name,
                                        "location" to "RACK",
                                        "userId" to userId,
                                        "observation" to "Par individual suelto"
                                    ),
                                    userId = userId
                                )
                            }
                        }
                        RemainderMode.FILL_LATER -> {
                            val boxSeq = autoBox.fullBoxes + 1
                            val (parentUuid, childrenUuids) = uuidGenerator.generateMasterBoxBatch(
                                origin = origin, lot = lot, size = size,
                                childCount = autoBox.remainderPairs, boxSequence = boxSeq,
                                startSequence = globalSeq
                            )

                            dao.insertMasterBox(MasterBoxEntity(
                                uuid = parentUuid, origin = origin.name,
                                model = model, size = size, lot = lot,
                                childCount = childCount,
                                activeChildCount = autoBox.remainderPairs,
                                isComplete = false,
                                status = "PENDIENTE_POR_RELLENAR",
                                createdAt = timestamp
                            ))
                            allUuids.add(parentUuid)
                            printItems.add(PrintLabelItem(parentUuid, model, size, lot, origin.displayName))

                            val children = childrenUuids.map { uuid ->
                                ProductEntity(
                                    uuid = uuid, parentUuid = parentUuid,
                                    origin = origin.name, model = model, size = size,
                                    lot = lot, entryType = entryType, status = "AVAILABLE",
                                    location = "RACK", createdAt = timestamp, updatedAt = timestamp
                                )
                            }
                            dao.insertProducts(children)
                            allUuids.addAll(childrenUuids)
                            childrenUuids.forEach { uuid ->
                                printItems.add(PrintLabelItem(uuid, model, size, lot, origin.displayName))
                                syncInventoryChange(
                                    entityType = "product",
                                    action = "upsert",
                                    payload = mapOf(
                                        "uuid" to uuid,
                                        "origin" to origin.name,
                                        "model" to model,
                                        "size" to size,
                                        "lot" to lot,
                                        "status" to "AVAILABLE",
                                        "location" to "RACK",
                                        "entryType" to entryType,
                                        "parentUuid" to parentUuid
                                    ),
                                    userId = userId
                                )
                            }

                            dao.insertMovement(MovementEntity(
                                uuid = parentUuid, type = "IN", reason = entryType,
                                observation = "Caja Master incompleta: ${autoBox.remainderPairs}/$childCount — Pendiente por rellenar",
                                location = "RACK", timestamp = timestamp, userId = userId
                            ))
                            movementsDto.add(buildMovementDto(parentUuid, "IN", entryType, model, size, lot, origin, timestamp, userId))
                            syncInventoryChange(
                                entityType = "movement",
                                action = "create",
                                payload = mapOf(
                                    "uuid" to parentUuid,
                                    "type" to "IN",
                                    "reason" to entryType,
                                    "model" to model,
                                    "size" to size,
                                    "lot" to lot,
                                    "origin" to origin.name,
                                    "location" to "RACK",
                                    "userId" to userId,
                                    "observation" to "Caja Master incompleta: ${autoBox.remainderPairs}/$childCount — Pendiente por rellenar"
                                ),
                                userId = userId
                            )
                        }
                    }
                }
            } else {
                val uuids = uuidGenerator.generateUnitBatch(origin, lot, size, totalQuantity, 1)
                val products = uuids.map { uuid ->
                    ProductEntity(
                        uuid = uuid, origin = origin.name, model = model,
                        size = size, lot = lot, entryType = entryType,
                        status = "AVAILABLE", location = "RACK",
                        createdAt = timestamp, updatedAt = timestamp
                    )
                }
                dao.insertProducts(products)
                allUuids.addAll(uuids)
                uuids.forEach { uuid ->
                    printItems.add(PrintLabelItem(uuid, model, size, lot, origin.displayName))
                    dao.insertMovement(MovementEntity(
                        uuid = uuid, type = "IN", reason = entryType,
                        observation = "Unidad individual", location = "RACK",
                        timestamp = timestamp, userId = userId
                    ))
                    movementsDto.add(buildMovementDto(uuid, "IN", entryType, model, size, lot, origin, timestamp, userId))
                    syncInventoryChange(
                        entityType = "product",
                        action = "upsert",
                        payload = mapOf(
                            "uuid" to uuid,
                            "origin" to origin.name,
                            "model" to model,
                            "size" to size,
                            "lot" to lot,
                            "status" to "AVAILABLE",
                            "location" to "RACK",
                            "entryType" to entryType,
                            "parentUuid" to ""
                        ),
                        userId = userId
                    )
                    syncInventoryChange(
                        entityType = "movement",
                        action = "create",
                        payload = mapOf(
                            "uuid" to uuid,
                            "type" to "IN",
                            "reason" to entryType,
                            "model" to model,
                            "size" to size,
                            "lot" to lot,
                            "origin" to origin.name,
                            "location" to "RACK",
                            "userId" to userId,
                            "observation" to "Unidad individual"
                        ),
                        userId = userId
                    )
                }
            }

            emitUiState(InventoryUiState.Loading("Enviando a impresora BarTender..."))
            val printSuccess = syncManager.sendPrintJobNow(printItems)
            if (!printSuccess) {
                syncManager.enqueuePrintJob(printItems)
            }

            emitUiState(InventoryUiState.Loading("Sincronizando con nube..."))
            val sheetsRequest = SyncRequestDto(
                title = "Entrada_${lot}_${getDateFormat().format(Date(timestamp))}",
                origin = origin.name, movements = movementsDto
            )
            val sheetsSuccess = syncManager.sendSheetsSyncNow(sheetsRequest)
            if (!sheetsSuccess) {
                syncManager.enqueueSheetsSync(sheetsRequest)
            }

            val warnings = mutableListOf<String>()
            if (!printSuccess) warnings.add("Impresión encolada: se enviará al restaurar conexión con BarTender.")
            if (!sheetsSuccess) warnings.add("Sincronización encolada: se enviará al restaurar conexión a internet.")

            val confirmationMessage = if (isMasterBox) {
                val boxSummary = BusinessRules.calculateAutoBoxing(totalQuantity, childCount)
                buildString {
                    append("${boxSummary.fullBoxes} Caja(s) Master")
                    if (boxSummary.hasRemainder) {
                        when (remainderMode) {
                            RemainderMode.LOOSE -> append(" y ${boxSummary.remainderPairs} unidad(es) sueltas")
                            RemainderMode.FILL_LATER -> append(" completas y 1 Incompleta (${boxSummary.remainderPairs}/$childCount)")
                        }
                    } else {
                        append(" completas")
                    }
                }
            } else {
                "$totalQuantity unidad(es) individuales sueltas"
            }

            emitUiState(InventoryUiState.SuccessEntry(
                uuids = allUuids, model = model, lot = lot, size = size,
                origin = origin,
                message = "Ingreso: $confirmationMessage",
                warning = if (warnings.isNotEmpty()) warnings.joinToString("\n") else null
            ))

        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al registrar: ${e.message}"))
            Log.e("InventoryVM", "processNewEntry failed", e)
        }
    }
}

fun InventoryViewModel.evaluateSmartEntry(
    origin: Origin, model: String, size: String, lot: String,
    entryType: String, totalQuantity: Int, isMasterBox: Boolean,
    childCount: Int, remainderMode: RemainderMode, userId: String
) {
    viewModelScope.launch {
        try {
            if (isMasterBox && totalQuantity >= childCount && totalQuantity % childCount == 0) {
                processNewEntry(origin, model, size, lot, entryType, isMasterBox, childCount, totalQuantity, remainderMode, userId)
                return@launch
            }

            val compatibleBoxes = dao.findCompatibleIncompleteBoxes(model, size, origin.name)

            if (compatibleBoxes.isNotEmpty()) {
                emitUiState(InventoryUiState.SmartEntrySuggestion(
                    compatibleBoxes = compatibleBoxes,
                    requestedQuantity = totalQuantity,
                    origin = origin, model = model, size = size,
                    lot = lot, entryType = entryType,
                    isMasterBox = isMasterBox, childCount = childCount,
                    remainderMode = remainderMode, userId = userId
                ))
            } else if (!isMasterBox || totalQuantity < childCount) {
                emitUiState(InventoryUiState.SmartEntrySuggestion(
                    compatibleBoxes = emptyList(),
                    requestedQuantity = totalQuantity,
                    origin = origin, model = model, size = size,
                    lot = lot, entryType = entryType,
                    isMasterBox = isMasterBox, childCount = childCount,
                    remainderMode = remainderMode, userId = userId
                ))
            } else {
                processNewEntry(origin, model, size, lot, entryType, isMasterBox, childCount, totalQuantity, remainderMode, userId)
            }
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al evaluar entrada: ${e.message}"))
        }
    }
}

fun InventoryViewModel.confirmAddToExistingBox(suggestion: InventoryUiState.SmartEntrySuggestion, targetBoxUuid: String) {
    viewModelScope.launch {
        try {
            emitUiState(InventoryUiState.Loading("Agregando a caja existente..."))
            val masterBox = dao.getMasterBoxByUuid(targetBoxUuid) ?: run {
                emitUiState(InventoryUiState.Error("Caja no encontrada: $targetBoxUuid"))
                return@launch
            }

            val available = masterBox.childCount - masterBox.activeChildCount
            val toAdd = minOf(suggestion.requestedQuantity, available)
            val remaining = suggestion.requestedQuantity - toAdd
            val timestamp = System.currentTimeMillis()
            val allUuids = mutableListOf<String>()
            val printItems = mutableListOf<PrintLabelItem>()

            val startSeq = masterBox.activeChildCount + 1
            val childUuids = uuidGenerator.generateUnitBatch(
                suggestion.origin, suggestion.lot, suggestion.size, toAdd, startSeq
            )

            val children = childUuids.map { uuid ->
                ProductEntity(
                    uuid = uuid, parentUuid = targetBoxUuid,
                    origin = suggestion.origin.name, model = suggestion.model,
                    size = suggestion.size, lot = suggestion.lot,
                    entryType = suggestion.entryType, status = "AVAILABLE",
                    location = "RACK", createdAt = timestamp, updatedAt = timestamp
                )
            }
            dao.insertProducts(children)
            allUuids.addAll(childUuids)
            childUuids.forEach { uuid ->
                printItems.add(PrintLabelItem(uuid, suggestion.model, suggestion.size, suggestion.lot, suggestion.origin.displayName))
                dao.insertMovement(MovementEntity(
                    uuid = uuid, type = "IN", reason = suggestion.entryType,
                    observation = "Agregado a caja existente $targetBoxUuid",
                    location = "RACK", timestamp = timestamp, userId = suggestion.userId
                ))
            }

            val newActive = masterBox.activeChildCount + toAdd
            val isNowComplete = newActive >= masterBox.childCount
            val newStatus = if (isNowComplete) "COMPLETE" else "PENDIENTE_POR_RELLENAR"
            dao.updateMasterBoxFull(targetBoxUuid, newActive, isNowComplete, newStatus)

            dao.insertMovement(MovementEntity(
                uuid = targetBoxUuid, type = "REFILL", reason = "Entrada inteligente",
                observation = "Añadidos $toAdd par(es). Ahora: $newActive/${masterBox.childCount}" +
                    if (isNowComplete) " — ¡COMPLETA!" else "",
                location = "RACK", timestamp = timestamp, userId = suggestion.userId
            ))

            var extraMessage = ""
            if (remaining > 0) {
                val looseUuids = uuidGenerator.generateUnitBatch(
                    suggestion.origin, suggestion.lot, suggestion.size, remaining, startSeq + toAdd
                )
                val loosePairs = looseUuids.map { uuid ->
                    ProductEntity(
                        uuid = uuid, origin = suggestion.origin.name,
                        model = suggestion.model, size = suggestion.size,
                        lot = suggestion.lot, entryType = suggestion.entryType,
                        status = "AVAILABLE", location = "RACK",
                        createdAt = timestamp, updatedAt = timestamp
                    )
                }
                dao.insertProducts(loosePairs)
                allUuids.addAll(looseUuids)
                looseUuids.forEach { uuid ->
                    printItems.add(PrintLabelItem(uuid, suggestion.model, suggestion.size, suggestion.lot, suggestion.origin.displayName))
                    dao.insertMovement(MovementEntity(
                        uuid = uuid, type = "IN", reason = suggestion.entryType,
                        observation = "Par suelto (excedente de caja $targetBoxUuid)",
                        location = "RACK", timestamp = timestamp, userId = suggestion.userId
                    ))
                }
                extraMessage = " + $remaining par(es) suelto(s)"
            }

            val printSuccess = syncManager.sendPrintJobNow(printItems)
            if (!printSuccess) syncManager.enqueuePrintJob(printItems)

            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessEntry(
                uuids = allUuids, model = suggestion.model, lot = suggestion.lot,
                size = suggestion.size, origin = suggestion.origin,
                message = "$toAdd par(es) agregados a caja $targetBoxUuid ($newActive/${masterBox.childCount})$extraMessage",
                warning = if (!printSuccess) "Impresión encolada" else null
            ))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error: ${e.message}"))
            Log.e("InventoryVM", "confirmAddToExistingBox failed", e)
        }
    }
}

fun InventoryViewModel.confirmLooseEntry(suggestion: InventoryUiState.SmartEntrySuggestion) {
    processNewEntry(
        origin = suggestion.origin, model = suggestion.model,
        size = suggestion.size, lot = suggestion.lot,
        entryType = suggestion.entryType, isMasterBox = false,
        childCount = suggestion.childCount,
        totalQuantity = suggestion.requestedQuantity,
        remainderMode = suggestion.remainderMode,
        userId = suggestion.userId
    )
}

fun InventoryViewModel.confirmNewIncompleteBox(suggestion: InventoryUiState.SmartEntrySuggestion) {
    processNewEntry(
        origin = suggestion.origin, model = suggestion.model,
        size = suggestion.size, lot = suggestion.lot,
        entryType = suggestion.entryType, isMasterBox = true,
        childCount = suggestion.childCount,
        totalQuantity = suggestion.requestedQuantity,
        remainderMode = RemainderMode.FILL_LATER,
        userId = suggestion.userId
    )
}
