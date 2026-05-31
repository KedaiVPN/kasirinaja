package com.kasirinaja.api.auth

import com.kasirinaja.api.common.Role
import com.kasirinaja.api.security.JwtTokenProvider
import com.kasirinaja.api.store.Store
import com.kasirinaja.api.store.StoreRepository
import com.kasirinaja.api.user.User
import com.kasirinaja.api.user.UserRepository
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val mailSender: JavaMailSender
) {

    @Transactional
    fun registerStore(request: RegisterStoreRequest) {
        if (userRepository.findByEmail(request.email) != null) {
            throw IllegalArgumentException("Email already exists")
        }

        // 1. Create User
        val user = User().apply {
            this.fullName = request.fullName
            this.email = request.email
            this.phone = request.phone
            this.passwordHash = passwordEncoder.encode(request.passwordHash)!!
            this.role = Role.STORE_OWNER
            this.isActive = false // Not active until email is verified
        }
        val savedUser = userRepository.save(user)

        // 2. Create Store
        val storeCode = generateUniqueStoreCode()
        val store = Store().apply {
            this.ownerId = savedUser.id
            this.storeCode = storeCode
            this.storeName = request.storeName
            this.address = request.address
            this.phone = request.phone
        }
        val savedStore = storeRepository.save(store)

        // 3. Update User with Store ID
        savedUser.storeId = savedStore.id
        userRepository.save(savedUser)

        // 4. Send Verification Email
        val token = jwtTokenProvider.generateToken(savedUser.id.toString(), savedUser.email, savedUser.role.name)
        sendVerificationEmail(savedUser.email, token)
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid email or password")

        if (!passwordEncoder.matches(request.passwordHash, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }

        if (!user.isActive) {
            throw IllegalArgumentException("Account is not active. Please verify your email.")
        }

        val token = jwtTokenProvider.generateToken(user.id.toString(), user.email, user.role.name)

        return AuthResponse(
            token = token,
            role = user.role.name,
            storeId = user.storeId?.toString(),
            fullName = user.fullName
        )
    }

    @Transactional
    fun verifyEmail(token: String) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw IllegalArgumentException("Invalid or expired verification token")
        }

        val authentication = jwtTokenProvider.getAuthentication(token)
        val userId = UUID.fromString(authentication.name)

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User not found")
        }

        user.isActive = true
        userRepository.save(user)
    }

    private fun generateUniqueStoreCode(): String {
        var code: String
        do {
            code = "TKO" + (1000..9999).random()
        } while (storeRepository.existsByStoreCode(code))
        return code
    }

    private fun sendVerificationEmail(toEmail: String, token: String) {
        val message = SimpleMailMessage().apply {
            setTo(toEmail)
            subject = "Verify your KasirinAja Account"
            // For now, print to console / local link
            text = "Click the link to verify your account: http://localhost:8080/api/auth/verify?token=\$token"
        }
        try {
            mailSender.send(message)
        } catch (e: Exception) {
            println("Warning: Failed to send email to \$toEmail. (Token: \$token) Error: \${e.message}")
        }
    }
}
