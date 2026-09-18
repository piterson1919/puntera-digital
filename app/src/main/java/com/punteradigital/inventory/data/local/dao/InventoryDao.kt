package com.punteradigital.inventory.data.local.dao

import androidx.room.*
import com.punteradigital.inventory.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // ═══════════════════════════════════════════════════════════════
    // PRODUCTS (Individual Units)
    // ═══════════════════════════════════════════════════════════════
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("SELECT * FROM products WHERE uuid = :uuid LIMIT 1")
    suspend fun getProductByUuid(uuid: String): ProductEntity?

    @Query("SELECT * FROM products WHERE parentUuid = :parentUuid")
    suspend fun getChildrenOfMasterBox(parentUuid: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogModel(model: CatalogModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalogModels(models: List<CatalogModelEntity>)

    @Query("DELETE FROM catalog_models")
    suspend fun clearCatalogModels()

    @Query("SELECT * FROM catalog_models ORDER BY name ASC")
    fun getCatalogModels(): Flow<List<CatalogModelEntity>>

    @Query("SELECT * FROM catalog_models WHERE id = :id LIMIT 1")
    suspend fun getCatalogModelById(id: String): CatalogModelEntity?

    @Query("DELETE FROM catalog_models WHERE id = :id")
    suspend fun deleteCatalogModel(id: String)

    @Query("SELECT COUNT(*) FROM catalog_models")
    suspend fun getCatalogModelCount(): Int

    @Query("UPDATE products SET status = :status, location = :location, updatedAt = :timestamp WHERE uuid = :uuid")
    suspend fun updateProductStatus(uuid: String, status: String, location: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE products SET status = :status, location = :location, updatedAt = :timestamp WHERE uuid = :uuid AND status = :expectedStatus")
    suspend fun updateProductStatusIfCurrent(uuid: String, expectedStatus: String, status: String, location: String, timestamp: Long = System.currentTimeMillis()): Int

    @Transaction
    suspend fun dispatchProduct(movement: MovementEntity): Boolean {
        val updated = updateProductStatusIfCurrent(
            uuid = movement.uuid,
            expectedStatus = "STB",
            status = "DISPATCHED",
            location = "DESPACHADO",
            timestamp = movement.timestamp
        )
        if (updated != 1) return false
        insertMovement(movement)
        return true
    }

    @Query("UPDATE products SET status = :status, location = :location, updatedAt = :timestamp WHERE parentUuid = :parentUuid AND status = 'AVAILABLE'")
    suspend fun updateChildrenStatus(parentUuid: String, status: String, location: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM products WHERE status = 'AVAILABLE'")
    fun getTotalAvailableCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE status = 'AVAILABLE' AND origin = :origin")
    fun getAvailableCountByOrigin(origin: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE status = 'STB'")
    fun getTotalStandByCount(): Flow<Int>

    @Query("""
        SELECT lot as batch, model, size, COUNT(*) as count 
        FROM products 
        WHERE status = 'AVAILABLE' 
        GROUP BY lot, model, size
    """)
    fun getInventoryStatusByBatch(): Flow<List<BatchStatus>>

    // getStatusGrouped was removed — unused dead code

    // ═══════════════════════════════════════════════════════════════
    // MASTER BOXES
    // ═══════════════════════════════════════════════════════════════
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterBox(box: MasterBoxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasterBoxes(boxes: List<MasterBoxEntity>)

    @Query("SELECT * FROM master_boxes WHERE uuid = :uuid LIMIT 1")
    suspend fun getMasterBoxByUuid(uuid: String): MasterBoxEntity?

    @Query("UPDATE master_boxes SET activeChildCount = :count, isComplete = :isComplete WHERE uuid = :uuid")
    suspend fun updateMasterBoxChildCount(uuid: String, count: Int, isComplete: Boolean)

    @Query("UPDATE master_boxes SET activeChildCount = :count, isComplete = :isComplete, status = :status WHERE uuid = :uuid")
    suspend fun updateMasterBoxFull(uuid: String, count: Int, isComplete: Boolean, status: String)

    @Query("SELECT COUNT(*) FROM master_boxes")
    fun getTotalMasterBoxCount(): Flow<Int>

    @Query("SELECT * FROM master_boxes WHERE isComplete = 0 OR status = 'PENDIENTE_POR_RELLENAR'")
    fun getIncompleteMasterBoxes(): Flow<List<MasterBoxEntity>>

    /** Smart Entry: find incomplete boxes matching model+size+origin */
    @Query("""
        SELECT * FROM master_boxes 
        WHERE model = :model AND size = :size AND origin = :origin 
        AND (isComplete = 0 OR status = 'PENDIENTE_POR_RELLENAR')
        ORDER BY activeChildCount DESC
    """)
    suspend fun findCompatibleIncompleteBoxes(model: String, size: String, origin: String): List<MasterBoxEntity>

    // ═══════════════════════════════════════════════════════════════
    // MOVEMENTS (Traceability)
    // ═══════════════════════════════════════════════════════════════
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: MovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<MovementEntity>)

    /** Recent movements for HomeScreen — avoids loading entire table */
    @Query("SELECT * FROM movements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMovements(limit: Int = 20): Flow<List<MovementEntity>>

    /** Full movement history — limited to 1000 for UI performance */
    @Query("SELECT * FROM movements ORDER BY timestamp DESC LIMIT 1000")
    fun getAllMovements(): Flow<List<MovementEntity>>

    /** Movements for the last N days */
    @Query("SELECT * FROM movements WHERE timestamp >= :timestamp ORDER BY timestamp DESC")
    fun getMovementsSince(timestamp: Long): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements ORDER BY timestamp DESC LIMIT 5000")
    suspend fun getAllMovementsSync(): List<MovementEntity>

    @Query("SELECT * FROM movements WHERE uuid = :uuid ORDER BY timestamp DESC")
    suspend fun getMovementsByUuid(uuid: String): List<MovementEntity>

    // ═══════════════════════════════════════════════════════════════
    // STAND-BY / DISPATCH QUERIES
    // ═══════════════════════════════════════════════════════════════
    @Query("SELECT * FROM products WHERE status = 'STB' ORDER BY updatedAt DESC")
    fun getStandByProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE status = 'MUESTRA' ORDER BY updatedAt DESC")
    fun getMuestrasActivas(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE parentUuid IS NULL AND status = 'AVAILABLE' AND model = :model AND size = :size")
    suspend fun getAvailableLooseProducts(model: String, size: String): List<ProductEntity>

    // ═══════════════════════════════════════════════════════════════
    // SYNC QUEUE (Offline-First)
    // ═══════════════════════════════════════════════════════════════
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueItem)

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') AND retryCount < 5 ORDER BY createdAt ASC LIMIT 100")
    suspend fun getPendingSyncItems(): List<SyncQueueItem>

    @Query("UPDATE sync_queue SET status = 'SYNCED', syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: Int, syncedAt: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'FAILED', retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markFailed(id: Int)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingSyncCount(): Flow<Int>

    // ═══════════════════════════════════════════════════════════════
    // USERS
    // ═══════════════════════════════════════════════════════════════
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE pin = :pin LIMIT 1")
    suspend fun getUserByPin(pin: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
    suspend fun getAdminCount(): Int

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS (Traceability Intelligence)
    // ═══════════════════════════════════════════════════════════════

    /** Top models ranked by dispatched count */
    @Query("SELECT model, COUNT(*) as count FROM products WHERE status = 'DISPATCHED' GROUP BY model ORDER BY count DESC")
    fun getTopDispatchedModels(): Flow<List<ModelCount>>

    /** Top sizes ranked by dispatched count */
    @Query("SELECT size, COUNT(*) as count FROM products WHERE status = 'DISPATCHED' GROUP BY size ORDER BY count DESC")
    fun getTopDispatchedSizes(): Flow<List<SizeCount>>

    /** Top clients ranked by dispatch order volume */
    @Query("SELECT cliente, COUNT(*) as count FROM movements WHERE type = 'OUT' AND cliente != '' GROUP BY cliente ORDER BY count DESC")
    fun getTopClients(): Flow<List<ClientCount>>

    /** Inventory breakdown: model × status matrix */
    @Query("SELECT model, status, COUNT(*) as count FROM products GROUP BY model, status")
    fun getModelStatusBreakdown(): Flow<List<ModelStatusBreakdown>>

    /** All models with available (in-stock) count */
    @Query("SELECT model, COUNT(*) as count FROM products WHERE status = 'AVAILABLE' GROUP BY model ORDER BY count DESC")
    fun getAllModelsInventory(): Flow<List<ModelCount>>

    /** All sizes with available count, ordered numerically */
    @Query("SELECT size, COUNT(*) as count FROM products WHERE status = 'AVAILABLE' GROUP BY size ORDER BY CAST(size AS INTEGER)")
    fun getAllSizesInventory(): Flow<List<SizeCount>>

    /** Total dispatched products */
    @Query("SELECT COUNT(*) FROM products WHERE status = 'DISPATCHED'")
    fun getTotalDispatchedCount(): Flow<Int>

    /** Total movement records */
    @Query("SELECT COUNT(*) FROM movements")
    fun getTotalMovementCount(): Flow<Int>

    // ═══════════════════════════════════════════════════════════════
    // RACK MAP QUERIES
    // ═══════════════════════════════════════════════════════════════

    /** Count of available products at each location */
    @Query("SELECT location, COUNT(*) as count FROM products WHERE status = 'AVAILABLE' GROUP BY location")
    fun getProductCountByLocation(): Flow<List<LocationCount>>

    /** Products at a specific rack location */
    @Query("SELECT * FROM products WHERE location = :location AND status = 'AVAILABLE' ORDER BY model, size")
    suspend fun getProductsAtLocation(location: String): List<ProductEntity>

    // ═══════════════════════════════════════════════════════════════
    // QR HISTORY QUERIES
    // ═══════════════════════════════════════════════════════════════

    /** Search products by partial UUID */
    @Query("SELECT * FROM products WHERE uuid LIKE :query || '%' ORDER BY createdAt DESC LIMIT 50")
    suspend fun searchProductsByUuid(query: String): List<ProductEntity>

    /** Get a page of entry movements for QR history. */
    @Query("SELECT * FROM movements WHERE type = 'IN' ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getEntryMovements(limit: Int, offset: Int): List<MovementEntity>

    // ═══════════════════════════════════════════════════════════════
    // LABELS / EMPAQUE (Pre-Entry) QUERIES
    // ═══════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabel(label: LabelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabels(labels: List<LabelEntity>)

    @Query("SELECT * FROM labels WHERE status = :status ORDER BY createdAt DESC")
    fun getLabelsByStatus(status: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE batchId = :batchId ORDER BY uuid ASC")
    fun getLabelsByBatch(batchId: String): Flow<List<LabelEntity>>

    @Query("SELECT * FROM labels WHERE batchId = :batchId AND status NOT IN ('ENTERED', 'DELETED')")
    suspend fun getPendingLabelsByBatchSync(batchId: String): List<LabelEntity>

    @Query("SELECT * FROM labels WHERE uuid = :uuid LIMIT 1")
    suspend fun getLabelByUuid(uuid: String): LabelEntity?

    @Query("SELECT * FROM labels WHERE parentLabelUuid = :parentUuid")
    suspend fun getChildrenLabels(parentUuid: String): List<LabelEntity>

    @Query("UPDATE labels SET status = 'PRINTED', printedAt = :timestamp WHERE uuid = :uuid AND status = 'CREATED'")
    suspend fun markLabelPrinted(uuid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE labels SET status = 'ENTERED', enteredAt = :timestamp, enteredBy = :userId WHERE uuid = :uuid")
    suspend fun markLabelEntered(uuid: String, userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE labels SET status = 'ENTERED', enteredAt = :timestamp, enteredBy = :userId WHERE batchId = :batchId AND status NOT IN ('ENTERED', 'DELETED')")
    suspend fun markBatchEntered(batchId: String, userId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE labels SET status = 'DELETED' WHERE batchId = :batchId AND status != 'ENTERED'")
    suspend fun deleteLabelBatch(batchId: String)

    @Query("UPDATE labels SET status = 'DELETED' WHERE uuid = :uuid AND status != 'ENTERED'")
    suspend fun deleteLabel(uuid: String)

    @Query("""
        SELECT batchId, model, size, lot, labelType, labelFormat, origin, createdBy, MIN(createdAt) as createdAt,
               COUNT(*) as totalCount,
               SUM(CASE WHEN status = 'PRINTED' THEN 1 ELSE 0 END) as printedCount,
               SUM(CASE WHEN status = 'ENTERED' THEN 1 ELSE 0 END) as enteredCount
        FROM labels 
        WHERE status != 'DELETED'
        GROUP BY batchId 
        ORDER BY createdAt DESC 
        LIMIT 100
    """)
    fun getLabelBatchSummaries(): Flow<List<LabelBatchSummary>>

    @Query("SELECT COUNT(*) FROM labels WHERE status IN ('CREATED', 'PRINTED')")
    fun getPendingLabelCount(): Flow<Int>
}

data class BatchStatus(
    val batch: String,
    val model: String,
    val size: String,
    val count: Int
)

data class LocationCount(
    val location: String,
    val count: Int
)
