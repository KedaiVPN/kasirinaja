package com.kasirinaja.api.sync

import com.kasirinaja.api.transaction.Transaction
import com.kasirinaja.api.transaction.TransactionItem
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class SyncTransactionRequest(
    @field:NotNull val id: UUID,
    @field:NotNull val storeId: UUID,
    @field:NotNull val cashierId: UUID,
    val invoiceNumber: String,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val changeAmount: BigDecimal,
    val paymentMethod: String,
    val transactionTime: LocalDateTime,
    val deviceId: String?,
    @field:NotEmpty val items: List<SyncTransactionItemRequest>
)

data class SyncTransactionItemRequest(
    @field:NotNull val id: UUID,
    @field:NotNull val storeProductId: UUID,
    @field:NotNull val masterProductId: UUID,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val buyPrice: BigDecimal,
    val sellPrice: BigDecimal,
    val subtotal: BigDecimal
)

data class SyncPushRequest(
    val transactions: List<SyncTransactionRequest>
)

data class SyncResponse(
    val success: Boolean,
    val syncedTransactionIds: List<UUID> = emptyList(),
    val failedTransactionIds: List<UUID> = emptyList(),
    val message: String? = null
)
