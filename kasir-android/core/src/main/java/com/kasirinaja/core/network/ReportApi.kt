package com.kasirinaja.core.network

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.Response

data class SubmitReportRequest(
    val start_time: String,
    val end_time: String,
    val total_transactions: Int,
    val total_revenue: Long,
    val total_profit: Long
)

data class SubmitReportResponse(
    val message: String,
    val report: Map<String, Any>
)

data class CashierReportDto(
    val id: String,
    val store_id: String,
    val cashier_id: String,
    val cashier_name: String,
    val start_time: String,
    val end_time: String,
    val total_transactions: Int,
    val total_revenue: Long,
    val total_profit: Long,
    val created_at: String
)

data class GetReportsResponse(
    val reports: List<CashierReportDto>
)

data class StockReportDto(
    val id: String,
    val name: String,
    val category: String,
    val stock: Int,
    val min_stock: Int,
    val status: String,
    val is_stock_notification_enabled: Boolean
)

interface ReportApi {
    @POST("reports/")
    suspend fun submitReport(@Body request: SubmitReportRequest): Response<SubmitReportResponse>

    @GET("products/stock-report")
    suspend fun getStockReport(): Response<List<StockReportDto>>

    @GET("reports/")
    suspend fun getStoreReports(): Response<GetReportsResponse>

    @DELETE("reports/{id}")
    suspend fun deleteReport(@Path("id") reportId: String): Response<Unit>
}
