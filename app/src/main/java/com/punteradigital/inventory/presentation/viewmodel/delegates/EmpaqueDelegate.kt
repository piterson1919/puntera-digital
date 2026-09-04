package com.punteradigital.inventory.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.punteradigital.inventory.data.local.entity.LabelEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

fun InventoryViewModel.generateLabels(
    origin: Origin,
    model: String,
    size: String,
    lot: String,
    labelType: String,
    labelFormat: String,
    isMasterBox: Boolean,
    childCount: Int,
    totalQuantity: Int,
    userId: String
) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Generando etiquetas de empaque..."))
        try {
            val timestamp = System.currentTimeMillis()
            val batchId = "BATCH-${getDateFormat().format(Date(timestamp))}-${timestamp}"
            val labels = mutableListOf<LabelEntity>()
            val printItems = mutableListOf<PrintLabelItem>()

            if (isMasterBox) {
                val autoBox = BusinessRules.calculateAutoBoxing(totalQuantity, childCount)
                var globalSeq = 1

                for (boxIdx in 1..autoBox.fullBoxes) {
                    val (parentUuid, childrenUuids) = uuidGenerator.generateMasterBoxBatch(
                        origin, lot, size, childCount, boxIdx, globalSeq
                    )
                    labels.add(LabelEntity(
                        uuid = parentUuid, batchId = batchId, origin = origin.name,
                        model = model, size = size, lot = lot, labelType = "MASTER_BOX",
                        labelFormat = labelFormat, createdBy = userId, createdAt = timestamp
                    ))
                    printItems.add(PrintLabelItem(parentUuid, model, size, lot, origin.displayName))

                    childrenUuids.forEach { childUuid ->
                        labels.add(LabelEntity(
                            uuid = childUuid, batchId = batchId, origin = origin.name,
                            model = model, size = size, lot = lot, labelType = "INDIVIDUAL",
                            labelFormat = labelFormat, parentLabelUuid = parentUuid,
                            createdBy = userId, createdAt = timestamp
                        ))
                        printItems.add(PrintLabelItem(childUuid, model, size, lot, origin.displayName))
                    }
                    globalSeq += childCount
                }
                if (autoBox.hasRemainder) {
                    val looseUuids = uuidGenerator.generateUnitBatch(
                        origin, lot, size, autoBox.remainderPairs, globalSeq
                    )
                    looseUuids.forEach { childUuid ->
                        labels.add(LabelEntity(
                            uuid = childUuid, batchId = batchId, origin = origin.name,
                            model = model, size = size, lot = lot, labelType = "INDIVIDUAL",
                            labelFormat = labelFormat, createdBy = userId, createdAt = timestamp
                        ))
                        printItems.add(PrintLabelItem(childUuid, model, size, lot, origin.displayName))
                    }
                }
            } else {
                val uuids = uuidGenerator.generateUnitBatch(origin, lot, size, totalQuantity, 1)
                uuids.forEach { uuid ->
                    labels.add(LabelEntity(
                        uuid = uuid, batchId = batchId, origin = origin.name,
                        model = model, size = size, lot = lot, labelType = "INDIVIDUAL",
                        labelFormat = labelFormat, createdBy = userId, createdAt = timestamp
                    ))
                    printItems.add(PrintLabelItem(uuid, model, size, lot, origin.displayName))
                }
            }

            dao.insertLabels(labels)

            emitUiState(InventoryUiState.Loading("Enviando a impresora BarTender..."))
            val printSuccess = syncManager.sendPrintJobNow(printItems)
            if (!printSuccess) {
                syncManager.enqueuePrintJob(printItems)
            }

            labels.forEach { 
                dao.markLabelPrinted(it.uuid, System.currentTimeMillis())
            }

            emitUiState(InventoryUiState.SuccessMovement(
                "Se generaron ${labels.size} etiquetas (Lote: $batchId)." + 
                if (!printSuccess) "\nImpresión encolada offline." else ""
            ))

        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al generar etiquetas: ${e.message}"))
            Log.e("InventoryVM", "generateLabels failed", e)
        }
    }
}

fun InventoryViewModel.deleteLabelBatch(batchId: String) {
    viewModelScope.launch {
        try {
            dao.deleteLabelBatch(batchId)
            emitUiState(InventoryUiState.SuccessMovement("Lote de etiquetas eliminado con éxito."))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al eliminar lote: ${e.message}"))
        }
    }
}

