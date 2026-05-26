package com.devscion.genai_pg_kmp.domain.document

import com.devscion.genai_pg_kmp.domain.FilePath
import com.devscion.genai_pg_kmp.domain.PlatformFile

interface DocumentTextParser {
    suspend fun parse(file: PlatformFile): String?
    suspend fun parse(path: FilePath, fileName: String? = null): String?
}

interface PdfTextExtractor {
    suspend fun extractText(path: FilePath): String?
}
