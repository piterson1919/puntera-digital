package com.punteradigital.inventory.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.punteradigital.inventory.data.local.dao.InventoryDao
import com.punteradigital.inventory.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class,
        CatalogModelEntity::class,
        MasterBoxEntity::class,
        MovementEntity::class,
        SyncQueueItem::class,
        UserEntity::class,
        LabelEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao
}
