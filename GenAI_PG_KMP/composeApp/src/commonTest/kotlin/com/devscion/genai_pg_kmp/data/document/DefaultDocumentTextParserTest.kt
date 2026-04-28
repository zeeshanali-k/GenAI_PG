package com.devscion.genai_pg_kmp.data.document

import com.devscion.genai_pg_kmp.domain.FilePath
import com.devscion.genai_pg_kmp.domain.ModelPathProvider
import com.devscion.genai_pg_kmp.domain.PlatformFile
import com.devscion.genai_pg_kmp.domain.MediaType
import com.devscion.genai_pg_kmp.domain.document.PdfTextExtractor
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultDocumentTextParserTest {

    @Test
    fun `formats csv into column aware text`() = runBlocking {
        val parser = createParser(
            mapOf(
                "people.csv" to """
                    name,role,city
                    Ali,Engineer,Karachi
                    Sara,Designer,Lahore
                """.trimIndent()
            )
        )

        val text = parser.parse("people.csv", "people.csv")

        assertEquals(
            """
                Columns: name, role, city

                Row 1: name: Ali | role: Engineer | city: Karachi

                Row 2: name: Sara | role: Designer | city: Lahore
            """.trimIndent(),
            text
        )
    }

    @Test
    fun `flattens json objects for rag ingestion`() = runBlocking {
        val parser = createParser(
            mapOf(
                "profile.json" to """
                    {
                      "user": {
                        "name": "Ali",
                        "skills": ["Kotlin", "RAG"]
                      }
                    }
                """.trimIndent()
            )
        )

        val text = parser.parse("profile.json", "profile.json")

        assertEquals(
            """
                user.name: Ali
                user.skills: Kotlin, RAG
            """.trimIndent(),
            text
        )
    }

    @Test
    fun `strips html tags into readable text`() = runBlocking {
        val parser = createParser(
            mapOf(
                "page.html" to """
                    <html><body><h1>Guide</h1><p>Hello <strong>team</strong>.</p><ul><li>One</li><li>Two</li></ul></body></html>
                """.trimIndent()
            )
        )

        val text = parser.parse("page.html", "page.html")

        assertEquals(
            """
                Guide
                Hello team.
                - One
                - Two
            """.trimIndent(),
            text
        )
    }

    @Test
    fun `routes pdf files through platform extractor`() = runBlocking {
        val parser = createParser(
            textFiles = emptyMap(),
            pdfText = "PDF body"
        )

        val text = parser.parse(
            PlatformFile(
                name = "paper.pdf",
                content = null,
                pathOrUri = "paper.pdf",
                type = MediaType.DOCUMENT
            )
        )

        assertEquals("PDF body", text)
    }

    @Test
    fun `falls back to in memory content when file path is unavailable`() = runBlocking {
        val parser = createParser(emptyMap())

        val text = parser.parse(
            PlatformFile(
                name = "note.md",
                content = "# Hello",
                pathOrUri = "",
                type = MediaType.DOCUMENT
            )
        )

        assertEquals("# Hello", text)
    }

    private fun createParser(
        textFiles: Map<String, String>,
        pdfText: String = "PDF content",
    ) = DefaultDocumentTextParser(
        pathProvider = object : ModelPathProvider {
            override fun getPath(modelName: String): String? = null
            override suspend fun resolvePath(path: String): String? = path
            override suspend fun makeLocalCopy(path: FilePath): FilePath? = path
            override suspend fun getContentByteArray(path: FilePath): ByteArray? = null
            override suspend fun getContentText(path: FilePath): String? = textFiles[path]
        },
        pdfTextExtractor = object : PdfTextExtractor {
            override suspend fun extractText(path: FilePath): String? = pdfText
        }
    )
}
