package com.kasirinaja.store.utils

import android.content.Context
import java.io.File

object FileUtil {
    private const val IMAGES_DIR_NAME = "product_images"

    fun getImagesDirectory(context: Context): File {
        val dir = File(context.filesDir, IMAGES_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getLocalImagePath(context: Context, fileName: String): File {
        return File(getImagesDirectory(context), fileName)
    }

    fun isImageExistsLocally(context: Context, fileName: String): Boolean {
        if (fileName.isEmpty()) return false
        val file = getLocalImagePath(context, fileName)
        return file.exists() && file.length() > 0
    }

    fun extractFileNameFromUrl(url: String): String {
        return url.substringAfterLast("/")
    }
}
