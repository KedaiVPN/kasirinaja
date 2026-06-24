package com.kasirinaja.core.network

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.GET



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


data class TransactionResponse(
    val id: String,
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

data class DashboardStatsResponse(
    val total_revenue: Long,
    val total_transactions: Int,
    val total_products: Int,
    val net_profit: Long,
    val recent_transactions: List<Map<String, Any>>
)

interface TransactionApi {
    @POST("transactions/")
    suspend fun createTransaction(@Body request: TransactionRequest): Map<String, Any>

    @GET("transactions/dashboard")
    suspend fun getDashboardStats(): DashboardStatsResponse

    @GET("transactions/")
    suspend fun getAllTransactions(): retrofit2.Response<List<TransactionResponse>>
}
