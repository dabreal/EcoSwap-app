package com.example.ecoswap

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Crea un fichero JPEG en el almacenamiento interno de la app y su [Uri] para usar con TakePicture.
 */
object ProductImageCapture {

    fun createNewPhoto(context: Context): CameraPhotoTarget {
        val dir = File(context.filesDir, "product_photos").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        if (file.exists()) file.delete()
        if (!file.createNewFile()) {
            throw IllegalStateException("No se pudo crear el fichero de foto")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return CameraPhotoTarget(uri = uri, file = file)
    }
}

data class CameraPhotoTarget(
    val uri: Uri,
    val file: File
)
