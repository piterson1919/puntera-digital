package com.punteradigital.inventory.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.punteradigital.inventory.data.local.entity.MovementEntity
import com.punteradigital.inventory.domain.model.BajaReason
import com.punteradigital.inventory.domain.rules.BusinessRules
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.presentation.viewmodel.*
import com.punteradigital.inventory.presentation.viewmodel.InventoryUiState
import kotlinx.coroutines.launch

fun InventoryViewModel.processStandBy(uuid: String, userId: String, cliente: String = "", observaciones: String = "") {
    viewModelScope.launch {
        try {
            val product = dao.getProductByUuid(uuid)
            if (product == null) {
                emitUiState(InventoryUiState.Error("UUID no encontrado: $uuid"))
                soundManager.playErrorBeep()
                return@launch
            }

            val validation = BusinessRules.validateStatusTransition(product.status, "STB")
            if (!validation.isValid) {
                emitUiState(InventoryUiState.Error(validation.errorMessage!!))
                soundManager.playErrorBeep()
                return@launch
            }

            dao.updateProductStatus(uuid, "STB", "ZONA_PREDESPACHO")
            dao.insertMovement(MovementEntity(
                uuid = uuid, type = "STB", reason = "Pre-despacho",
                location = "ZONA_PREDESPACHO", userId = userId,
                cliente = cliente, observacionesExtra = observaciones
            ))

            if (product.parentUuid != null) {
                updateMasterBoxCompleteness(product.parentUuid)
            }

            detectOriginFromUuid(uuid)
            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessMovement("Stand-By registrado: $uuid → ZONA_PREDESPACHO"))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error: ${e.message}"))
        }
    }
}

fun InventoryViewModel.confirmDispatchFromList(
    selectedUuids: List<String>,
    cliente: String,
    observaciones: String,
    userId: String
) {
    viewModelScope.launch {
        if (selectedUuids.isEmpty()) return@launch

        emitUiState(InventoryUiState.Loading("Procesando despacho..."))
        try {
            val products = selectedUuids.mapNotNull { dao.getProductByUuid(it) }

            val nonStb = products.filter { it.status != "STB" }
            if (nonStb.isNotEmpty()) {
                emitUiState(InventoryUiState.Error("Error: ${nonStb.size} producto(s) no están en Stand-By. No se puede despachar."))
                soundManager.playErrorBeep()
                return@launch
            }

            val validation = BusinessRules.validateNoMixedOrigins(products)
            if (!validation.isValid) {
                soundManager.playCriticalAlert()
                emitUiState(InventoryUiState.PokayokeAlert(validation.errorMessage!!))
                return@launch
            }

            for (product in products) {
                val dispatched = dao.dispatchProduct(MovementEntity(
                    uuid = product.uuid, type = "OUT", reason = "Despacho",
                    location = "DESPACHADO", userId = userId,
                    cliente = cliente, observacionesExtra = observaciones
                ))
                if (!dispatched) {
                    throw IllegalStateException("El producto ${product.uuid} ya no está en Stand-By")
                }
                if (product.parentUuid != null) {
                    updateMasterBoxCompleteness(product.parentUuid)
                }
            }

            emitUiState(InventoryUiState.SuccessMovement("Despacho completado: ${products.size} unidades procesadas → $cliente"))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al despachar: ${e.message}"))
        }
    }
}

fun InventoryViewModel.confirmPedidoDelivery(
    selectedUuids: List<String>,
    cliente: String,
    userId: String
) {
    viewModelScope.launch {
        if (selectedUuids.isEmpty()) return@launch
        emitUiState(InventoryUiState.Loading("Confirmando entrega de pedido..."))
        try {
            val products = selectedUuids.mapNotNull { dao.getProductByUuid(it) }
            val timestamp = System.currentTimeMillis()
            for (product in products) {
                dao.updateProductStatus(product.uuid, "DISPATCHED", "DESPACHADO")
                dao.insertMovement(MovementEntity(
                    uuid = product.uuid, type = "OUT", reason = "Entrega Pedido Venta",
                    location = "DESPACHADO", timestamp = timestamp, userId = userId,
                    cliente = cliente, observacionesExtra = "Entrega de pedido"
                ))
                if (product.parentUuid != null) {
                    updateMasterBoxCompleteness(product.parentUuid)
                }
            }
            emitUiState(InventoryUiState.SuccessMovement("Pedido entregado: ${products.size} unidades despachadas."))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al entregar pedido: ${e.message}"))
        }
    }
}

