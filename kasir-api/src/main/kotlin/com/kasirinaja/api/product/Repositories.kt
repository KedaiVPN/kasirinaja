package com.kasirinaja.api.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MasterProductRepository : JpaRepository<MasterProduct, UUID> {
    fun findByBarcodeAndIsActiveTrue(barcode: String): MasterProduct?
    fun findAllByIsActiveTrue(): List<MasterProduct>
}

@Repository
interface CategoryRepository : JpaRepository<Category, UUID> {
    fun findAllByIsActiveTrue(): List<Category>
}

@Repository
interface BrandRepository : JpaRepository<Brand, UUID> {
    fun findAllByIsActiveTrue(): List<Brand>
}

@Repository
interface StoreProductRepository : JpaRepository<StoreProduct, UUID> {
    fun findByStoreIdAndMasterProductId(storeId: UUID, masterProductId: UUID): StoreProduct?
    fun findAllByStoreIdAndIsActiveTrue(storeId: UUID): List<StoreProduct>
}
