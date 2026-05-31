package com.kasirinaja.api.store

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StoreRepository : JpaRepository<Store, UUID> {
    fun existsByStoreCode(storeCode: String): Boolean
}

@Repository
interface StockMovementRepository : JpaRepository<StockMovement, UUID> {
    fun findAllByStoreId(storeId: UUID): List<StockMovement>
}