fun InventoryViewModel.confirmPedidoReturn(
    selectedUuids: List<String>,
    userId: String
) {
    viewModelScope.launch {
        if (selectedUuids.isEmpty()) return@launch
        emitUiState(InventoryUiState.Loading("Registrando retorno de pedido..."))
        try {
            val products = selectedUuids.mapNotNull { dao.getProductByUuid(it) }
            val timestamp = System.currentTimeMillis()
            for (product in products) {
                dao.updateProductStatus(product.uuid, "AVAILABLE", "RACK")
                dao.insertMovement(MovementEntity(
                    uuid = product.uuid, type = "IN", reason = "Retorno Pedido Venta",
                    location = "RACK", timestamp = timestamp, userId = userId,
                    observacionesExtra = "Retorno de pedido devuelto al stock"
                ))
                if (product.parentUuid != null) {
                    updateMasterBoxCompleteness(product.parentUuid)
                }
            }
            emitUiState(InventoryUiState.SuccessMovement("Retorno registrado: ${products.size} unidades devueltas al stock."))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al retornar pedido: ${e.message}"))
        }
    }
}

fun InventoryViewModel.processMuestra(uuid: String, cliente: String, observaciones: String, userId: String) {
    viewModelScope.launch {
        try {
            val product = dao.getProductByUuid(uuid)
            if (product == null) {
                emitUiState(InventoryUiState.Error("UUID no encontrado: $uuid"))
                soundManager.playErrorBeep()
                return@launch
            }

            val validation = BusinessRules.validateStatusTransition(product.status, "MUESTRA")
            if (!validation.isValid) {
                emitUiState(InventoryUiState.Error(validation.errorMessage!!))
                soundManager.playErrorBeep()
                return@launch
            }

            dao.updateProductStatus(uuid, "MUESTRA", "ZONA_CUSTODIA_COMERCIAL")
            dao.insertMovement(MovementEntity(
                uuid = uuid, type = "MUESTRA", reason = "Muestra retornable",
                observation = "Entregado a cliente: $cliente",
                location = "ZONA_CUSTODIA_COMERCIAL", userId = userId,
                cliente = cliente, observacionesExtra = observaciones
            ))

            if (product.parentUuid != null) {
                updateMasterBoxCompleteness(product.parentUuid)
            }

            detectOriginFromUuid(uuid)
            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessMovement("Muestra registrada: $uuid → $cliente (ZONA_CUSTODIA_COMERCIAL)"))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error: ${e.message}"))
        }
    }
}

fun InventoryViewModel.returnMuestra(uuid: String, userId: String) {
    viewModelScope.launch {
        try {
            val product = dao.getProductByUuid(uuid)
            if (product == null || product.status != "MUESTRA") {
                emitUiState(InventoryUiState.Error("Muestra no encontrada o estado inválido"))
                soundManager.playErrorBeep()
                return@launch
            }

            dao.updateProductStatus(uuid, "AVAILABLE", "RACK")
            dao.insertMovement(MovementEntity(
                uuid = uuid, type = "IN", reason = "Retorno de muestra",
                observation = "Muestra devuelta al stock",
                location = "RACK", userId = userId
            ))

            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessMovement("Muestra retornada al stock: $uuid → RACK"))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error: ${e.message}"))
        }
    }
}

