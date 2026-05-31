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
        return try {
            authService.registerStore(request)
            ResponseEntity.ok(mapOf("message" to "Store registered. Please check email for verification."))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<Any> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }

    @GetMapping("/verify")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<Any> {
        return try {
            authService.verifyEmail(token)
            ResponseEntity.ok(mapOf("message" to "Email verified successfully. You can now login."))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        }
    }
}
