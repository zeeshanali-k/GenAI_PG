package com.devscion.genai_pg_kmp.data

import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.PickedFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerMode
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

class iOSFilePicker : FilePicker {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun pickDocument(): PickedFile? {
        return suspendCancellableCoroutine { continuation ->
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (url != null) {
                        // Access security scoped resource if needed
                        val shouldStopAccessing = url.startAccessingSecurityScopedResource()

                        try {
                            val name = url.lastPathComponent ?: "document.txt"
                            val content = NSString.stringWithContentsOfURL(
                                url,
                                encoding = NSUTF8StringEncoding,
                                error = null
                            ) as? String ?: ""
                            continuation.resume(PickedFile(name, content))
                        } catch (e: Exception) {
                            continuation.resume(null)
                        } finally {
                            if (shouldStopAccessing) {
                                url.stopAccessingSecurityScopedResource()
                            }
                        }
                    } else {
                        continuation.resume(null)
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    continuation.resume(null)
                }
            }

            val documentPicker = UIDocumentPickerViewController(
                documentTypes = listOf("public.text", "public.plain-text", "public.content"),
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            documentPicker.delegate = delegate

            // Get root view controller
            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootController?.presentViewController(
                documentPicker,
                animated = true,
                completion = null
            )
        }
    }
}
