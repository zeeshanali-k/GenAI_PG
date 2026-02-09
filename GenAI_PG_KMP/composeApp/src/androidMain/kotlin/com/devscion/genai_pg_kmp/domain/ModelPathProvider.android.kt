package com.devscion.genai_pg_kmp.domain

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ModelPathProviderAndroid(
    private val context: Context,
) : ModelPathProvider {

    private var modelPfd: ParcelFileDescriptor? = null

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
                val displayName = queryDisplayName(uri) ?: "model"
                modelPfd?.close()
                modelPfd = null
                copyUriToAppStorage(uri, displayName)
            }
        } catch (e: Exception) {
            Log.e("MediaPipeModelManager", "Failed to open model Uri: ${e.message}")
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

    private fun copyUriToAppStorage(uri: android.net.Uri, fileName: String): String? {
        return try {
            val targetDir =
                context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
            if (!targetDir.exists() && !targetDir.mkdirs()) return null
            val targetFile = File(targetDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e("MediaPipeModelManager", "Failed to copy model: ${e.message}")
            null
        }
    }
}