package com.kasirinaja.core.network

import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.GET
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

interface ReportApi {
    @POST("reports/")
    suspend fun submitReport(@Body request: SubmitReportRequest): Response<SubmitReportResponse>

    @GET("reports/")
    suspend fun getStoreReports(): Response<GetReportsResponse>
}
