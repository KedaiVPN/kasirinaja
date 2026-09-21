package com.poskedai.store.utils

import android.content.Context
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object FileUtil {
    private const val IMAGES_DIR_NAME = "product_images"
    private var cachedImagesDir: File? = null

    // In-memory cache of file existence to eliminate disk I/O blocking the main UI thread during list scrolling
    private val localFilesCache = ConcurrentHashMap<String, Boolean>()

    fun getImagesDirectory(context: Context): File {
        cachedImagesDir?.let { return it }
        val dir = File(context.filesDir, IMAGES_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        cachedImagesDir = dir
        return dir
    }

    fun getLocalImagePath(context: Context, fileName: String): File {
        return File(getImagesDirectory(context), fileName)
    }

    fun isImageExistsLocally(context: Context, fileName: String): Boolean {
        if (fileName.isEmpty()) return false

        // Fast path: memory lookup avoids disk syscalls
        localFilesCache[fileName]?.let { return it }

        val file = getLocalImagePath(context, fileName)
        val exists = file.exists() && file.length() > 0
        localFilesCache[fileName] = exists
        return exists
    }

    fun markImageDownloaded(fileName: String) {
        if (fileName.isNotEmpty()) {
            localFilesCache[fileName] = true
        }
    }

    fun invalidateImageCache(fileName: String? = null) {
        if (fileName != null) {
            localFilesCache.remove(fileName)
        } else {
            localFilesCache.clear()
        }
    }

    fun extractFileNameFromUrl(url: String): String {
        return url.substringAfterLast("/")
    }
}
