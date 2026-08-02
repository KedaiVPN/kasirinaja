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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.kasirinaja.core.network.TokenManager

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val transactionDao: TransactionDao,
    private val transactionRepository: TransactionRepository,
    private val tokenManager: TokenManager? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _hasNextPage = MutableStateFlow(false)
    val hasNextPage: StateFlow<Boolean> = _hasNextPage.asStateFlow()

    val PAGE_SIZE = 10

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val transactions: StateFlow<List<LocalTransactionEntity>?> = combine(
        _searchQuery.debounce(300),
        _currentPage
    ) { query, page ->
        Pair(query, page)
    }.flatMapLatest { (query, page) ->
        val offset = page * PAGE_SIZE
        val role = tokenManager?.getRole() ?: "owner"
        val cashierIdFilter = if (role == "kasir") tokenManager?.getUserId() else null
        transactionDao.getTransactionsPagedFlow(PAGE_SIZE, offset, query, cashierIdFilter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            val searchFlow = _searchQuery.debounce(300)

            combine(
                searchFlow,
                _currentPage
            ) { query, page -> Pair(query, page) }
                .flatMapLatest { (query, _) ->
                    val role = tokenManager?.getRole() ?: "owner"
                    val cashierIdFilter = if (role == "kasir") tokenManager?.getUserId() else null
                    transactionDao.getTotalTransactionsCountFlow(query, cashierIdFilter)
                }
                .collect { totalCount ->
                    _hasNextPage.value = (_currentPage.value + 1) * PAGE_SIZE < totalCount
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _currentPage.value = 0
    }

    fun nextPage() {
        if (_hasNextPage.value) {
            _currentPage.value += 1
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
        }
    }
}

class HistoryViewModelFactory(
    private val transactionDao: TransactionDao,
    private val transactionRepository: TransactionRepository,
    private val tokenManager: TokenManager? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(transactionDao, transactionRepository, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
