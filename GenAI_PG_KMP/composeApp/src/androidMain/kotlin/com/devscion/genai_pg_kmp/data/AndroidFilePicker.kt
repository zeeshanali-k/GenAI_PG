package com.devscion.genai_pg_kmp.data

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.PlatformFile
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidFilePicker(
    private val context: Context
) : FilePicker {

    private var launcher: ActivityResultLauncher<String>? = null
    private var fileLauncher: ActivityResultLauncher<Array<String>>? = null
    private var activeContinuation: CancellableContinuation<PlatformFile?>? = null

    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            onFilePicked(uri)
        }
        fileLauncher =
            activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                onFilePicked(uri, isModel = true)
            }
    }

    private fun onFilePicked(uri: Uri?, isModel: Boolean = false) {
        if (uri != null) {
            try {
                val type = context.contentResolver.getType(uri)
                val isImage = type?.startsWith("image/") == true
                val fileName = getFileName(context, uri) ?: "unknown"

                if (isImage) {
                    // For images, we get bytes
                    val bytes = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes()
                    }
                    activeContinuation?.resume(
                        PlatformFile(
                            name = fileName,
                            content = null,
                            pathOrUri = uri.toString(),
                            type = MediaType.IMAGE,
                            bytes = bytes
                        )
                    )
                } else if (isModel) {
                    // For models, we just need the URI/Path, content is not needed in memory usually
                    // Persist permission for future access
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    activeContinuation?.resume(
                        PlatformFile(
                            name = fileName,
                            content = null, // Models are binary/large, don't load into string
                            pathOrUri = uri.toString(),
                            type = MediaType.MODEL
                        )
                    )
                } else {
                    // For documents, we get text content
                    val content = context.contentResolver.openInputStream(uri)?.use {
                        it.bufferedReader().readText()
                    } ?: ""
                    activeContinuation?.resume(
                        PlatformFile(
                            name = fileName,
                            content = content,
                            pathOrUri = uri.toString(),
                            type = MediaType.DOCUMENT
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activeContinuation?.resume(null)
            }
        } else {
            activeContinuation?.resume(null)
        }
        activeContinuation = null
    }

    override suspend fun pickMedia(): PlatformFile? {
        return suspendCancellableCoroutine { continuation ->
            activeContinuation = continuation
            launcher?.launch("*/*") ?: run {
                continuation.cancel(IllegalStateException("Launcher not registered. Call register(activity) first."))
                activeContinuation = null
            }
        }
    }

    override suspend fun pickFile(extensions: List<String>): PlatformFile? {
        return suspendCancellableCoroutine { continuation ->
            activeContinuation = continuation
            val mimeTypes = "application/octet-stream"

            fileLauncher?.launch(arrayOf(mimeTypes)) ?: run {
                continuation.cancel(IllegalStateException("Launcher not registered. Call register(activity) first."))
                activeContinuation = null
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor.use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
}
