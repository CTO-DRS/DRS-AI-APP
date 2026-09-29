package com.drs.ai.domain

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.drs.ai.data.db.ChatMessage
import com.drs.ai.data.db.ChatSession
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Session export to JSON / Markdown / TXT / HTML / PDF — written to cache, shared via FileProvider.
 * v1.3 improvements: HTML with RTL support + print-to-PDF, Arabic-aware PDF via StaticLayout,
 * timestamps and message metadata in every format, one-tap share/save.
 */
object ChatExporter {

    @Serializable
    data class ExportMessage(val role: String, val content: String, val timestamp: Long, val tokens: Int = 0)

    @Serializable
    data class ExportSession(
        val app: String = "DRS AI",
        val version: String = "1.3.0",
        val sessionTitle: String,
        val createdAt: Long,
        val messages: List<ExportMessage>
    )

    sealed class Format(val ext: String, val mime: String) {
        data object Json : Format("json", "application/json")
        data object Markdown : Format("md", "text/markdown")
        data object Txt : Format("txt", "text/plain")
        data object Html : Format("html", "text/html")
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
            Format.Html -> out.writeText(toHtml(session, messages))
            Format.Pdf -> writePdf(out, session, messages)
        }
        return out
    }

    /** Fire the system share sheet so the user can save to Drive/Files or send anywhere. */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.extension.let { ext ->
                when (ext) {
                    "pdf" -> "application/pdf"
                    "json" -> "application/json"
                    "html" -> "text/html"
                    "md" -> "text/markdown"
                    else -> "text/plain"
                }
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun dateOf(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))

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
        appendLine("_Exported from DRS AI — ${dateOf(System.currentTimeMillis())} · ${messages.size} messages_")
        appendLine()
        for (m in messages) {
            appendLine("## ${if (m.role == "user") "👤 User" else "🤖 Assistant"}")
            appendLine()
            appendLine("_${dateOf(m.createdAt)}_")
            appendLine()
            appendLine(m.content)
            appendLine()
            appendLine("---")
            appendLine()
        }
    }

    fun toTxt(session: ChatSession, messages: List<ChatMessage>): String = buildString {
        appendLine("DRS AI — ${session.title}")
        appendLine("=".repeat(40))
        appendLine("Exported: ${dateOf(System.currentTimeMillis())} · ${messages.size} messages")
        appendLine()
        for (m in messages) {
            appendLine("[${dateOf(m.createdAt)}] ${if (m.role == "user") "USER" else "ASSISTANT"}:")
            appendLine(m.content)
            appendLine()
        }
    }

    fun toHtml(session: ChatSession, messages: List<ChatMessage>): String {
        val esc: (String) -> String = { s ->
            s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;")
        }
        val body = StringBuilder()
        for (m in messages) {
            val isUser = m.role == "user"
            body.append("""<div class="msg ${if (isUser) "user" else "assistant"}">""")
            body.append("""<div class="meta">${if (isUser) "👤" else "🤖"} ${dateOf(m.createdAt)}${if (m.tokens > 0) " · ${m.tokens} tok" else ""}</div>""")
            body.append("""<div class="bubble">${esc(m.content).replace("\n", "<br/>")}</div>""")
            body.append("</div>")
        }
        return """<!DOCTYPE html>
<html lang="ar" dir="auto">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>${esc(session.title)} — DRS AI</title>
<style>
  :root { color-scheme: light dark; }
  body { font-family: system-ui, -apple-system, "Segoe UI", Tahoma, sans-serif;
         max-width: 820px; margin: 0 auto; padding: 24px 16px 64px; line-height: 1.65; }
  header { border-bottom: 2px solid #6C5CE7; padding-bottom: 12px; margin-bottom: 22px; }
  h1 { font-size: 1.5rem; margin: 0 0 4px; }
  .sub { opacity: .7; font-size: .9rem; }
  .msg { margin: 18px 0; }
  .meta { font-size: .75rem; opacity: .65; margin-bottom: 4px; }
  .bubble { padding: 12px 16px; border-radius: 16px; white-space: pre-wrap; }
  .user .bubble { background: #EEEBFF; border-start-start-radius: 4px; }
  .assistant .bubble { background: #F2F4F7; border-start-end-radius: 4px; }
  @media (prefers-color-scheme: dark) {
    .user .bubble { background: #2A2550; color: #E6E1FF; }
    .assistant .bubble { background: #23262D; color: #E7EAEE; }
  }
  @media print {
    .bubble { background: none !important; border: 1px solid #ccc; color: #000 !important; }
    .meta { color: #444 !important; }
  }
</style>
</head>
<body>
<header>
  <h1>${esc(session.title)}</h1>
  <div class="sub">DRS AI v1.3.0 · ${dateOf(System.currentTimeMillis())} · ${messages.size} messages · 100% offline</div>
</header>
$body
<footer style="margin-top:36px;font-size:.75rem;opacity:.6;text-align:center">Generated locally by DRS AI — no cloud, no tracking</footer>
</body>
</html>"""
    }

    private fun writePdf(out: File, session: ChatSession, messages: List<ChatMessage>) {
        val pdf = PdfDocument()
        val pageWidth = 595   // A4 @72dpi
        val pageHeight = 842
        val margin = 40f
        val contentWidth = (pageWidth - 2 * margin).toInt()

        val titlePaint = TextPaint().apply {
            textSize = 18f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); isAntiAlias = true
        }
        val headPaint = TextPaint().apply {
            textSize = 12f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); isAntiAlias = true
        }
        val bodyPaint = TextPaint().apply {
            textSize = 11f; typeface = Typeface.SANS_SERIF; isAntiAlias = true
        }
        val metaPaint = TextPaint().apply {
            textSize = 8f; typeface = Typeface.SANS_SERIF; isAntiAlias = true; color = 0xFF666666.toInt()
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

        // v1.3: StaticLayout does proper line-breaking AND Arabic/RTL shaping —
        // the previous Canvas.drawText approach rendered Arabic disconnected.
        fun drawStatic(text: String, paint: TextPaint, spacing: Float) {
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(false)
                .build()
            ensureSpace(layout.height.toFloat() + spacing)
            canvas.save()
            canvas.translate(margin, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + spacing
        }

        drawStatic(session.title, titlePaint, 6f)
        drawStatic("DRS AI v1.3.0 · ${dateOf(System.currentTimeMillis())} · ${messages.size} messages", metaPaint, 14f)

        for (m in messages) {
            ensureSpace(50f)
            drawStatic((if (m.role == "user") "USER" else "ASSISTANT") + "  ·  " + dateOf(m.createdAt), headPaint, 4f)
            drawStatic(m.content.ifBlank { " " }, bodyPaint, 12f)
        }
        pdf.finishPage(page)
        pdf.writeTo(out.outputStream())
        pdf.close()
    }
}
