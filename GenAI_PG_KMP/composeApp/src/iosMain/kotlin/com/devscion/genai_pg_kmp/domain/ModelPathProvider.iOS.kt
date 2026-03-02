@file:OptIn(ExperimentalForeignApi::class)

package com.devscion.genai_pg_kmp.domain

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURL.Companion.fileURLWithPath
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.posix.memcpy

class ModelPathProviderIOS : ModelPathProvider {

    private val fileManager = NSFileManager.defaultManager

    @Deprecated("It was used when models were assumed to be in app storage")
    override fun getPath(modelName: String): String? {
        return try {
            val modelFile = documentsInternalDirectory()?.let { "$it/$modelName" }
            if (modelFile != null && fileManager.fileExistsAtPath(modelFile)) {
                return modelFile
            }

            val extension = modelName.substringAfterLast('.', "")
            val resourceName =
                if (extension.isEmpty()) modelName else modelName.removeSuffix(".$extension")
            NSBundle.mainBundle.pathForResource(resourceName, ofType = extension.ifEmpty { null })
        } catch (e: Exception) {
            println("LlamatikModelManager-> Error: ${e.message} :: ${e.cause}")
            null
        }
    }

    override suspend fun resolvePath(path: String): String? = withContext(Dispatchers.Default) {
        if (path.isBlank()) return@withContext null
        val resolvedPath = resolveFilePath(path) ?: return@withContext null
        if (fileManager.fileExistsAtPath(resolvedPath)) return@withContext resolvedPath
        makeLocalCopy(path)
    }

    override suspend fun makeLocalCopy(path: FilePath): FilePath? =
        withContext(Dispatchers.Default) {
            runCatching {
                val sourcePath = resolveFilePath(path) ?: return@withContext null
                if (!fileManager.fileExistsAtPath(sourcePath)) return@withContext null
                val fileName = sourcePath.substringAfterLast('/')
                    .ifBlank { "File_${platform.Foundation.NSDate().timeIntervalSince1970}" }
                val targetDir = documentsInternalDirectory() ?: return@withContext null
                if (sourcePath.startsWith("$targetDir/")) return@withContext sourcePath
                if (fileManager.fileExistsAtPath(targetDir).not()) {
                    fileManager.createDirectoryAtPath(
                        path = targetDir,
                        withIntermediateDirectories = true,
                        attributes = null,
                        error = null
                    )
                }
                val targetPath = "$targetDir/$fileName"
                if (fileManager.fileExistsAtPath(targetPath)) {
                    fileManager.removeItemAtPath(targetPath, error = null)
                }
                fileManager.copyItemAtURL(
                    fileURLWithPath(sourcePath),
                    fileURLWithPath(targetPath),
                    null
                )
                targetPath
            }.getOrNull()
        }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getContentByteArray(path: FilePath): ByteArray? =
        withContext(Dispatchers.Default) {
            runCatching {
                val resolvedPath = resolveFilePath(path) ?: return@withContext null
                val data = platform.Foundation.NSData.dataWithContentsOfFile(resolvedPath)
                    ?: return@withContext null
                val size = data.length.toInt()
                if (size == 0) return@withContext ByteArray(0)
                ByteArray(size).apply {
                    usePinned { pinned ->
                        memcpy(pinned.addressOf(0), data.bytes, data.length)
                    }
                }
            }.getOrNull()
        }

    override suspend fun getContentText(path: FilePath): String? =
        withContext(Dispatchers.Default) {
            runCatching {
                val resolvedPath = resolveFilePath(path) ?: return@withContext null
                NSString.stringWithContentsOfURL(
                    fileURLWithPath(resolvedPath),
                    encoding = NSUTF8StringEncoding,
                    error = null
                )
            }.getOrNull()
        }

    private fun resolveFilePath(path: String): String? {
        if (path.isBlank()) return null
        if (path.startsWith("file://")) {
            return NSURL(string = path).path
        }
        return path
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentsInternalDirectory(): String? {
        val documentDirectory = fileManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: return null
        return "${documentDirectory.path}/internal"
    }
}
