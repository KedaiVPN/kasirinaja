package com.poskedai.core.utils

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ApiErrorParser {

    /**
     * Extract a human-readable error message from a throwable.
     * - Prefers the backend's "error" or "message" JSON field when available.
     * - Falls back to friendly status-code messages for common HTTP errors.
     * - Maps network/IO errors to Indonesian user-facing copy.
     */
    fun parse(throwable: Throwable?): String {
        if (throwable == null) {
            return "Terjadi kesalahan. Silakan coba lagi."
        }

        if (throwable is HttpException) {
            return parseHttpException(throwable)
        }

        return when (throwable) {
            is UnknownHostException,
            is SocketTimeoutException,
            is IOException -> "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
            else -> throwable.message ?: "Terjadi kesalahan. Silakan coba lagi."
        }
    }

    private fun parseHttpException(httpException: HttpException): String {
        val response = httpException.response()
        val code = httpException.code()

        val serverMessage = response?.errorBody()?.string()?.let { raw ->
            extractJsonMessage(raw)
        }

        if (!serverMessage.isNullOrBlank()) {
            return serverMessage
        }

        return when (code) {
            400 -> "Permintaan tidak valid. Periksa data yang Anda masukkan."
            401 -> "Email, username, atau password salah. Silakan periksa kembali."
            403 -> "Akses ditolak. Anda tidak memiliki izin untuk melakukan ini."
            404 -> "Data tidak ditemukan."
            409 -> "Data sudah ada atau terjadi konflik."
            429 -> "Terlalu banyak percobaan. Silakan tunggu sebentar dan coba lagi."
            in 500..599 -> "Server sedang bermasalah. Silakan coba lagi nanti."
            else -> "Terjadi kesalahan (HTTP $code). Silakan coba lagi."
        }
    }

    /**
     * Parse a raw error-body string (e.g. {"error":"..."}) and extract the
     * "error" or "message" field. Returns null when the string is blank or
     * not recognizable JSON with either field.
     */
    fun extractJsonMessage(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(raw).asJsonObject
            val error = json.get("error")?.takeIf { !it.isJsonNull }?.asString
            val message = json.get("message")?.takeIf { !it.isJsonNull }?.asString
            (error ?: message)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            raw.takeIf { it.isNotBlank() }
        }
    }

    /**
     * Parse a raw error-body string with a fallback when nothing useful can be extracted.
     */
    fun fromErrorBody(raw: String?, fallback: String): String {
        return extractJsonMessage(raw) ?: fallback
    }
}
