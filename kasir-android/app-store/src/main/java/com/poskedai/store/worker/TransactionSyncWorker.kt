package com.poskedai.store.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.poskedai.core.network.RetrofitClient
import com.poskedai.store.data.local.AppDatabase
import com.poskedai.store.data.repository.TransactionRepository

class TransactionSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val transactionDao = database.transactionDao()
            val productDao = database.productDao()
            val transactionApi = RetrofitClient.transactionApi

            val repository = TransactionRepository(transactionDao, transactionApi, productDao)
            val success = repository.syncPendingTransactions()

            if (success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
