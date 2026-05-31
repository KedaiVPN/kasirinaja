package com.kasirinaja.api.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register-store")
    fun registerStore(@RequestBody request: RegisterStoreRequest): ResponseEntity<Any> {
        authService.registerStore(request)
        return ResponseEntity.ok(mapOf("message" to "Store registered. Please check email for verification."))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        // Implementation for dummy login token mapped to mock data
        return ResponseEntity.ok(AuthResponse("dummy-jwt-token", "STORE_OWNER", "store-123", "John Doe"))
    }
}
