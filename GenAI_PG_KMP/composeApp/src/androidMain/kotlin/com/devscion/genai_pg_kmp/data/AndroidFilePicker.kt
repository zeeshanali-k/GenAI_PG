package com.devscion.genai_pg_kmp.data

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.PickedFile
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidFilePicker(
    private val context: Context
) : FilePicker {

    private var launcher: ActivityResultLauncher<String>? = null
    private var activeContinuation: CancellableContinuation<PickedFile?>? = null

    fun bind(launcher: ActivityResultLauncher<String>) {
        this.launcher = launcher
    }

    fun onFilePicked(uri: Uri?) {
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: ""

                val name = getFileName(context, uri) ?: "document.txt"

                activeContinuation?.resume(PickedFile(name, content))
            } catch (e: Exception) {
                e.printStackTrace()
                activeContinuation?.resume(null)
            }
        } else {
            activeContinuation?.resume(null)
        }
        activeContinuation = null
    }

    override suspend fun pickDocument(): PickedFile? {
        return suspendCancellableCoroutine { continuation ->
            activeContinuation = continuation
            launcher?.launch("*/*") ?: run {
                continuation.cancel(IllegalStateException("Launcher not bound"))
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
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
