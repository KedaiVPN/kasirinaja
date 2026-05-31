package com.kasirinaja.api.auth

import org.springframework.stereotype.Service

@Service
class AuthService {

    fun registerStore(request: RegisterStoreRequest) {
        // Implement store registration logic
        println("Registering store for ${request.email}")
    }

    fun verifyEmail(token: String) {
        // Implement email verification logic
        println("Verifying email with token $token")
    }
}
