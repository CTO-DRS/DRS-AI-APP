package com.drs.ai.domain

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.drs.ai.data.db.ChatMessage
import com.drs.ai.data.db.ChatSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Session export to JSON / Markdown / TXT / PDF — written to cache, shared via FileProvider.
 */
object ChatExporter {

    @Serializable
    data class ExportMessage(val role: String, val content: String, val timestamp: Long, val tokens: Int = 0)

    @Serializable
    data class ExportSession(
        val app: String = "DRS AI",
        val version: String = "1.0.0",
        val sessionTitle: String,
        val createdAt: Long,
        val messages: List<ExportMessage>
    )

    sealed class Format(val ext: String, val mime: String) {
        data object Json : Format("json", "application/json")
        data object Markdown : Format("md", "text/markdown")
        data object Txt : Format("txt", "text/plain")
        data object Pdf : Format("pdf", "application/pdf")
    }

    fun export(context: Context, session: ChatSession, messages: List<ChatMessage>, format: Format): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safe = session.title.replace(Regex("[^\\p{L}\\p{N} _-]"), "").trim().ifBlank { "session" }
        val out = File(dir, "${safe}_${System.currentTimeMillis()}.${format.ext}")
        when (format) {
            Format.Json -> out.writeText(toJson(session, messages))
            Format.Markdown -> out.writeText(toMarkdown(session, messages))
            Format.Txt -> out.writeText(toTxt(session, messages))
            Format.Pdf -> writePdf(out, session, messages)
        }
        return out
    }

    fun toJson(session: ChatSession, messages: List<ChatMessage>): String {
        val dto = ExportSession(
            sessionTitle = session.title,
            createdAt = session.createdAt,
            messages = messages.map { ExportMessage(it.role, it.content, it.createdAt, it.tokens) }
        )
        return Json { prettyPrint = true }.encodeToString(dto)
    }

    fun toMarkdown(session: ChatSession, messages: List<ChatMessage>): String = buildString {
        appendLine("# ${session.title}")
        appendLine()
        appendLine("_Exported from DRS AI — ${System.currentTimeMillis()}_")
        appendLine()
        for (m in messages) {
            appendLine("## ${if (m.role == "user") "User" else "Assistant"}")
            appendLine()
            appendLine(m.content)
            appendLine()
        }
    }

    fun toTxt(session: ChatSession, messages: List<ChatMessage>): String = buildString {
        appendLine("DRS AI — ${session.title}")
        appendLine("=".repeat(40))
        appendLine()
        for (m in messages) {
            appendLine("${if (m.role == "user") "USER" else "ASSISTANT"}:")
            appendLine(m.content)
            appendLine()
        }
    }

    private fun writePdf(out: File, session: ChatSession, messages: List<ChatMessage>) {
        val pdf = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val paint = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }
        val titlePaint = Paint(paint).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headPaint = Paint(paint).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        var pageNo = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            pdf.finishPage(page)
            pageNo++
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawWrapped(text: String, p: Paint) {
            val maxW = pageWidth - 2 * margin
            for (rawLine in text.split('\n')) {
                val words = rawLine.split(' ')
                var line = StringBuilder()
                for (w in words) {
                    val candidate = if (line.isEmpty()) w else line.toString() + " " + w
                    if (p.measureText(candidate) > maxW && line.isNotEmpty()) {
                        ensureSpace(p.textSpacing() + 2)
                        canvas.drawText(line.toString(), margin, y + p.textSize, p)
                        y += p.textSpacing() + 2
                        line = StringBuilder(w)
                    } else {
                        line = StringBuilder(candidate)
                    }
                }
                if (line.isNotEmpty()) {
                    ensureSpace(p.textSpacing() + 2)
                    canvas.drawText(line.toString(), margin, y + p.textSize, p)
                    y += p.textSpacing() + 2
                }
            }
        }

        drawWrapped(session.title, titlePaint)
        y += 10f
        for (m in messages) {
            ensureSpace(30f)
            drawWrapped(if (m.role == "user") "USER" else "ASSISTANT", headPaint)
            drawWrapped(m.content, paint)
            y += 8f
        }
        pdf.finishPage(page)
        pdf.writeTo(out.outputStream())
        pdf.close()
    }

    private fun Paint.textSpacing(): Float = textSize * 1.35f
}
