package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.local.LocalTransactionEntity
import com.kasirinaja.store.data.local.TransactionDao
import com.kasirinaja.store.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val transactionDao: TransactionDao,
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    val transactions: Flow<List<LocalTransactionEntity>> = transactionDao.getAllTransactionsFlow()

}

class HistoryViewModelFactory(
    private val transactionDao: TransactionDao,
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(transactionDao, transactionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