fun InventoryViewModel.sellMuestra(uuid: String, userId: String) {
    viewModelScope.launch {
        try {
            val product = dao.getProductByUuid(uuid)
            if (product == null || product.status != "MUESTRA") {
                emitUiState(InventoryUiState.Error("Muestra no encontrada o estado inválido"))
                soundManager.playErrorBeep()
                return@launch
            }

            dao.updateProductStatus(uuid, "MUESTRA_VENDIDA", "VENDIDA")
            dao.insertMovement(MovementEntity(
                uuid = uuid, type = "OUT", reason = "Muestra vendida",
                observation = "Cliente decidió quedarse con el par",
                location = "VENDIDA", userId = userId
            ))

            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessMovement("Muestra vendida: $uuid — Cliente se quedó con el par"))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error: ${e.message}"))
        }
    }
}

fun InventoryViewModel.processQualityBaja(uuid: String, reason: BajaReason, userId: String) {
    viewModelScope.launch {
        try {
            val product = dao.getProductByUuid(uuid)
            if (product == null) {
                emitUiState(InventoryUiState.Error("UUID no encontrado: $uuid"))
                soundManager.playErrorBeep()
                return@launch
            }

            val targetStatus = reason.toProductStatus().name
            dao.updateProductStatus(uuid, targetStatus, "BAJA")
            dao.insertMovement(MovementEntity(
                uuid = uuid, type = "BAJA", reason = reason.displayName,
                location = "BAJA", userId = userId
            ))

            if (product.parentUuid != null) {
                updateMasterBoxCompleteness(product.parentUuid)
            }

            detectOriginFromUuid(uuid)
            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessMovement("Baja registrada: $uuid → ${reason.displayName}"))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al registrar baja: ${e.message}"))
            soundManager.playErrorBeep()
        }
    }
}

fun InventoryViewModel.refillMasterBox(parentUuid: String, childUuids: List<String>, userId: String) {
    viewModelScope.launch {
        try {
            val masterBox = dao.getMasterBoxByUuid(parentUuid)
            if (masterBox == null) {
                emitUiState(InventoryUiState.Error("Caja Master no encontrada: $parentUuid"))
                soundManager.playErrorBeep()
                return@launch
            }

            val capacityCheck = BusinessRules.validateRefillCapacity(masterBox, childUuids.size)
            if (!capacityCheck.isValid) {
                emitUiState(InventoryUiState.Error(capacityCheck.errorMessage!!))
                soundManager.playErrorBeep()
                return@launch
            }

            val timestamp = System.currentTimeMillis()
            for (childUuid in childUuids) {
                val product = dao.getProductByUuid(childUuid)
                if (product == null) {
                    emitUiState(InventoryUiState.Error("Producto no encontrado: $childUuid"))
                    soundManager.playErrorBeep()
                    return@launch
                }

                val compat = BusinessRules.validateRefillCompatibility(masterBox, product)
                if (!compat.isValid) {
                    emitUiState(InventoryUiState.Error(compat.errorMessage!!))
                    soundManager.playErrorBeep()
                    return@launch
                }

                dao.insertProduct(product.copy(parentUuid = parentUuid, updatedAt = timestamp))
                dao.insertMovement(MovementEntity(
                    uuid = childUuid, type = "REFILL", reason = "Rellenado de caja master",
                    observation = "Vinculado a caja $parentUuid",
                    location = "RACK", timestamp = timestamp, userId = userId
                ))
            }

            val newActiveCount = masterBox.activeChildCount + childUuids.size
            val isNowComplete = newActiveCount >= masterBox.childCount
            val newStatus = if (isNowComplete) "COMPLETE" else "PENDIENTE_POR_RELLENAR"
            dao.updateMasterBoxFull(parentUuid, newActiveCount, isNowComplete, newStatus)

            dao.insertMovement(MovementEntity(
                uuid = parentUuid, type = "REFILL", reason = "Caja rellenada",
                observation = "Añadidos ${childUuids.size} par(es). Ahora: $newActiveCount/${masterBox.childCount}",
                location = "RACK", timestamp = timestamp, userId = userId
            ))

            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessRefill(
                parentUuid = parentUuid,
                addedCount = childUuids.size,
                totalActive = newActiveCount,
                totalCapacity = masterBox.childCount
            ))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al rellenar caja: ${e.message}"))
            Log.e("InventoryVM", "refillMasterBox failed", e)
        }
    }
}

