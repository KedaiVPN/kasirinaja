package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.core.network.TokenManager
import com.kasirinaja.store.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody

data class EditStoreState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val storeName: String = "",
    val storeAddress: String = "",
    val logoUrl: String? = null
)

class EditStoreViewModel(
    private val storeRepository: StoreRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow(EditStoreState())
    val state: StateFlow<EditStoreState> = _state.asStateFlow()

    init {
        _state.value = _state.value.copy(
            storeName = tokenManager.getStoreName() ?: "",
            storeAddress = tokenManager.getStoreAddress() ?: "",
            logoUrl = tokenManager.getStoreLogoUrl()
        )
    }

    fun onStoreNameChange(name: String) {
        _state.value = _state.value.copy(storeName = name)
    }

    fun onStoreAddressChange(address: String) {
        _state.value = _state.value.copy(storeAddress = address)
    }

    fun updateStore(imageFile: File?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)

            try {
                // Upload image if selected
                if (imageFile != null) {
                    val requestBody = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    val logoPart = MultipartBody.Part.createFormData("logo", imageFile.name, requestBody)

                    val logoResult = storeRepository.uploadStoreLogo(logoPart)
                    logoResult.onSuccess { response ->
                        val url = response["logo_url"]?.toString()
                        if (url != null) {
                            tokenManager.saveStoreLogoUrl(url)
                            _state.value = _state.value.copy(logoUrl = url)
                        }
                    }.onFailure {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "Gagal mengupload logo: ${it.message}"
                        )
                        return@launch
                    }
                }

                // Update text details
                val currentState = _state.value
                val updateResult = storeRepository.updateStore(currentState.storeName, currentState.storeAddress)

                updateResult.onSuccess {
                    tokenManager.saveStoreName(currentState.storeName)
                    tokenManager.saveStoreAddress(currentState.storeAddress)
                    _state.value = _state.value.copy(isLoading = false, isSuccess = true)
                }.onFailure {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal menyimpan detail toko: ${it.message}"
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Terjadi kesalahan: ${e.message}"
                )
            }
        }
    }

    fun resetState() {
        _state.value = _state.value.copy(isSuccess = false, errorMessage = null)
    }

    class Factory(
        private val storeRepository: StoreRepository,
        private val tokenManager: TokenManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditStoreViewModel(storeRepository, tokenManager) as T
        }
    }
}
