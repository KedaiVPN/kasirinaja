package com.kasirinaja.api.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class RegisterStoreRequest(
    @field:NotBlank val fullName: String,
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val phone: String,
    @field:NotBlank val passwordHash: String,
    @field:NotBlank val storeName: String,
    val address: String? = null
)

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val passwordHash: String
)

data class AuthResponse(
    val token: String,
    val role: String,
    val storeId: String?,
    val fullName: String
)
