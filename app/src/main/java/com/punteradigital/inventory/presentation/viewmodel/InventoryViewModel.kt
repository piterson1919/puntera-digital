package com.punteradigital.inventory.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punteradigital.inventory.data.local.dao.BatchStatus
import com.punteradigital.inventory.data.local.dao.InventoryDao
import com.punteradigital.inventory.data.local.entity.*
import com.punteradigital.inventory.data.remote.InventorySyncEventDto
import com.punteradigital.inventory.data.repository.InventorySyncRepository
import com.punteradigital.inventory.data.repository.SyncManager
import com.punteradigital.inventory.domain.model.Origin
import com.punteradigital.inventory.domain.usecase.PdfGeneratorUseCase
import com.punteradigital.inventory.domain.usecase.UuidGeneratorUseCase
import com.punteradigital.inventory.util.ConnectivityMonitor
import com.punteradigital.inventory.util.SoundManager
import com.punteradigital.inventory.data.local.EmpaquePreferences
import com.punteradigital.inventory.data.local.SyncPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val dao: InventoryDao,
    val uuidGenerator: UuidGeneratorUseCase,
    val pdfGenerator: PdfGeneratorUseCase,
    val syncManager: SyncManager,
    val inventorySyncRepository: InventorySyncRepository,
    val connectivityMonitor: ConnectivityMonitor,
    val soundManager: SoundManager,
    val empaquePreferences: EmpaquePreferences,
    val syncPreferences: SyncPreferences
) : ViewModel() {

    companion object {
        const val PIN_SALT = "punteradigital_v1_salt"
    }

    private val dateFormat = ThreadLocal.withInitial { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    private val timeFormat = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    fun getDateFormat(): SimpleDateFormat = dateFormat.get()!!
    fun getTimeFormat(): SimpleDateFormat = timeFormat.get()!!

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Idle)
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _currentOrigin = MutableStateFlow(Origin.FOOT_SAFE)
    val currentOrigin: StateFlow<Origin> = _currentOrigin.asStateFlow()

    fun emitUiState(state: InventoryUiState) {
        _uiState.value = state
    }

    val inventoryStatus: Flow<List<BatchStatus>> = dao.getInventoryStatusByBatch()
    val totalAvailable: Flow<Int> = dao.getTotalAvailableCount()
    val totalStandBy: Flow<Int> = dao.getTotalStandByCount()
    val totalMasterBoxes: Flow<Int> = dao.getTotalMasterBoxCount()
    val pendingSyncCount: Flow<Int> = dao.getPendingSyncCount()

    val recentMovements: Flow<List<MovementEntity>> by lazy { dao.getRecentMovements(20) }
    val traceabilityMovements: Flow<List<MovementEntity>> by lazy { dao.getAllMovements() }

    val weeklyMovements: Flow<List<MovementEntity>> by lazy {
        flow {
            emit(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L))
        }.flatMapLatest { cutoff ->
            dao.getMovementsSince(cutoff)
        }
    }

    val standByItems: Flow<List<ProductEntity>> by lazy { dao.getStandByProducts() }
    val muestrasActivas: Flow<List<ProductEntity>> by lazy { dao.getMuestrasActivas() }
    val incompleteMasterBoxes: Flow<List<MasterBoxEntity>> by lazy { dao.getIncompleteMasterBoxes() }
    val allUsers: Flow<List<UserEntity>> by lazy { dao.getAllUsers() }
    val catalogModels: Flow<List<CatalogModelEntity>> by lazy { dao.getCatalogModels() }

    val topDispatchedModels by lazy { dao.getTopDispatchedModels() }
    val topDispatchedSizes by lazy { dao.getTopDispatchedSizes() }
    val topClients by lazy { dao.getTopClients() }
    val modelStatusBreakdown by lazy { dao.getModelStatusBreakdown() }
    val allModelsInventory by lazy { dao.getAllModelsInventory() }
    val allSizesInventory by lazy { dao.getAllSizesInventory() }
    val totalDispatched by lazy { dao.getTotalDispatchedCount() }
    val totalMovements by lazy { dao.getTotalMovementCount() }
    val rackOccupancy by lazy { dao.getProductCountByLocation() }
    private val _entryMovements = MutableStateFlow<List<MovementEntity>>(emptyList())
    val entryMovements: StateFlow<List<MovementEntity>> = _entryMovements.asStateFlow()

    private val _isLoadingEntryMovements = MutableStateFlow(false)
    val isLoadingEntryMovements: StateFlow<Boolean> = _isLoadingEntryMovements.asStateFlow()

    private var entryMovementsOffset = 0
    private var hasMoreEntryMovements = true
    private var qrSearchJob: Job? = null
    private val catalogSyncMutex = Mutex()

    private val _qrSearchResults = MutableStateFlow<List<ProductEntity>>(emptyList())
    val qrSearchResults: StateFlow<List<ProductEntity>> = _qrSearchResults.asStateFlow()

    val pendingLabelBatches: StateFlow<List<LabelBatchSummary>> = dao.getLabelBatchSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingLabelCount: StateFlow<Int> = dao.getPendingLabelCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _lockedRack = MutableStateFlow<String?>(null)
    val lockedRack: StateFlow<String?> = _lockedRack.asStateFlow()

    private val _isRackLocked = MutableStateFlow(false)
    val isRackLocked: StateFlow<Boolean> = _isRackLocked.asStateFlow()

    init {
        loadMoreEntryMovements()

        viewModelScope.launch {
            seedDefaultCatalogIfEmpty()
            syncCatalogFromServer()
            syncUsersFromServer()
        }

        inventorySyncRepository.connectRealtime { event ->
            if (event.entityType == "catalog_model") {
                viewModelScope.launch {
                    applyCatalogModelEvent(event)
                }
            }
            if (event.entityType == "user") {
                viewModelScope.launch {
                    applyUserEvent(event)
                }
            }
        }

        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { isOnline ->
                if (isOnline) {
                    val pendingCount = dao.getPendingSyncItems().size
                    if (pendingCount > 0) {
                        val synced = syncManager.processPendingQueue()
                        if (synced > 0) {
                            Log.i("InventoryVM", "Auto-synced $synced/$pendingCount pending items")
                        }
                    }

                    syncCatalogFromServer()
                    syncUsersFromServer()
                }
            }
        }
    }

    private suspend fun seedDefaultCatalogIfEmpty() {
        if (dao.getCatalogModelCount() > 0) return

        val defaults = listOf(
            CatalogModelEntity(id = "default-fs300", code = "FS300CMFFPBL", name = "Foot Safe 300 Comp", sizeMin = 36, sizeMax = 46, pairsPerBox = 8, isActive = true),
            CatalogModelEntity(id = "default-fs302", code = "FS302CMN", name = "Foot Safe 302", sizeMin = 36, sizeMax = 46, pairsPerBox = 8, isActive = true),
            CatalogModelEntity(id = "default-fs400", code = "FS400BK", name = "Foot Safe 400 Black", sizeMin = 36, sizeMax = 46, pairsPerBox = 8, isActive = true),
            CatalogModelEntity(id = "default-sf200", code = "SF200LT", name = "Safety 200 Lite", sizeMin = 36, sizeMax = 46, pairsPerBox = 8, isActive = false)
        )

        defaults.forEach { dao.insertCatalogModel(it) }
    }

    private suspend fun syncCatalogFromServer() {
        if (!syncPreferences.isEnabled) return

        catalogSyncMutex.withLock {
            try {
                val remoteModels = inventorySyncRepository.fetchCatalogModels()
                if (remoteModels.isEmpty()) return

                val localModels = remoteModels.map { remote ->
                    CatalogModelEntity(
                        id = remote.id,
                        code = remote.code,
                        name = remote.name,
                        sizeMin = remote.sizeMin,
                        sizeMax = remote.sizeMax,
                        pairsPerBox = remote.pairsPerBox,
                        isActive = remote.isActive,
                        imageUri = remote.imageUri?.let { imageUri ->
                            if (imageUri.startsWith("/")) {
                                syncPreferences.baseUrl.trimEnd('/') + imageUri
                            } else {
                                imageUri
                            }
                        },
                        createdAt = remote.createdAt,
                        updatedAt = remote.updatedAt
                    )
                }

                dao.clearCatalogModels()
                dao.insertCatalogModels(localModels)
                Log.i("InventoryVM", "Synced ${localModels.size} catalog models from central backend")
            } catch (e: Exception) {
                Log.e("InventoryVM", "Failed to sync central catalog models", e)
            }
        }
    }

    private suspend fun applyCatalogModelEvent(event: InventorySyncEventDto) {
        val payload = event.payload
        val modelId = payload["id"] ?: payload["code"] ?: return

        if (event.action == "delete") {
            dao.deleteCatalogModel(modelId)
            return
        }

        dao.insertCatalogModel(
            CatalogModelEntity(
                id = modelId,
                code = payload["code"] ?: modelId,
                name = payload["name"] ?: "",
                sizeMin = payload["sizeMin"]?.toIntOrNull() ?: 36,
                sizeMax = payload["sizeMax"]?.toIntOrNull() ?: 46,
                pairsPerBox = payload["pairsPerBox"]?.toIntOrNull() ?: 8,
                isActive = payload["isActive"]?.toBoolean() ?: true,
                imageUri = if (!payload["imageData"].isNullOrBlank()) {
                    "${syncPreferences.baseUrl.trimEnd('/')}/api/catalog/models/$modelId/image"
                } else {
                    payload["imageUri"]?.ifBlank { null }
                },
                createdAt = payload["createdAt"]?.toLongOrNull() ?: event.timestamp,
                updatedAt = payload["updatedAt"]?.toLongOrNull() ?: event.timestamp
            )
        )
    }

    private suspend fun syncUsersFromServer() {
        if (!syncPreferences.isEnabled) return

        try {
            val remoteUsers = inventorySyncRepository.fetchUsers()
            val localUsers = dao.getAllUsers().first()
            if (remoteUsers.isEmpty()) {
                localUsers.forEach { user ->
                    inventorySyncRepository.sendInventoryEvent(
                        InventorySyncEventDto(
                            eventId = UUID.randomUUID().toString(),
                            entityType = "user",
                            action = "upsert",
                            payload = mapOf(
                                "id" to user.id,
                                "name" to user.name,
                                "pin" to user.pin,
                                "role" to user.role
                            ),
                            userId = currentUser.value?.id ?: "system",
                            deviceId = "android-device-${UUID.randomUUID()}"
                        )
                    )
                }
                if (localUsers.isNotEmpty()) {
                    Log.i("InventoryVM", "Bootstrapped ${localUsers.size} local users to central backend")
                }
                return
            }

            val remoteIds = remoteUsers.map { it.id }.toSet()
            localUsers.filter { it.id !in remoteIds }.forEach { dao.deleteUser(it.id) }

            remoteUsers.forEach { remote ->
                dao.insertUser(
                    UserEntity(
                        id = remote.id,
                        name = remote.name,
                        pin = remote.pin,
                        role = remote.role
                    )
                )
            }
            Log.i("InventoryVM", "Synced ${remoteUsers.size} users from central backend")
        } catch (e: Exception) {
            Log.e("InventoryVM", "Failed to sync central users", e)
        }
    }

    private suspend fun applyUserEvent(event: InventorySyncEventDto) {
        val userId = event.payload["id"] ?: return
        if (event.action == "delete") {
            dao.deleteUser(userId)
            return
        }

        dao.insertUser(
            UserEntity(
                id = userId,
                name = event.payload["name"] ?: return,
                pin = event.payload["pin"] ?: return,
                role = event.payload["role"] ?: "OPERADOR"
            )
        )
    }

    fun saveCatalogModel(model: CatalogModelEntity) {
        viewModelScope.launch {
            val updated = model.copy(updatedAt = System.currentTimeMillis())
            dao.insertCatalogModel(updated)
            val imageData = updated.imageUri?.let { imageUriString ->
                if (imageUriString.startsWith("content://")) {
                    val imageUri = Uri.parse(imageUriString)
                    context.contentResolver.openInputStream(imageUri)?.use { input ->
                        val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                        val encoded = Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
                        "data:$mimeType;base64,$encoded"
                    }
                } else {
                    null
                }
            }

            val event = InventorySyncEventDto(
                eventId = UUID.randomUUID().toString(),
                entityType = "catalog_model",
                action = "upsert",
                payload = mapOf(
                    "id" to updated.id,
                    "code" to updated.code,
                    "name" to updated.name,
                    "sizeMin" to updated.sizeMin.toString(),
                    "sizeMax" to updated.sizeMax.toString(),
                    "pairsPerBox" to updated.pairsPerBox.toString(),
                    "isActive" to updated.isActive.toString(),
                    "imageUri" to (updated.imageUri ?: ""),
                    "imageData" to (imageData ?: ""),
                    "createdAt" to updated.createdAt.toString(),
                    "updatedAt" to updated.updatedAt.toString()
                ),
                userId = currentUser.value?.id ?: "unknown-user",
                deviceId = "android-device-${UUID.randomUUID()}"
            )

            inventorySyncRepository.sendInventoryEvent(event)
            syncCatalogFromServer()
        }
    }

    fun deleteCatalogModel(id: String) {
        viewModelScope.launch {
            dao.deleteCatalogModel(id)

            val event = InventorySyncEventDto(
                eventId = UUID.randomUUID().toString(),
                entityType = "catalog_model",
                action = "delete",
                payload = mapOf("id" to id),
                userId = currentUser.value?.id ?: "unknown-user",
                deviceId = "android-device-${UUID.randomUUID()}"
            )

            inventorySyncRepository.sendInventoryEvent(event)
            syncCatalogFromServer()
        }
    }

    override fun onCleared() {
        inventorySyncRepository.disconnectRealtime()
        super.onCleared()
    }

    fun searchQRByUuid(query: String) {
        qrSearchJob?.cancel()
        qrSearchJob = viewModelScope.launch {
            val normalizedQuery = query.trim().uppercase()
            if (normalizedQuery.isBlank()) {
                _qrSearchResults.value = emptyList()
                return@launch
            }
            delay(250)
            _qrSearchResults.value = dao.searchProductsByUuid(normalizedQuery)
        }
    }

    fun loadMoreEntryMovements() {
        if (_isLoadingEntryMovements.value || !hasMoreEntryMovements) return

        viewModelScope.launch {
            _isLoadingEntryMovements.value = true
            try {
                val pageSize = 50
                val page = dao.getEntryMovements(pageSize, entryMovementsOffset)
                _entryMovements.update { current -> current + page }
                entryMovementsOffset += page.size
                hasMoreEntryMovements = page.size == pageSize
            } finally {
                _isLoadingEntryMovements.value = false
            }
        }
    }

    suspend fun getProductsAtRack(location: String): List<ProductEntity> {
        return dao.getProductsAtLocation(location)
    }

    fun setLockedRack(rack: String?) {
        _lockedRack.value = rack
    }

    fun setRackLocked(locked: Boolean) {
        _isRackLocked.value = locked
    }

    fun setOrigin(origin: Origin) {
        _currentOrigin.value = origin
    }

    fun detectOriginFromUuid(uuid: String) {
        Origin.fromUuid(uuid)?.let { _currentOrigin.value = it }
    }

    fun setCurrentUser(user: UserEntity) {
        _currentUser.value = user
    }

    fun logout() {
        _currentUser.value = null
        _uiState.value = InventoryUiState.Idle
        _currentOrigin.value = Origin.FOOT_SAFE
    }

    fun resetUiState() {
        _uiState.value = InventoryUiState.Idle
    }

    fun syncInventoryChange(
        entityType: String,
        action: String,
        payload: Map<String, Any?>,
        userId: String = currentUser.value?.id ?: "unknown-user"
    ) {
        viewModelScope.launch {
            val event = InventorySyncEventDto(
                eventId = UUID.randomUUID().toString(),
                entityType = entityType,
                action = action,
                payload = payload.mapValues { (_, value) -> value?.toString() ?: "" },
                userId = userId,
                deviceId = "android-device-${UUID.randomUUID()}"
            )
            inventorySyncRepository.sendInventoryEvent(event)
        }
    }

    fun setUiError(message: String) {
        _uiState.value = InventoryUiState.Error(message)
    }
}