fun InventoryViewModel.transferLocation(uuid: String, newLocation: String, userId: String) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Procesando traslado..."))
        try {
            val timestamp = System.currentTimeMillis()
            val masterBox = dao.getMasterBoxByUuid(uuid)
            if (masterBox != null) {
                val children = dao.getChildrenOfMasterBox(uuid)
                children.forEach { child ->
                    dao.updateProductStatus(child.uuid, child.status, newLocation, timestamp)
                    dao.insertMovement(MovementEntity(
                        uuid = child.uuid, type = "TRANSFER", reason = "Traslado Interno a $newLocation",
                        location = newLocation, timestamp = timestamp, userId = userId
                    ))
                }
                dao.insertMovement(MovementEntity(
                    uuid = uuid, type = "TRANSFER", reason = "Traslado Interno Caja Master a $newLocation",
                    location = newLocation, timestamp = timestamp, userId = userId
                ))
                soundManager.playSuccessBeep()
                emitUiState(InventoryUiState.SuccessMovement("Caja Master $uuid y sus ${children.size} pares trasladados a $newLocation."))
            } else {
                val product = dao.getProductByUuid(uuid)
                if (product != null) {
                    dao.updateProductStatus(uuid, product.status, newLocation, timestamp)
                    dao.insertMovement(MovementEntity(
                        uuid = uuid, type = "TRANSFER", reason = "Traslado Interno a $newLocation",
                        location = newLocation, timestamp = timestamp, userId = userId
                    ))
                    soundManager.playSuccessBeep()
                    emitUiState(InventoryUiState.SuccessMovement("Producto $uuid trasladado a $newLocation."))
                } else {
                    emitUiState(InventoryUiState.Error("UUID no encontrado para traslado: $uuid"))
                    soundManager.playErrorBeep()
                }
            }
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al realizar traslado: ${e.message}"))
            soundManager.playErrorBeep()
        }
    }
}

fun InventoryViewModel.adjustAuditInventory(
    location: String,
    scannedUuids: List<String>,
    missingUuids: List<String>,
    extraUuids: List<String>,
    userId: String
) {
    viewModelScope.launch {
        emitUiState(InventoryUiState.Loading("Ajustando inventario de auditoría..."))
        try {
            val timestamp = System.currentTimeMillis()
            
            extraUuids.forEach { uuid ->
                val product = dao.getProductByUuid(uuid)
                if (product != null) {
                    dao.updateProductStatus(uuid, "AVAILABLE", location, timestamp)
                    dao.insertMovement(MovementEntity(
                        uuid = uuid, type = "TRANSFER", reason = "Ajuste Auditoría: Extra reubicado a $location",
                        location = location, timestamp = timestamp, userId = userId
                    ))
                    if (product.parentUuid != null) {
                         updateMasterBoxCompleteness(product.parentUuid)
                    }
                }
            }

            missingUuids.forEach { uuid ->
                val product = dao.getProductByUuid(uuid)
                if (product != null) {
                    dao.updateProductStatus(uuid, "BAJA_CONTEO_CICLICO", "BAJA", timestamp)
                    dao.insertMovement(MovementEntity(
                        uuid = uuid, type = "BAJA", reason = "Baja por Auditoría: Faltante en $location",
                        location = "BAJA", timestamp = timestamp, userId = userId
                    ))
                    if (product.parentUuid != null) {
                        updateMasterBoxCompleteness(product.parentUuid)
                    }
                }
            }

            soundManager.playSuccessBeep()
            emitUiState(InventoryUiState.SuccessMovement("Ajuste de auditoría completado en rack $location. Extras reubicados: ${extraUuids.size}, Faltantes dados de baja: ${missingUuids.size}."))
        } catch (e: Exception) {
            emitUiState(InventoryUiState.Error("Error al ajustar inventario de auditoría: ${e.message}"))
            soundManager.playErrorBeep()
        }
    }
}
