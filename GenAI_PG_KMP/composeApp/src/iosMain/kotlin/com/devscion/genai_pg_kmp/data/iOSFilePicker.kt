package com.devscion.genai_pg_kmp.data

import com.devscion.genai_pg_kmp.domain.FilePicker
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    private var activeDelegate: NSObject? = null
    private var activePicker: UIDocumentPickerViewController? = null
    private var activeContinuation: CancellableContinuation<PlatformFile?>? = null

    private fun clearActivePickerState() {
        activeContinuation = null
        activeDelegate = null
        activePicker = null
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun pickMedia(): PlatformFile? = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            activeContinuation = continuation
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (url != null) {
                        val shouldStopAccessing = url.startAccessingSecurityScopedResource()

                        try {
                            val name = url.lastPathComponent ?: "unknown"
                            val path = url.absoluteString ?: ""

                            // Check file type extension for simplicity
                            val ext = url.pathExtension?.lowercase()
                            val isImage =
                                listOf("jpg", "jpeg", "png", "webp", "heic").contains(ext)

                            if (isImage) {
//                                val data = NSData.dataWithContentsOfURL(url)
//                                val bytes = if (data != null) {
//                                    val byteArray = ByteArray(data.length.toInt())
//                                    if (data.length > 0u) {
//                                        data.bytes?.let { ptr ->
//                                            // Copy bytes logic or use simplified bridge if available
//                                            // For now assuming we can read it.
//                                            // Native pointer handling in KMP is verbose, simplified here:
//                                            // TODO: Proper NSData to ByteArray conversion helper
//                                            null // Placeholder to ensure compilation if helper missing
//                                        }
//                                    } else null
//
//                                    // Placeholder: actually implementing NSData -> ByteArray
//                                    val pinned = byteArray.usePinned {
//                                        // memcpy(it.addressOf(0), data.bytes, data.length)
//                                        // requires CInterop
//                                    }
//                                    // Simplified for this context:
//                                    // We will rely on returning generic PlatformFile and deal with bytes properly in real implementation
//                                    // using standard KMP conversions.
//                                    null
//                                } else null

                                if (continuation.isActive) {
                                    continuation.resume(
                                        null//TODO: support media for iOS
//                                        PlatformFile(
//                                            name,
//                                            null,
//                                            path,
//                                            MediaType.IMAGE,
//                                            bytes
//                                        )
                                    )
                                }
                            } else {
                                val content = NSString.stringWithContentsOfURL(
                                    url,
                                    encoding = NSUTF8StringEncoding,
                                    error = null
                                ) ?: ""
                                println("iOSFIlePicker: pickMedia-> $content")
                                if (continuation.isActive) {
                                    continuation.resume(
                                        PlatformFile(
                                            name,
                                            content,
                                            path,
                                            MediaType.DOCUMENT
                                        )
                                    )
                                }
                            }

                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resume(null)
                        } finally {
                            if (shouldStopAccessing) {
                                url.stopAccessingSecurityScopedResource()
                            }
                            clearActivePickerState()
                        }
                    } else {
                        if (continuation.isActive) continuation.resume(null)
                        clearActivePickerState()
                    }
                }

                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    if (continuation.isActive) continuation.resume(null)
                    clearActivePickerState()
                }
            }

            val documentPicker = UIDocumentPickerViewController(
                documentTypes = listOf("public.item"), // Allow all Items
                inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
            )
            activeDelegate = delegate
            activePicker = documentPicker
            documentPicker.delegate = delegate

            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootController == null) {
                if (continuation.isActive) continuation.resume(null)
                clearActivePickerState()
            } else {
                rootController.presentViewController(
                    documentPicker,
                    animated = true,
                    completion = null
                )
            }

            continuation.invokeOnCancellation {
                clearActivePickerState()
            }
        }
    }

    override suspend fun pickFile(extensions: List<String>): PlatformFile? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                activeContinuation = continuation
                val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAtURLs: List<*>
                    ) {
                        println("iOSFilePicker: documentPicker-> didPickDocumentsAtURLs: $didPickDocumentsAtURLs")
                        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                        println("iOSFilePicker: documentPicker-> url: $url")
                        if (url != null) {
                            // For models/files we want to persist permission if possible or just get the path
                            // Models are large, we only need the path usually.
                            val path = url.path // specific path
                            val name = url.lastPathComponent ?: "unknown"
                            if (continuation.isActive) {
                                continuation.resume(
                                    PlatformFile(
                                        name = name,
                                        content = null,
                                        pathOrUri = path ?: url.absoluteString ?: "",
                                        type = MediaType.MODEL
                                    )
                                )
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(null)
                        }
                        clearActivePickerState()
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        println("iOSFilePicker: documentPickerWasCancelled")
                        if (continuation.isActive) continuation.resume(null)
                        clearActivePickerState()
                    }
                }

                val documentPicker = UIDocumentPickerViewController(
                    documentTypes = listOf("public.data", "public.content"),
                    inMode = UIDocumentPickerMode.UIDocumentPickerModeImport
                )
                activeDelegate = delegate
                activePicker = documentPicker
                documentPicker.delegate = delegate

                val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (rootController == null) {
                    if (continuation.isActive) continuation.resume(null)
                    clearActivePickerState()
                } else {
                    rootController.presentViewController(
                        viewControllerToPresent = documentPicker,
                        animated = true,
                        completion = null
                    )
                }

                continuation.invokeOnCancellation {
                    clearActivePickerState()
                }
            }
        }
}
