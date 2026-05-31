package com.kasirinaja.api.catalog

import java.util.UUID

data class CatalogProductResponse(
    val id: UUID,
    val barcode: String,
    val name: String,
    val photoUrl: String?,
    val categoryId: UUID?,
    val brandId: UUID?,
    val unit: String?
)

data class CatalogCategoryResponse(
    val id: UUID,
    val name: String,
    val slug: String
)
