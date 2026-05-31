package com.kasirinaja.api.product

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "store_products")
class StoreProduct : BaseEntity() {
    var storeId: UUID? = null
    var masterProductId: UUID? = null
    var buyPrice: BigDecimal = BigDecimal.ZERO
    var sellPrice: BigDecimal = BigDecimal.ZERO
    var stock: Int = 0
    var minStock: Int = 0
}
