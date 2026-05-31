package com.kasirinaja.api.store

import com.kasirinaja.api.product.StoreProduct
import com.kasirinaja.api.product.StoreProductRepository
import com.kasirinaja.api.user.UserRepository
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StoreProductService(
    private val storeProductRepository: StoreProductRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val userRepository: UserRepository
) {

    private fun getCurrentUserStoreId(): UUID {
        val auth: Authentication = SecurityContextHolder.getContext().authentication ?: throw IllegalStateException("Not authenticated")
        val userId = UUID.fromString(auth.name)
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        return user.storeId ?: throw IllegalArgumentException("User is not associated with any store")
    }

    fun getAllStoreProducts(): List<StoreProductResponse> {
        val storeId = getCurrentUserStoreId()
        return storeProductRepository.findAllByStoreIdAndIsActiveTrue(storeId).map { mapToResponse(it) }
    }

    @Transactional
    fun addStoreProduct(request: AddStoreProductRequest): StoreProductResponse {
        val storeId = getCurrentUserStoreId()

        // Check if already exists
        val existing = storeProductRepository.findByStoreIdAndMasterProductId(storeId, request.masterProductId)
        if (existing != null) {
            throw IllegalArgumentException("Product is already in your store catalog")
        }

        val storeProduct = StoreProduct().apply {
            this.storeId = storeId
            this.masterProductId = request.masterProductId
            this.buyPrice = request.buyPrice
            this.sellPrice = request.sellPrice
            this.stock = request.initialStock
            this.minStock = request.minStock
            this.isActive = true
        }
        val savedProduct = storeProductRepository.save(storeProduct)

        if (request.initialStock > 0) {
            val movement = StockMovement().apply {
                this.storeId = storeId
                this.storeProductId = savedProduct.id
                this.movementType = "IN"
                this.quantity = request.initialStock
                this.referenceType = "INITIAL"
            }
            stockMovementRepository.save(movement)
        }

        return mapToResponse(savedProduct)
    }

    @Transactional
    fun updateStoreProduct(id: UUID, request: UpdateStoreProductRequest): StoreProductResponse {
        val storeId = getCurrentUserStoreId()
        val product = storeProductRepository.findById(id).orElseThrow { IllegalArgumentException("Product not found") }

        if (product.storeId != storeId) {
            throw IllegalArgumentException("You don't have permission to modify this product")
        }

        product.buyPrice = request.buyPrice
        product.sellPrice = request.sellPrice
        product.minStock = request.minStock
        product.isActive = request.isActive

        val updatedProduct = storeProductRepository.save(product)
        return mapToResponse(updatedProduct)
    }

    @Transactional
    fun deleteStoreProduct(id: UUID) {
        val storeId = getCurrentUserStoreId()
        val product = storeProductRepository.findById(id).orElseThrow { IllegalArgumentException("Product not found") }

        if (product.storeId != storeId) {
            throw IllegalArgumentException("You don't have permission to delete this product")
        }

        // Soft delete
        product.isActive = false
        storeProductRepository.save(product)
    }

    private fun mapToResponse(product: StoreProduct): StoreProductResponse {
        return StoreProductResponse(
            id = product.id,
            storeId = product.storeId!!,
            masterProductId = product.masterProductId!!,
            buyPrice = product.buyPrice,
            sellPrice = product.sellPrice,
            stock = product.stock,
            minStock = product.minStock,
            isActive = product.isActive
        )
    }
}
