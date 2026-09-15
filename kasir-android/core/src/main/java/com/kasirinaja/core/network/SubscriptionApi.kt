package com.kasirinaja.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

data class SubscriptionPlanDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("price") val price: Long,
    @SerializedName("description") val description: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreatePlanRequest(
    @SerializedName("name") val name: String,
    @SerializedName("duration_days") val durationDays: Int,
    @SerializedName("price") val price: Long,
    @SerializedName("description") val description: String,
    @SerializedName("is_active") val isActive: Boolean
)

data class PlansResponse(
    @SerializedName("plans") val plans: List<SubscriptionPlanDto>,
    @SerializedName("is_pro") val isPro: Boolean = false,
    @SerializedName("pro_expires_at") val proExpiresAt: String? = null
)

data class PlanDetailResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("plan") val plan: SubscriptionPlanDto?
)

data class TripayFeeDto(
    @SerializedName("flat") val flat: Long = 0,
    @SerializedName("percent") val percent: Double = 0.0
)

data class PaymentChannelDto(
    @SerializedName("group") val group: String,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
    @SerializedName("fee_merchant") val feeMerchant: TripayFeeDto?,
    @SerializedName("fee_customer") val feeCustomer: TripayFeeDto?,
    @SerializedName("icon_url") val iconUrl: String?,
    @SerializedName("active") val active: Boolean
)

data class PaymentChannelsResponse(
    @SerializedName("channels") val channels: List<PaymentChannelDto>
)

data class CheckoutRequestDto(
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("payment_method") val paymentMethod: String
)

data class TripayStepDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("steps") val steps: List<String> = emptyList()
)

data class TripayInstructionDto(
    @SerializedName("title") val title: Any? = null, // Can be String or Map
    @SerializedName("steps") val steps: List<String> = emptyList()
)

data class SubscriptionTransactionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("reference") val reference: String,
    @SerializedName("merchant_ref") val merchantRef: String,
    @SerializedName("store_id") val storeId: String,
    @SerializedName("plan_id") val planId: Int,
    @SerializedName("amount") val amount: Long,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_name") val paymentName: String,
    @SerializedName("status") val status: String,
    @SerializedName("pay_code") val payCode: String?,
    @SerializedName("qr_url") val qrUrl: String?,
    @SerializedName("checkout_url") val checkoutUrl: String?,
    @SerializedName("instructions_json") val instructionsJson: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("paid_at") val paidAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("plan_name") val planName: String? = null,
    @SerializedName("duration_days") val durationDays: Int = 0,
    @SerializedName("store_name") val storeName: String? = null
)

data class CheckoutResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("transaction") val transaction: SubscriptionTransactionDto,
    @SerializedName("instructions") val instructions: List<TripayInstructionDto>? = null
)

data class TransactionDetailResponse(
    @SerializedName("transaction") val transaction: SubscriptionTransactionDto
)

data class AdminTransactionsResponse(
    @SerializedName("transactions") val transactions: List<SubscriptionTransactionDto>
)

interface SubscriptionApi {
    // Store Endpoints
    @GET("subscriptions/plans")
    suspend fun getPlans(): Response<PlansResponse>

    @GET("subscriptions/payment-channels")
    suspend fun getPaymentChannels(): Response<PaymentChannelsResponse>

    @POST("subscriptions/checkout")
    suspend fun checkout(@Body request: CheckoutRequestDto): Response<CheckoutResponse>

    @GET("subscriptions/transactions/{reference}")
    suspend fun getTransactionByRef(@Path("reference") reference: String): Response<TransactionDetailResponse>

    // Admin Endpoints
    @GET("admin/subscription-plans")
    suspend fun adminListPlans(): Response<PlansResponse>

    @POST("admin/subscription-plans")
    suspend fun adminCreatePlan(@Body request: CreatePlanRequest): Response<PlanDetailResponse>

    @PUT("admin/subscription-plans/{id}")
    suspend fun adminUpdatePlan(@Path("id") id: Int, @Body request: CreatePlanRequest): Response<PlanDetailResponse>

    @DELETE("admin/subscription-plans/{id}")
    suspend fun adminDeletePlan(@Path("id") id: Int): Response<Map<String, String>>

    @GET("admin/subscription-transactions")
    suspend fun adminListTransactions(): Response<AdminTransactionsResponse>
}
