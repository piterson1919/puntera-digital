package com.punteradigital.inventory.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.punteradigital.inventory.data.local.InventoryDatabase
import com.punteradigital.inventory.data.local.dao.InventoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migration v5 → v6: Creates the `labels` table for the Empaque (Pre-Entry) module.
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `labels` (
                    `uuid` TEXT NOT NULL PRIMARY KEY,
                    `batchId` TEXT NOT NULL,
                    `origin` TEXT NOT NULL,
                    `model` TEXT NOT NULL,
                    `size` TEXT NOT NULL,
                    `lot` TEXT NOT NULL,
                    `labelType` TEXT NOT NULL,
                    `labelFormat` TEXT NOT NULL,
                    `parentLabelUuid` TEXT,
                    `status` TEXT NOT NULL DEFAULT 'CREATED',
                    `createdBy` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `printedAt` INTEGER,
                    `enteredAt` INTEGER,
                    `enteredBy` TEXT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_status` ON `labels` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_model_size` ON `labels` (`model`, `size`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_batchId` ON `labels` (`batchId`)")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_labels_parentLabelUuid` ON `labels` (`parentLabelUuid`)")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_products_status_model_size`")
            db.execSQL("DROP INDEX IF EXISTS `index_products_status_location`")
            db.execSQL("DROP INDEX IF EXISTS `index_movements_timestamp`")
            db.execSQL("DROP INDEX IF EXISTS `index_sync_queue_status_createdAt`")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_products_status_model_size`")
            db.execSQL("DROP INDEX IF EXISTS `index_products_status_location`")
            db.execSQL("DROP INDEX IF EXISTS `index_products_origin_status`")
            db.execSQL("DROP INDEX IF EXISTS `index_products_parentUuid`")
            db.execSQL("DROP INDEX IF EXISTS `index_products_status`")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `catalog_models` (
                    `id` TEXT NOT NULL,
                    `code` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sizeMin` INTEGER NOT NULL,
                    `sizeMax` INTEGER NOT NULL,
                    `pairsPerBox` INTEGER NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `imageUri` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_catalog_models_code` ON `catalog_models` (`code`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_catalog_models_isActive` ON `catalog_models` (`isActive`)")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_origin_status` ON `products` (`origin`, `status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_parentUuid` ON `products` (`parentUuid`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_status` ON `products` (`status`)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InventoryDatabase {
        return Room.databaseBuilder(
            context,
            InventoryDatabase::class.java,
            "inventory_db"
        )
            .addMigrations(MIGRATION_5_6)
            .addMigrations(MIGRATION_6_7)
            .addMigrations(MIGRATION_7_8)
            .addMigrations(MIGRATION_8_9)
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
            .build()
    }

    @Provides
    fun provideInventoryDao(database: InventoryDatabase): InventoryDao {
        return database.inventoryDao()
    }
}