fun InventoryViewModel.reprintLabelBatch(batchId: String) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Reimprimiendo lote..."))
        try {
            val labels = dao.getLabelsByBatch(batchId).first()
            val printItems = labels.map { 
                PrintLabelItem(it.uuid, it.model, it.size, it.lot, Origin.fromString(it.origin).displayName) 
            }
            val success = syncManager.sendPrintJobNow(printItems)
            if (!success) syncManager.enqueuePrintJob(printItems)
            
            emitUiState(InventoryUiState.SuccessMovement("Lote reenviado a impresora."))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al reimprimir: ${e.message}"))
        }
    }
}

fun InventoryViewModel.confirmPreEntryBatch(batchId: String, userId: String) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Confirmando entrada al almacén..."))
        try {
            val labels = dao.getPendingLabelsByBatchSync(batchId)
            if (labels.isEmpty()) {
                emitUiState(InventoryUiState.Error("No hay etiquetas pendientes en este lote."))
                return@launch
            }

            val timestamp = System.currentTimeMillis()
            val products = mutableListOf<ProductEntity>()
            val masterBoxes = mutableListOf<MasterBoxEntity>()
            val movements = mutableListOf<MovementEntity>()
            val movementsDto = mutableListOf<InventoryMovementDto>()

            val masterLabels = labels.filter { it.labelType == "MASTER_BOX" }
            val childLabels = labels.filter { it.labelType == "INDIVIDUAL" }

            masterLabels.forEach { ml ->
                val childrenCount = childLabels.count { it.parentLabelUuid == ml.uuid }
                masterBoxes.add(MasterBoxEntity(
                    uuid = ml.uuid, origin = ml.origin, model = ml.model, size = ml.size, lot = ml.lot,
                    childCount = childrenCount, activeChildCount = childrenCount,
                    isComplete = true, status = "COMPLETE", createdAt = timestamp
                ))
                movements.add(MovementEntity(
                    uuid = ml.uuid, type = "IN", reason = "PRODUCCION",
                    observation = "Ingreso desde Empaque (Lote: $batchId)",
                    location = "RACK", timestamp = timestamp, userId = userId
                ))
                movementsDto.add(buildMovementDto(ml.uuid, "IN", "PRODUCCION", ml.model, ml.size, ml.lot, Origin.fromString(ml.origin), timestamp, userId))
            }

            childLabels.forEach { cl ->
                products.add(ProductEntity(
                    uuid = cl.uuid, parentUuid = cl.parentLabelUuid, origin = cl.origin,
                    model = cl.model, size = cl.size, lot = cl.lot, entryType = "PRODUCCION",
                    status = "AVAILABLE", location = "RACK", createdAt = timestamp, updatedAt = timestamp
                ))
                if (cl.parentLabelUuid == null) {
                    movements.add(MovementEntity(
                        uuid = cl.uuid, type = "IN", reason = "PRODUCCION",
                        observation = "Ingreso individual desde Empaque (Lote: $batchId)",
                        location = "RACK", timestamp = timestamp, userId = userId
                    ))
                    movementsDto.add(buildMovementDto(cl.uuid, "IN", "PRODUCCION", cl.model, cl.size, cl.lot, Origin.fromString(cl.origin), timestamp, userId))
                }
            }

            dao.insertMasterBoxes(masterBoxes)
            dao.insertProducts(products)
            dao.insertMovements(movements)
            dao.markBatchEntered(batchId, userId, timestamp)

            val originStr = labels.first().origin
            val lot = labels.first().lot
            val sheetsRequest = SyncRequestDto(
                title = "Entrada_${lot}_${getDateFormat().format(Date(timestamp))}",
                origin = originStr, movements = movementsDto
            )
            if (!syncManager.sendSheetsSyncNow(sheetsRequest)) {
                syncManager.enqueueSheetsSync(sheetsRequest)
            }

            emitUiState(InventoryUiState.SuccessEntry(
                uuids = labels.map { it.uuid }, model = labels.first().model, 
                lot = lot, size = labels.first().size, origin = Origin.fromString(originStr),
                message = "Entrada confirmada correctamente para ${labels.size} unidades.",
                warning = null
            ))

        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al confirmar entrada: ${e.message}"))
            Log.e("InventoryVM", "confirmPreEntryBatch failed", e)
        }
    }
}

