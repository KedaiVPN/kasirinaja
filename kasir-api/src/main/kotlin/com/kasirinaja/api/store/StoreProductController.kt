package com.kasirinaja.api.store

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/store/products")
class StoreProductController(
    private val storeProductService: StoreProductService
) {

    @GetMapping
    fun getAllProducts(): ResponseEntity<List<StoreProductResponse>> {
        return ResponseEntity.ok(storeProductService.getAllStoreProducts())
    }

    @PostMapping
    fun addProduct(@Valid @RequestBody request: AddStoreProductRequest): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(storeProductService.addStoreProduct(request))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PutMapping("/{id}")
    fun updateProduct(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateStoreProductRequest
    ): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(storeProductService.updateStoreProduct(id, request))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: UUID): ResponseEntity<Any> {
        return try {
            storeProductService.deleteStoreProduct(id)
            ResponseEntity.ok(mapOf("message" to "Product successfully deleted/deactivated"))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}
