package com.kasirinaja.api.catalog

import com.kasirinaja.api.product.CategoryRepository
import com.kasirinaja.api.product.MasterProductRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CatalogService(
    private val masterProductRepository: MasterProductRepository,
    private val categoryRepository: CategoryRepository
) {

    fun getAllActiveMasterProducts(): List<CatalogProductResponse> {
        return masterProductRepository.findAllByIsActiveTrue().map { product ->
            CatalogProductResponse(
                id = product.id,
                barcode = product.barcode,
                name = product.name,
                photoUrl = product.photoUrl,
                categoryId = product.categoryId,
                brandId = product.brandId,
                unit = product.unit
            )
        }
    }

    fun getMasterProductByBarcode(barcode: String): CatalogProductResponse {
        val product = masterProductRepository.findByBarcodeAndIsActiveTrue(barcode)
            ?: throw IllegalArgumentException("Product with barcode \$barcode not found")

        return CatalogProductResponse(
            id = product.id,
            barcode = product.barcode,
            name = product.name,
            photoUrl = product.photoUrl,
            categoryId = product.categoryId,
            brandId = product.brandId,
            unit = product.unit
        )
    }

    fun getMasterProductById(id: UUID): CatalogProductResponse {
        val product = masterProductRepository.findById(id).orElseThrow {
            IllegalArgumentException("Product not found")
        }
        if (!product.isActive) {
            throw IllegalArgumentException("Product is inactive")
        }
        return CatalogProductResponse(
            id = product.id,
            barcode = product.barcode,
            name = product.name,
            photoUrl = product.photoUrl,
            categoryId = product.categoryId,
            brandId = product.brandId,
            unit = product.unit
        )
    }

    fun getAllCategories(): List<CatalogCategoryResponse> {
        return categoryRepository.findAllByIsActiveTrue().map {
            CatalogCategoryResponse(
                id = it.id,
                name = it.name,
                slug = it.slug
            )
        }
    }
}
