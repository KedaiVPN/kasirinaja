package com.poskedai.core.utils

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ApiErrorParser {

    private val DOMAIN_REGEX = Regex(
        """https?://\S+|([a-zA-Z0-9.-]+\.(free-account\.my\.id|my\.id|com|org|net|io|dev|app|id)(:\d+)?)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Extract a human-readable, user-friendly error message from a throwable.
     * - Inspects the entire cause chain for network / connectivity issues.
     * - Maps network/IO errors to clean Indonesian copy without technical details or hostnames.
     * - Parses backend JSON error response ("error" / "message") if available.
     * - Maps HTTP status codes to friendly Indonesian messages.
     * - Translates common English backend errors into Indonesian.
     * - Strips any backend domains, IP addresses, or URLs that might leak.
     */
    fun parse(throwable: Throwable?): String {
        if (throwable == null) {
            return "Terjadi kesalahan. Silakan coba lagi."
        }

        // 1. Check if ANY cause in the chain is a network / connectivity exception
        if (isNetworkIssue(throwable)) {
            return "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
        }

        // 2. Check for HttpException directly or in cause chain
        val httpException = findCause<HttpException>(throwable)
        if (httpException != null) {
            return parseHttpException(httpException)
        }

        // 3. Inspect raw message
        val rawMessage = throwable.message ?: return "Terjadi kesalahan. Silakan coba lagi."

        // Check if raw message indicates network/host resolution failure
        if (containsNetworkFailureKeywords(rawMessage)) {
            return "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
        }

        // Check if raw message is a JSON string
        val extractedJson = extractJsonMessage(rawMessage)
        if (!extractedJson.isNullOrBlank() && extractedJson != rawMessage) {
            return translateAndSanitize(extractedJson)
        }

        return translateAndSanitize(rawMessage)
    }

    private fun isNetworkIssue(throwable: Throwable): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is UnknownHostException ||
                current is SocketTimeoutException ||
                current is ConnectException ||
                current is SSLException ||
                current is IOException) {
                return true
            }
            val msg = current.message.orEmpty()
            if (containsNetworkFailureKeywords(msg)) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun containsNetworkFailureKeywords(msg: String): Boolean {
        return msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("No address associated with hostname", ignoreCase = true) ||
                msg.contains("Failed to connect", ignoreCase = true) ||
                msg.contains("Connection refused", ignoreCase = true) ||
                msg.contains("Network is unreachable", ignoreCase = true) ||
                msg.contains("Software caused connection abort", ignoreCase = true) ||
                msg.contains("Connection reset", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true)
    }

    private inline fun <reified T : Throwable> findCause(throwable: Throwable): T? {
        var current: Throwable? = throwable
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    private fun parseHttpException(httpException: HttpException): String {
        val response = httpException.response()
        val code = httpException.code()

        val serverMessage = response?.errorBody()?.string()?.let { raw ->
            extractJsonMessage(raw)
        }

        if (!serverMessage.isNullOrBlank()) {
            return translateAndSanitize(serverMessage)
        }

        return fromStatusCode(code)
    }

    /**
     * Map a response code and optional raw errorBody to a friendly Indonesian error message.
     */
    fun fromResponse(code: Int, errorBodyString: String? = null): String {
        val serverMessage = extractJsonMessage(errorBodyString)
        if (!serverMessage.isNullOrBlank()) {
            return translateAndSanitize(serverMessage)
        }
        return fromStatusCode(code)
    }

    fun fromStatusCode(code: Int): String {
        return when (code) {
            400 -> "Permintaan tidak valid. Periksa data yang Anda masukkan."
            401 -> "Email, username, atau password salah. Silakan periksa kembali."
            403 -> "Akses ditolak. Anda tidak memiliki izin untuk melakukan ini."
            404 -> "Data tidak ditemukan."
            408 -> "Waktu permintaan habis. Periksa koneksi internet Anda."
            409 -> "Data sudah ada atau terjadi konflik."
            422 -> "Data yang dikirim tidak dapat diproses."
            429 -> "Terlalu banyak permintaan. Silakan tunggu beberapa saat lagi."
            500 -> "Terjadi gangguan pada server. Silakan coba lagi nanti."
            502 -> "Server sedang tidak dapat dijangkau. Silakan coba lagi nanti."
            503 -> "Layanan sedang dalam pemeliharaan. Silakan coba lagi nanti."
            504 -> "Server membutuhkan waktu terlalu lama untuk merespons."
            in 500..599 -> "Server sedang bermasalah. Silakan coba lagi nanti."
            else -> "Terjadi kesalahan. Silakan coba lagi."
        }
    }

    fun extractJsonMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(raw).asJsonObject
            val error = json.get("error")?.takeIf { !it.isJsonNull }?.asString
            val message = json.get("message")?.takeIf { !it.isJsonNull }?.asString
            (error ?: message)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            raw.takeIf { it.isNotBlank() && !it.trim().startsWith("<") }
        }
    }

    fun fromErrorBody(raw: String?, fallback: String): String {
        val msg = extractJsonMessage(raw)
        return if (!msg.isNullOrBlank()) translateAndSanitize(msg) else fallback
    }

    /**
     * Translate known English backend phrases to friendly Indonesian
     * and strip any internal hostnames/URLs.
     */
    fun translateAndSanitize(message: String): String {
        var clean = message.trim()

        // Translate common backend errors to friendly Indonesian
        clean = when {
            clean.equals("Invalid email or password", ignoreCase = true) ->
                "Email, username, atau password salah."
            clean.equals("Account is inactive", ignoreCase = true) ->
                "Akun ini tidak aktif. Silakan hubungi pemilik toko."
            clean.equals("Invalid request parameters", ignoreCase = true) ->
                "Data yang dimasukkan tidak valid."
            clean.equals("Invalid OTP", ignoreCase = true) ->
                "Kode OTP salah atau tidak valid."
            clean.equals("Invalid password", ignoreCase = true) ->
                "Password yang Anda masukkan salah."
            clean.equals("Invalid store context", ignoreCase = true) ->
                "Konteks toko tidak valid."
            clean.equals("Invalid user context", ignoreCase = true) ->
                "Konteks pengguna tidak valid."
            clean.equals("unauthorized", ignoreCase = true) ||
            clean.equals("Unauthorized", ignoreCase = true) ->
                "Sesi login Anda telah berakhir. Silakan login kembali."
            clean.equals("Token not found in response", ignoreCase = true) ->
                "Gagal mendapatkan token login dari server."
            clean.contains("Unable to resolve host", ignoreCase = true) ||
            clean.contains("No address associated with hostname", ignoreCase = true) ||
            clean.contains("Failed to connect", ignoreCase = true) ->
                "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
            else -> clean
        }

        // Strip internal domains/URLs
        clean = DOMAIN_REGEX.replace(clean, "server")
        clean = clean.replace("\"server\"", "server")
        return clean.trim()
    }
}
