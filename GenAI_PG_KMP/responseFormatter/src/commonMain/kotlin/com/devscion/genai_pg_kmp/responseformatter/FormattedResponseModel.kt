package com.devscion.genai_pg_kmp.responseformatter

sealed interface FormattedResponseBlock {
    data class Heading(
        val level: Int,
        val content: List<FormattedResponseInline>
    ) : FormattedResponseBlock

    data class Paragraph(
        val content: List<FormattedResponseInline>
    ) : FormattedResponseBlock

    data class BulletList(
        val items: List<List<FormattedResponseInline>>
    ) : FormattedResponseBlock

    data class NumberedList(
        val items: List<List<FormattedResponseInline>>
    ) : FormattedResponseBlock

    data class CodeBlock(
        val language: String?,
        val code: String
    ) : FormattedResponseBlock

    data class Table(
        val headers: List<String>,
        val rows: List<List<String>>
    ) : FormattedResponseBlock
}

sealed interface FormattedResponseInline {
    data class Text(val value: String) : FormattedResponseInline
    data class Bold(val children: List<FormattedResponseInline>) : FormattedResponseInline
    data class Italic(val children: List<FormattedResponseInline>) : FormattedResponseInline
    data class InlineCode(val value: String) : FormattedResponseInline
}
