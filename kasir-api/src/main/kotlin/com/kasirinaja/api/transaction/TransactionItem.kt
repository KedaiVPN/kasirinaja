package com.kasirinaja.api.transaction

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "transaction_items")
class TransactionItem : BaseEntity() {
    var transactionId: UUID? = null
    var storeProductId: UUID? = null
    var masterProductId: UUID? = null
    var productName: String = ""
    var barcode: String = ""
    var quantity: Int = 0
    var buyPrice: BigDecimal = BigDecimal.ZERO
    var sellPrice: BigDecimal = BigDecimal.ZERO
    var subtotal: BigDecimal = BigDecimal.ZERO
}
