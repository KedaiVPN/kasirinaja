package com.poskedai.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.poskedai.store.data.local.LocalTransactionEntity
import com.poskedai.store.data.local.LocalTransactionItemEntity
import com.poskedai.store.data.repository.ProductRepository
import com.poskedai.store.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReceiptViewModel(
    private val repository: ProductRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    private val _transaction = MutableStateFlow<LocalTransactionEntity?>(null)
    val transaction: StateFlow<LocalTransactionEntity?> = _transaction.asStateFlow()

    private val _items = MutableStateFlow<List<LocalTransactionItemEntity>>(emptyList())
    val items: StateFlow<List<LocalTransactionItemEntity>> = _items.asStateFlow()

    fun loadTransaction(transactionId: String) {
        viewModelScope.launch {
            var tx = repository.getTransaction(transactionId)
            if (tx == null) {
                // Not found locally. It might be a new transaction from another user.
                // Fetch all transactions from server to sync up.
                transactionRepository.fetchAndSaveAllTransactions()
                tx = repository.getTransaction(transactionId)
            }

            _transaction.value = tx
            _items.value = repository.getTransactionItems(transactionId)
        }
    }
}

class ReceiptViewModelFactory(
    private val repository: ProductRepository,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReceiptViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReceiptViewModel(repository, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}