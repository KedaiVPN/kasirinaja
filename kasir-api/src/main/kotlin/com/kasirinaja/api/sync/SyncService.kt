package com.kasirinaja.api.sync

import com.kasirinaja.api.product.StoreProductRepository
import com.kasirinaja.api.store.StockMovement
import com.kasirinaja.api.store.StockMovementRepository
import com.kasirinaja.api.transaction.Transaction
import com.kasirinaja.api.transaction.TransactionItem
import com.kasirinaja.api.transaction.TransactionItemRepository
import com.kasirinaja.api.transaction.TransactionRepository
import com.kasirinaja.api.user.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SyncService(
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val storeProductRepository: StoreProductRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val userRepository: UserRepository
) {

    private fun getCurrentUserStoreId(): UUID {
        val auth = SecurityContextHolder.getContext().authentication ?: throw IllegalStateException("Not authenticated")
        val userId = UUID.fromString(auth.name)
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        return user.storeId ?: throw IllegalArgumentException("User is not associated with any store")
    }

    @Transactional
    fun pushTransactions(request: SyncPushRequest): SyncResponse {
        val storeId = getCurrentUserStoreId()
        val syncedIds = mutableListOf<UUID>()
        val failedIds = mutableListOf<UUID>()

        for (txReq in request.transactions) {
            try {
                // Ensure request belongs to the authenticated store
                if (txReq.storeId != storeId) {
                    throw IllegalArgumentException("Store ID mismatch")
                }

                // Check if already synced
                if (transactionRepository.findById(txReq.id).isPresent) {
                    syncedIds.add(txReq.id)
                    continue
                }

                // 1. Save Transaction
                val transaction = Transaction().apply {
                    this.id = txReq.id
                    this.storeId = txReq.storeId
                    this.cashierId = txReq.cashierId
                    this.invoiceNumber = txReq.invoiceNumber
                    this.totalAmount = txReq.totalAmount
                    this.paidAmount = txReq.paidAmount
                    this.changeAmount = txReq.changeAmount
                    this.paymentMethod = txReq.paymentMethod
                    this.transactionTime = txReq.transactionTime
                    this.syncStatus = "SYNCED"
                    this.deviceId = txReq.deviceId
                }
                transactionRepository.save(transaction)

                // 2. Save Items & Update Stock
                for (itemReq in txReq.items) {
                    val txItem = TransactionItem().apply {
                        this.id = itemReq.id
                        this.transactionId = transaction.id
                        this.storeProductId = itemReq.storeProductId
                        this.masterProductId = itemReq.masterProductId
                        this.productName = itemReq.productName
                        this.barcode = itemReq.barcode
                        this.quantity = itemReq.quantity
                        this.buyPrice = itemReq.buyPrice
                        this.sellPrice = itemReq.sellPrice
                        this.subtotal = itemReq.subtotal
                    }
                    transactionItemRepository.save(txItem)

                    // Decrease Stock in Store Product
                    val storeProduct = storeProductRepository.findById(itemReq.storeProductId)
                        .orElseThrow { IllegalArgumentException("Store product not found: \${itemReq.storeProductId}") }

                    storeProduct.stock -= itemReq.quantity
                    storeProductRepository.save(storeProduct)

                    // Add Stock Movement
                    val movement = StockMovement().apply {
                        this.storeId = storeId
                        this.storeProductId = storeProduct.id
                        this.movementType = "SALE"
                        this.quantity = -itemReq.quantity // negative for sales
                        this.referenceType = "TRANSACTION"
                        this.referenceId = transaction.id
                    }
                    stockMovementRepository.save(movement)
                }

                syncedIds.add(txReq.id)
            } catch (e: Exception) {
                // Ignore failure for individual transaction to continue loop, just track failure
                failedIds.add(txReq.id)
            }
        }

        return SyncResponse(
            success = failedIds.isEmpty(),
            syncedTransactionIds = syncedIds,
            failedTransactionIds = failedIds,
            message = "Sync completed. Synced: \${syncedIds.size}, Failed: \${failedIds.size}"
        )
    }
}