fun InventoryViewModel.confirmLabelEntry(uuid: String, location: String, userId: String, checkedChildUuids: List<String>? = null) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Registrando entrada por escaneo..."))
        try {
            val label = dao.getLabelByUuid(uuid)
            if (label == null) {
                emitUiState(InventoryUiState.Error("Etiqueta no encontrada en el sistema pre-registro."))
                soundManager.playErrorBeep()
                return@launch
            }

            if (label.status == "ENTERED") {
                emitUiState(InventoryUiState.Error("Esta etiqueta ya fue ingresada al almacén anteriormente."))
                soundManager.playErrorBeep()
                return@launch
            }

            val timestamp = System.currentTimeMillis()
            val products = mutableListOf<ProductEntity>()
            val masterBoxes = mutableListOf<MasterBoxEntity>()
            val movements = mutableListOf<MovementEntity>()
            val movementsDto = mutableListOf<InventoryMovementDto>()
            
            val origin = Origin.fromString(label.origin)

            if (label.labelType == "MASTER_BOX") {
                val childLabels = dao.getChildrenLabels(label.uuid)
                val activeChildLabels = if (checkedChildUuids != null) {
                    childLabels.filter { it.uuid in checkedChildUuids }
                } else {
                    childLabels
                }
                val activeCount = activeChildLabels.size
                val isComplete = activeCount == childLabels.size

                masterBoxes.add(MasterBoxEntity(
                    uuid = label.uuid,
                    origin = label.origin,
                    model = label.model,
                    size = label.size,
                    lot = label.lot,
                    childCount = childLabels.size,
                    activeChildCount = activeCount,
                    isComplete = isComplete,
                    status = if (isComplete) "COMPLETE" else "PENDIENTE_POR_RELLENAR",
                    createdAt = timestamp
                ))

                activeChildLabels.forEach { cl ->
                    products.add(ProductEntity(
                        uuid = cl.uuid,
                        parentUuid = label.uuid,
                        origin = cl.origin,
                        model = cl.model,
                        size = cl.size,
                        lot = cl.lot,
                        entryType = "PRODUCCION",
                        status = "AVAILABLE",
                        location = location,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    ))
                    dao.markLabelEntered(cl.uuid, userId, timestamp)
                }

                if (checkedChildUuids != null) {
                    val unchecked = childLabels.filter { it.uuid !in checkedChildUuids }
                    unchecked.forEach { cl ->
                        dao.deleteLabel(cl.uuid)
                    }
                }

                movements.add(MovementEntity(
                    uuid = label.uuid,
                    type = "IN",
                    reason = "PRODUCCION",
                    observation = "Ingreso Caja Master escaneada (Lote: ${label.lot}, $activeCount pares)" + if (!isComplete) " — Incompleta" else "",
                    location = location,
                    timestamp = timestamp,
                    userId = userId
                ))

                movementsDto.add(buildMovementDto(label.uuid, "IN", "PRODUCCION", label.model, label.size, label.lot, origin, timestamp, userId))

            } else {
                products.add(ProductEntity(
                    uuid = label.uuid,
                    parentUuid = label.parentLabelUuid,
                    origin = label.origin,
                    model = label.model,
                    size = label.size,
                    lot = label.lot,
                    entryType = "PRODUCCION",
                    status = "AVAILABLE",
                    location = location,
                    createdAt = timestamp,
                    updatedAt = timestamp
                ))

                movements.add(MovementEntity(
                    uuid = label.uuid,
                    type = "IN",
                    reason = "PRODUCCION",
                    observation = "Ingreso Individual escaneado (Lote: ${label.lot})",
                    location = location,
                    timestamp = timestamp,
                    userId = userId
                ))

                movementsDto.add(buildMovementDto(label.uuid, "IN", "PRODUCCION", label.model, label.size, label.lot, origin, timestamp, userId))
            }

            dao.insertMasterBoxes(masterBoxes)
            dao.insertProducts(products)
            dao.insertMovements(movements)
            dao.markLabelEntered(label.uuid, userId, timestamp)

            val sheetsRequest = SyncRequestDto(
                title = "Entrada_Escaneo_${label.lot}_${getDateFormat().format(Date(timestamp))}",
                origin = label.origin,
                movements = movementsDto
            )
            if (!syncManager.sendSheetsSyncNow(sheetsRequest)) {
                syncManager.enqueueSheetsSync(sheetsRequest)
            }

            detectOriginFromUuid(label.uuid)
            soundManager.playSuccessBeep()

            val quantityMessage = if (label.labelType == "MASTER_BOX") {
                "Caja Master con ${products.size} pares"
            } else {
                "1 par individual"
            }

            emitUiState(InventoryUiState.SuccessEntry(
                uuids = products.map { it.uuid } + if (label.labelType == "MASTER_BOX") listOf(label.uuid) else emptyList(),
                model = label.model,
                lot = label.lot,
                size = label.size,
                origin = origin,
                message = "Ingreso por Escaneo exitoso: $quantityMessage registrado en Rack $location.",
                warning = null
            ))

        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al confirmar ingreso por escaneo: ${e.message}"))
            Log.e("InventoryVM", "confirmLabelEntry failed", e)
        }
    }
}
