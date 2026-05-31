package com.kasirinaja.api.catalog

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/catalog")
class CatalogController(
    private val catalogService: CatalogService
) {

    @GetMapping("/products")
    fun getAllProducts(): ResponseEntity<List<CatalogProductResponse>> {
        return ResponseEntity.ok(catalogService.getAllActiveMasterProducts())
    }

    @GetMapping("/products/{id}")
    fun getProductById(@PathVariable id: UUID): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(catalogService.getMasterProductById(id))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/products/barcode/{barcode}")
    fun getProductByBarcode(@PathVariable barcode: String): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(catalogService.getMasterProductByBarcode(barcode))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/categories")
    fun getAllCategories(): ResponseEntity<List<CatalogCategoryResponse>> {
        return ResponseEntity.ok(catalogService.getAllCategories())
    }
}
