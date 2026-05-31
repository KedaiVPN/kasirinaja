package com.kasirinaja.api.store

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class AddStoreProductRequest(
    @field:NotNull val masterProductId: UUID,
    @field:Min(0) val buyPrice: BigDecimal,
    @field:Min(0) val sellPrice: BigDecimal,
    @field:Min(0) val initialStock: Int,
    @field:Min(0) val minStock: Int
)

data class UpdateStoreProductRequest(
    @field:Min(0) val buyPrice: BigDecimal,
    @field:Min(0) val sellPrice: BigDecimal,
    @field:Min(0) val minStock: Int,
    val isActive: Boolean
)

data class StoreProductResponse(
    val id: UUID,
    val storeId: UUID,
    val masterProductId: UUID,
    val buyPrice: BigDecimal,
    val sellPrice: BigDecimal,
    val stock: Int,
    val minStock: Int,
    val isActive: Boolean
)
