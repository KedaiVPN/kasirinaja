package com.kasirinaja.core.network

data class MasterProductRequest(
    val name: String,
    val photo_url: String,
    val photo_path: String = "",
    val category_id: String = "",
    val brand_id: String = "",
    val unit: String = "pcs",
    val source: String = "admin_app",
    val is_generated_barcode: Boolean = false,
    val created_by: String = "",
    val barcode: String
)
