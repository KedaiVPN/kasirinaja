package com.kasirinaja.core.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    const val BASE_URL = "https://api-go-v1.free-account.my.id/api/"
    const val IMAGE_BASE_URL = "https://api-go-v1.free-account.my.id"

    private var tokenProvider: (() -> String?)? = null

    fun initialize(tokenProvider: () -> String?) {
        this.tokenProvider = tokenProvider
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        tokenProvider?.invoke()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val catalogApi: CatalogApi = retrofit.create(CatalogApi::class.java)
    val storeProductApi: StoreProductApi = retrofit.create(StoreProductApi::class.java)
    val productApi: ProductApi = retrofit.create(ProductApi::class.java)
    val adminApi: AdminApi = retrofit.create(AdminApi::class.java)
    val transactionApi: TransactionApi = retrofit.create(TransactionApi::class.java)
    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val storeApi: StoreApi = retrofit.create(StoreApi::class.java)
}
