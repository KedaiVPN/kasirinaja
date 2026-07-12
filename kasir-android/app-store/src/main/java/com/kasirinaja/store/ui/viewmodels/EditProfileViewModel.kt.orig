package com.kasirinaja.store.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

data class EditProfileState(
    val fullName: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class EditProfileViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    init {
        _state.update {
            it.copy(
                fullName = tokenManager.getUserName(),
                photoUrl = tokenManager.getPhotoUrl()
            )
        }
    }

    fun onFullNameChange(name: String) {
        _state.update { it.copy(fullName = name) }
    }

    fun saveProfile(context: Context, photoUri: Uri?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            var tempFile: File? = null
            try {
                var photoPart: MultipartBody.Part? = null

                if (photoUri != null) {
                    tempFile = getFileFromUri(context, photoUri)
                    if (tempFile != null) {
                        val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                        photoPart = MultipartBody.Part.createFormData("photo", tempFile.name, requestFile)
                    }
                }

                val fullNameBody = if (_state.value.fullName.isNotBlank()) {
                    _state.value.fullName.toRequestBody("text/plain".toMediaTypeOrNull())
                } else {
                    null
                }

                val response = RetrofitClient.userApi.updateProfile(fullNameBody, photoPart)
                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if (responseBody != null) {
                        val newFullName = responseBody["full_name"] as? String
                        val newPhotoUrl = responseBody["photo_url"] as? String

                        tokenManager.saveUserProfile(newFullName, newPhotoUrl)
                    }
                    _state.update { it.copy(isLoading = false, isSuccess = true) }
                } else {
                    val errorString = response.errorBody()?.string()
                    _state.update { it.copy(isLoading = false, error = errorString ?: "Failed to update profile") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            } finally {
                tempFile?.delete()
            }
        }
    }

    fun resetState() {
        _state.update { it.copy(error = null, isSuccess = false) }
    }

    private suspend fun getFileFromUri(context: Context, uri: Uri): File? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.cacheDir, "temp_profile_upload_${System.currentTimeMillis()}.png")
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (file.exists()) file else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    class Factory(private val tokenManager: TokenManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditProfileViewModel(tokenManager) as T
        }
    }
}
