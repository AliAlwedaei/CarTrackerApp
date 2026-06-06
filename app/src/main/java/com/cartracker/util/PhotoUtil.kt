package com.cartracker.util

import android.content.Context
import android.net.Uri
import java.io.File

object PhotoUtil {
    private const val DIR = "car_photos"

    fun getPhotoFile(context: Context, carId: Long): File {
        val dir = File(context.filesDir, DIR).also { it.mkdirs() }
        return File(dir, "$carId.jpg")
    }

    fun copyToInternal(context: Context, sourceUri: Uri, carId: Long): String {
        val dest = getPhotoFile(context, carId)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return dest.absolutePath
    }

    fun deletePhoto(context: Context, carId: Long) {
        getPhotoFile(context, carId).delete()
    }
}
