package com.devscion.genai_pg_kmp.data.rag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleDocumentSplitterTest {

    private val splitter = SimpleDocumentSplitter()

    @Test
    fun `splitOnCharacter stops after final chunk instead of sliding one character at a time`() {
        val text =
            "Price action often depends on clear areas of support and resistance to buy or sell at."

        val chunks = splitter.splitOnCharacter(
            text = text,
            chunkSize = 50,
            overlap = 10
        )

        assertEquals(
            listOf(
                "Price action often depends on clear areas of",
                "of support and resistance to buy or sell at."
            ),
            chunks
        )
    }

    @Test
    fun `splitOnCharacter keeps overlap starts on word boundaries`() {
        val text =
            "Kotlin multiplatform document parsing should avoid overlap chunks that restart midword."

        val chunks = splitter.splitOnCharacter(
            text = text,
            chunkSize = 45,
            overlap = 12
        )

        assertTrue(chunks.size >= 2)
        var searchFrom = 0
        chunks.forEach { chunk ->
            val startIndex = text.indexOf(chunk, startIndex = searchFrom)
            assertTrue(startIndex >= 0, "Chunk should exist in original text: $chunk")
            assertTrue(
                startIndex == 0 || text[startIndex - 1].isLetterOrDigit().not(),
                "Chunk should begin at a word boundary: $chunk"
            )
            searchFrom = startIndex + 1
        }
    }

    @Test
    fun `splitOnCharacter never emits one character suffix cascade near tail`() {
        val text =
            "Support and resistance can help a trader find areas of support and resistance to buy or sell at."

        val chunks = splitter.splitOnCharacter(
            text = text,
            chunkSize = 55,
            overlap = 14
        )

        assertTrue(chunks.size <= 3)
        assertTrue(chunks.none { chunk ->
            chunk in setOf(
                "reas of support and resistance to buy or sell at.",
                "eas of support and resistance to buy or sell at.",
                "as of support and resistance to buy or sell at.",
                "s of support and resistance to buy or sell at."
            )
        })
    }
}
