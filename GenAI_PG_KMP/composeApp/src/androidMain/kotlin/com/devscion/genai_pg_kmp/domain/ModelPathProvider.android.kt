package com.devscion.genai_pg_kmp.domain

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ModelPathProviderAndroid(
    private val context: Context,
) : ModelPathProvider {

    private var modelPfd: ParcelFileDescriptor? = null
    private val logger = Logger.withTag("ModelPathProvider")

    @Deprecated("It was used when models were assumed to be in app storage")
    override fun getPath(modelName: String): String? {
        return try {
            val modelFile = context.getExternalFilesDir("llm") ?: return ""
            if (modelFile.exists()) {
                return "${modelFile.absolutePath}/${modelName}"
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolvePath(path: String): String? = withContext(Dispatchers.IO) {
        if (!path.startsWith("content://")) return@withContext path
        val uri = path.toUri()
        return@withContext try {
            modelPfd?.close()
            modelPfd = context.contentResolver.openFileDescriptor(uri, "r")
            val pfd = modelPfd ?: return@withContext null
            val procPath = "/proc/self/fd/${pfd.fd}"
            if (canOpenProcFd(procPath)) {
                procPath
            } else {
                modelPfd?.close()
                modelPfd = null
                makeLocalCopy(uri.toString())
            }
        } catch (e: Exception) {
            Log.e("MediaPipeModelManager", "Failed to open model Uri: ${e.message}")
            null
        }
    }

    override suspend fun makeLocalCopy(path: FilePath): FilePath? {
        return try {
            return withContext(Dispatchers.IO) {
                val uri = path.toUri()
                val fileName = queryDisplayName(uri) ?: "File_${System.currentTimeMillis()}.${
                    path.split(".").lastOrNull() ?: return@withContext null
                }"
                val targetDir = context.getExternalFilesDir("internal")
                    ?: File(context.filesDir, "internal")
                if (!targetDir.exists() && !targetDir.mkdirs()) return@withContext null
                val targetFile = File(targetDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null
                return@withContext targetFile.absolutePath
            }
        } catch (e: Exception) {
            Log.d("MediaPipeModelManager", "Failed to copy model: ${e.message}")
            null
        }
    }

    override suspend fun getContentByteArray(path: FilePath): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(path.toUri())?.use {
                    it.readBytes()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    override suspend fun getContentText(path: FilePath): String? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                FileInputStream(file).use {
                    it.bufferedReader().readText()
                }
//                context.contentResolver.openInputStream(path.toUri())?.use {
//                    it.bufferedReader().readText()
//                }
            } catch (e: Exception) {
                logger.d { "getContextText: ${e.message} :: ${e.cause} :: ${e.localizedMessage}" }
                e.printStackTrace()
                null
            }
        }


    private fun canOpenProcFd(procPath: String): Boolean {
        return try {
            FileInputStream(procPath).use { }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun queryDisplayName(uri: android.net.Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor.use { c ->
            if (c != null && c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return null
    }
}