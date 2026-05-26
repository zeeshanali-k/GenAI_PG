package com.devscion.genai_pg_kmp.data.document

import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.FilePath
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.document.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL.Companion.fileURLWithPath
import platform.PDFKit.PDFDocument

class IOSPdfTextExtractor(
    private val pathProvider: ModelPathProvider,
) : PdfTextExtractor {

    private val logger = Logger.withTag("IOSPdfTextExtractor")

    override suspend fun extractText(path: FilePath): String? = withContext(Dispatchers.Default) {
        runCatching {
            val resolvedPath = pathProvider.resolvePath(path) ?: path
            PDFDocument(fileURLWithPath(resolvedPath)).string
        }.onFailure { error ->
            logger.e(error) { "Failed to extract PDF text from $path" }
        }.getOrNull()
    }
}
