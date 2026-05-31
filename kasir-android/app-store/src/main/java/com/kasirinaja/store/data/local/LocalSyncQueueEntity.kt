package com.kasirinaja.store.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class LocalSyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payload: String,
    val status: String,
    val retryCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)
