package com.kasirinaja.api.transaction

import com.kasirinaja.api.common.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "transactions")
class Transaction : BaseEntity() {
    var storeId: UUID? = null
    var cashierId: UUID? = null
    var invoiceNumber: String = ""
    var totalAmount: BigDecimal = BigDecimal.ZERO
    var paidAmount: BigDecimal = BigDecimal.ZERO
    var changeAmount: BigDecimal = BigDecimal.ZERO
    var paymentMethod: String = ""
    var transactionTime: LocalDateTime = LocalDateTime.now()
    var syncStatus: String = ""
    var deviceId: String? = null
}
