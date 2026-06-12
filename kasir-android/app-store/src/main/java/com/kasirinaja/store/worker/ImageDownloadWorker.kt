package com.kasirinaja.store.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kasirinaja.store.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import com.kasirinaja.core.network.RetrofitClient

class ImageDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val imageUrls = inputData.getStringArray("IMAGE_URLS") ?: return@withContext Result.success()

        var allSuccess = true

        for (urlPath in imageUrls) {
            if (urlPath.isEmpty()) continue

            // e.g. urlPath might be "uploads/xyz.jpg" or "/uploads/xyz.jpg"
            val safeUrlPath = if (urlPath.startsWith("/")) urlPath else "/$urlPath"
            val fileName = FileUtil.extractFileNameFromUrl(safeUrlPath)

            // Skip if already downloaded
            if (FileUtil.isImageExistsLocally(applicationContext, fileName)) {
                continue
            }

            val fullUrlString = "${RetrofitClient.IMAGE_BASE_URL}$safeUrlPath"

            try {
                val url = URL(fullUrlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val localFile = FileUtil.getLocalImagePath(applicationContext, fileName)
                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(localFile)

                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }

                    outputStream.close()
                    inputStream.close()
                } else {
                    allSuccess = false
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }

            // Sleep briefly to avoid hammering the server with rapid requests
            kotlinx.coroutines.delay(500)
        }

        if (allSuccess) Result.success() else Result.retry()
    }
}
