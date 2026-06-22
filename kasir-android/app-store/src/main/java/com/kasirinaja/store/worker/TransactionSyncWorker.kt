package com.kasirinaja.store.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kasirinaja.core.network.RetrofitClient
import com.kasirinaja.store.data.local.AppDatabase
import com.kasirinaja.store.data.repository.TransactionRepository

class TransactionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val transactionDao = database.transactionDao()
            val transactionApi = RetrofitClient.transactionApi

            val repository = TransactionRepository(transactionDao, transactionApi)
            repository.syncPendingTransactions()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
