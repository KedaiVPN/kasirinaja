package com.kasirinaja.store.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalStoreProductEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KasirDatabase : RoomDatabase() {
    // abstract fun productDao(): ProductDao
}
