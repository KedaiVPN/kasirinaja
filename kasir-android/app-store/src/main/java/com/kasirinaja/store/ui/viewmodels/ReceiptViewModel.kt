package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.LocalTransactionItemEntity
import com.kasirinaja.store.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiptViewModel(private val repository: ProductRepository) : ViewModel() {
    private val _transaction = MutableStateFlow<LocalTransactionEntity?>(null)
    val transaction: StateFlow<LocalTransactionEntity?> = _transaction.asStateFlow()

    private val _items = MutableStateFlow<List<LocalTransactionItemEntity>>(emptyList())
    val items: StateFlow<List<LocalTransactionItemEntity>> = _items.asStateFlow()

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            _transaction.value = repository.getTransaction(transactionId)
            _items.value = repository.getTransactionItems(transactionId)
        }
    }
}

class ReceiptViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
