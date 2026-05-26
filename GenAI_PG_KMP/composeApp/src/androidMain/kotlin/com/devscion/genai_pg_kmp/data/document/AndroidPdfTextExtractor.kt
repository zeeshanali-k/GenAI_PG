package com.devscion.genai_pg_kmp.data.document

import android.content.Context
import co.touchlab.kermit.Logger
import com.devscion.genai_pg_kmp.domain.FilePath
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.document.PdfTextExtractor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPdfTextExtractor(
    private val context: Context,
    private val pathProvider: ModelPathProvider,
) : PdfTextExtractor {

    private val logger = Logger.withTag("AndroidPdfTextExtractor")

    override suspend fun extractText(path: FilePath): String? = withContext(Dispatchers.IO) {
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            val bytes = pathProvider.getContentByteArray(path) ?: return@withContext null
            PDDocument.load(bytes).use { document ->
                PDFTextStripper().getText(document)
            }
        }.onFailure { error ->
            logger.e(error) { "Failed to extract PDF text from $path" }
        }.getOrNull()
    }
}
