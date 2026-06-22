package com.kasirinaja.core.network

import retrofit2.http.Body
import retrofit2.http.POST

data class TransactionItemRequest(
    val store_product_id: String,
    val master_product_id: String,
    val product_name: String,
    val barcode: String,
    val quantity: Int,
    val buy_price: Long,
    val sell_price: Long,
    val subtotal: Long
)

data class TransactionRequest(
    val store_id: String,
    val cashier_id: String,
    val invoice_number: String,
    val total_amount: Long,
    val paid_amount: Long,
    val change_amount: Long,
    val payment_method: String,
    val transaction_time: String,
    val sync_status: String,
    val device_id: String,
    val items: List<TransactionItemRequest>
)

interface TransactionApi {
    @POST("transactions/")
    suspend fun createTransaction(@Body request: TransactionRequest): Map<String, Any>
}
