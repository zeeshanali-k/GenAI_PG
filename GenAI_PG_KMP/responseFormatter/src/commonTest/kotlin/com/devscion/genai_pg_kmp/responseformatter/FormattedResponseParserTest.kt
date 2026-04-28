package com.devscion.genai_pg_kmp.responseformatter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FormattedResponseParserTest {

    @Test
    fun `normalizes escaped new lines and tabs`() {
        val normalized = FormattedResponseParser.normalize("Hello\\nWorld\\t2026")

        assertEquals("Hello\nWorld\t2026", normalized)
    }

    @Test
    fun `parses markdown style tables into structured block`() {
        val blocks = FormattedResponseParser.parse(
            """
                | Name | Role |
                | --- | --- |
                | Ali | Engineer |
                | Sara | Designer |
            """.trimIndent()
        )

        val table = assertIs<FormattedResponseBlock.Table>(blocks.single())
        assertEquals(listOf("Name", "Role"), table.headers)
        assertEquals(
            listOf(
                listOf("Ali", "Engineer"),
                listOf("Sara", "Designer")
            ),
            table.rows
        )
    }

    @Test
    fun `keeps unmatched emphasis markers as plain text`() {
        val blocks = FormattedResponseParser.parse("This has a stray * marker")

        val paragraph = assertIs<FormattedResponseBlock.Paragraph>(blocks.single())
        assertEquals(
            listOf(FormattedResponseInline.Text("This has a stray * marker")),
            paragraph.content
        )
    }
}
