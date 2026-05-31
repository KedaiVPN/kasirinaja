package com.kasirinaja.api.transaction

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TransactionRepository : JpaRepository<Transaction, UUID> {
    fun findByInvoiceNumber(invoiceNumber: String): Transaction?
    fun findAllByStoreId(storeId: UUID): List<Transaction>
}

@Repository
interface TransactionItemRepository : JpaRepository<TransactionItem, UUID> {
    fun findAllByTransactionId(transactionId: UUID): List<TransactionItem>
}
