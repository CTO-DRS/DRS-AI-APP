package com.drs.ai.core.rag

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Local document text extraction: PDF (pdfbox-android), DOCX (ZipFile + XmlPullParser),
 * HTML (jsoup), TXT/MD (direct). No network, no external services.
 */
object DocumentParser {

    data class Extracted(val text: String, val format: String)

    class ParseException(message: String) : Exception(message)

    fun supports(mime: String?, name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return mime?.contains("pdf") == true || mime?.contains("wordprocessingml") == true ||
            mime?.contains("html") == true || mime?.contains("text/") == true ||
            ext in setOf("pdf", "txt", "md", "markdown", "docx", "html", "htm")
    }

    fun extract(context: Context, uri: Uri, mime: String?, name: String): Extracted {
        val ext = name.substringAfterLast('.', "").lowercase()
        return context.contentResolver.openInputStream(uri)?.use { raw ->
            BufferedInputStream(raw).use { buf ->
                when {
                    ext == "pdf" || mime?.contains("pdf") == true -> Extracted(parsePdf(buf), "PDF")
                    ext == "docx" || mime?.contains("wordprocessingml") == true -> Extracted(parseDocx(buf), "DOCX")
                    ext in setOf("html", "htm") || mime?.contains("html") == true -> Extracted(parseHtml(buf), "HTML")
                    ext in setOf("txt", "md", "markdown") || mime?.contains("text/") == true || mime == null -> Extracted(parseText(buf), ext.uppercase().ifBlank { "TXT" })
                    else -> throw ParseException("Unsupported format: .$ext")
                }
            }
        } ?: throw ParseException("Cannot open document")
    }

    private fun parsePdf(ins: InputStream): String = try {
        PDDocument.load(ins).use { doc ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.getText(doc)
        }
    } catch (e: Exception) {
        throw ParseException("PDF parse failed: ${e.message}")
    }

    private fun parseDocx(ins: InputStream): String = try {
        ZipInputStream(ins).use { zip ->
            var entry = zip.nextEntry
            var found = false
            val sb = StringBuilder()
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            while (entry != null) {
                if (entry.name == "document.xml" && entry.isDirectory.not()) {
                    found = true
                    parser.setInput(zip, "UTF-8")
                    var event = parser.eventType
                    var inParagraph = false
                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> when (parser.name) {
                                "p" -> { inParagraph = true; if (sb.isNotEmpty() && !sb.endsWith("\n")) sb.append('\n') }
                                "t" -> if (inParagraph) sb.append(parser.nextText())
                            }
                            XmlPullParser.END_TAG -> if (parser.name == "p") inParagraph = false
                        }
                        event = parser.next()
                    }
                    break
                }
                entry = zip.nextEntry
            }
            if (!found) throw ParseException("DOCX: word/document.xml not found")
            sb.toString()
        }
    } catch (e: ParseException) {
        throw e
    } catch (e: Exception) {
        throw ParseException("DOCX parse failed: ${e.message}")
    }

    private fun parseHtml(ins: InputStream): String = try {
        val html = ins.readBytes().toString(Charsets.UTF_8)
        Jsoup.parse(html).wholeText()
    } catch (e: Exception) {
        throw ParseException("HTML parse failed: ${e.message}")
    }

    private fun parseText(ins: InputStream): String = try {
        ins.readBytes().toString(Charsets.UTF_8)
    } catch (e: Exception) {
        throw ParseException("Text read failed: ${e.message}")
    }
}
