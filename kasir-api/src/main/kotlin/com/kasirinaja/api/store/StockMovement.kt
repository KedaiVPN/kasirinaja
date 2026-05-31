package com.kasirinaja.api.store

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "stock_movements")
class StockMovement : BaseEntity() {
    var storeId: UUID? = null
    var storeProductId: UUID? = null
    var movementType: String = "" // IN, OUT, SALE, RETURN, ADJUSTMENT
    var quantity: Int = 0
    var referenceType: String? = null // e.g. TRANSACTION
    var referenceId: UUID? = null
}
