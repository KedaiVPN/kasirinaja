package com.kasirinaja.store.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kasirinaja.store.data.local.TransactionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.core.network.StockReportDto

data class ReportsState(
    val startDate: Long,
    val endDate: Long,
    val transactionData: Map<String, Int> = emptyMap(), // "YYYY-MM-DD" to Count
    val totalRevenue: Double = 0.0,
    val netProfit: Double = 0.0
)

class ReportsViewModel(
    private val transactionDao: TransactionDao
) : ViewModel() {

    private val _startDate = MutableStateFlow(getStartOfDefaultRange())
    private val _endDate = MutableStateFlow(getEndOfDefaultRange())

    private val _stockReports = MutableStateFlow<List<StockReportDto>>(emptyList())
    val stockReports: StateFlow<List<StockReportDto>> = _stockReports.asStateFlow()

    private val _isStockLoading = MutableStateFlow(false)
    val isStockLoading: StateFlow<Boolean> = _isStockLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ReportsState> = combine(_startDate, _endDate) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        combine(
            transactionDao.getTransactionsBetweenDatesFlow(start, end),
            transactionDao.getTotalRevenueBetweenDatesFlow(start, end),
            transactionDao.getNetProfitBetweenDatesFlow(start, end)
        ) { transactions, totalRevenue, netProfit ->
                val grouped = transactions.groupBy { tx ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.transactionTime }
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    dateFormat.format(cal.time)
                }

                // Fill missing days with 0
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val filledData = mutableMapOf<String, Int>()
                val calStart = Calendar.getInstance().apply { timeInMillis = start }
                calStart.set(Calendar.HOUR_OF_DAY, 0)
                calStart.set(Calendar.MINUTE, 0)
                calStart.set(Calendar.SECOND, 0)
                calStart.set(Calendar.MILLISECOND, 0)

                val calEnd = Calendar.getInstance().apply { timeInMillis = end }
                calEnd.set(Calendar.HOUR_OF_DAY, 23)
                calEnd.set(Calendar.MINUTE, 59)
                calEnd.set(Calendar.SECOND, 59)
                calEnd.set(Calendar.MILLISECOND, 999)

                val calCurrent = calStart.clone() as Calendar
                while (calCurrent.before(calEnd) || calCurrent.get(Calendar.DAY_OF_YEAR) == calEnd.get(Calendar.DAY_OF_YEAR)) {
                    val dateString = format.format(calCurrent.time)
                    filledData[dateString] = grouped[dateString]?.size ?: 0
                    calCurrent.add(Calendar.DAY_OF_YEAR, 1)
                }

                ReportsState(
                    startDate = start,
                    endDate = end,
                    transactionData = filledData,
                    totalRevenue = totalRevenue ?: 0.0,
                    netProfit = netProfit ?: 0.0
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsState(
            startDate = getStartOfDefaultRange(),
            endDate = getEndOfDefaultRange()
        )
    )

    fun fetchStockReport() {
        viewModelScope.launch {
            _isStockLoading.value = true
            try {
                val response = RetrofitClient.reportApi.getStockReport()
                if (response.isSuccessful) {
                    _stockReports.value = response.body() ?: emptyList()
                } else {
                    _stockReports.value = emptyList()
                }
            } catch (e: Exception) {
                _stockReports.value = emptyList()
            } finally {
                _isStockLoading.value = false
            }
        }
    }

    fun updateDateRange(start: Long, end: Long) {
        viewModelScope.launch {
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val localCal = Calendar.getInstance()

            utcCal.timeInMillis = start
            localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DATE), 0, 0, 0)
            _startDate.value = localCal.timeInMillis

            utcCal.timeInMillis = end
            localCal.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DATE), 23, 59, 59)
            _endDate.value = localCal.timeInMillis
        }
    }


    private val _deleteReportStatus = MutableStateFlow<Result<Unit>?>(null)
    val deleteReportStatus: StateFlow<Result<Unit>?> = _deleteReportStatus

    fun deleteReport(reportId: String, api: com.kasirinaja.core.network.ReportApi) {
        viewModelScope.launch {
            try {
                val response = api.deleteReport(reportId)
                if (response.isSuccessful) {
                    _deleteReportStatus.value = Result.success(Unit)
                } else {
                    _deleteReportStatus.value = Result.failure(Exception(response.message()))
                }
            } catch (e: Exception) {
                _deleteReportStatus.value = Result.failure(e)
            }
        }
    }

    fun resetDeleteReportStatus() {
        _deleteReportStatus.value = null
    }

    private fun getStartOfDefaultRange(): Long {

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDefaultRange(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    class Factory(
        private val transactionDao: TransactionDao
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReportsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ReportsViewModel(transactionDao) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
