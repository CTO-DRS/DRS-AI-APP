package com.drs.ai.core.rag

import android.content.Context
import android.net.Uri
import com.drs.ai.core.ai.LlmEngine
import com.drs.ai.data.dao.RagDao
import com.drs.ai.data.db.Document
import com.drs.ai.data.db.VectorRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local RAG pipeline: extract → chunk → embed (local embedder engine) → store vectors
 * in Room → brute-force cosine retrieval. Fully offline; sized for on-device corpora.
 */
class RagPipeline(
    private val context: Context,
    private val dao: RagDao,
    private val embedder: LlmEngine
) {
    data class SearchHit(val docId: Long, val docName: String, val chunkIndex: Int, val text: String, val score: Float)

    suspend fun importDocument(uri: Uri, displayName: String, mime: String?, onProgress: (String) -> Unit): Int = withContext(Dispatchers.IO) {
        if (!embedder.isLoaded) throw IllegalStateException("embedder_not_loaded")
        onProgress("Parsing…")
        val extracted = DocumentParser.extract(context, uri, mime, displayName)
        val text = extracted.text.trim()
        if (text.isEmpty()) throw IllegalStateException("No extractable text found in this file")
        onProgress("Chunking…")
        val chunks = Chunker.chunk(text)
        if (chunks.isEmpty()) throw IllegalStateException("Document produced no chunks")

        val docId = dao.insertDocument(
            Document(name = displayName, sizeBytes = text.length.toLong(), chunks = chunks.size, createdAt = System.currentTimeMillis())
        )
        var done = 0
        val rows = mutableListOf<VectorRow>()
        var dim = 0
        for (c in chunks) {
            val (vec, err) = embedder.embed(c)
            if (vec == null) {
                dao.deleteVectorsForDoc(docId)
                dao.deleteDocument(docId)
                throw IllegalStateException("Embedding failed: ${err ?: "unknown"}")
            }
            dim = vec.size
            rows.add(VectorRow(docId = docId, chunkIndex = done, text = c, dim = dim, vec = VectorMath.pack(VectorMath.l2normalize(vec))))
            done++
            if (done % 5 == 0 || done == chunks.size) onProgress("Embedding $done/${chunks.size}")
        }
        dao.insertVectors(rows)
        chunks.size
    }

    suspend fun search(query: String, topK: Int = 4): List<SearchHit> = withContext(Dispatchers.IO) {
        if (!embedder.isLoaded) return@withContext emptyList()
        val emb = embedder.embed(query) ?: return@withContext emptyList()
        val qv = emb.first ?: return@withContext emptyList()
        val q = VectorMath.l2normalize(qv)
        val docs = dao.allVectors().groupBy { it.docId }
        val hits = mutableListOf<SearchHit>()
        for ((docId, rows) in docs) {
            for (r in rows) {
                if (r.dim != q.size) continue
                val v = VectorMath.unpack(r.vec)
                val score = VectorMath.cosine(q, v)
                if (score > 0f) hits.add(SearchHit(docId, "", r.chunkIndex, r.text, score))
            }
        }
        hits.sortByDescending { it.score }
        hits.take(topK)
    }

    suspend fun buildRagBlock(query: String, topK: Int = 4, maxChars: Int = 2400): Pair<String?, List<String>> {
        val hits = search(query, topK)
        if (hits.isEmpty()) return null to emptyList()
        val names = docNames()
        val sb = StringBuilder("Document excerpts retrieved for context (use them if relevant, cite as sources):\n")
        var used = 0
        val sources = LinkedHashSet<String>()
        for (h in hits) {
            val name = names[h.docId] ?: "document"
            val block = "[${name}#chunk${h.chunkIndex}]\n${h.text}\n\n"
            if (used + block.length > maxChars) break
            sb.append(block)
            used += block.length
            sources.add(name)
        }
        return sb.toString() to sources.toList()
    }

    suspend fun docNames(): Map<Long, String> =
        dao.allDocuments().associate { it.id to it.name }

    suspend fun delete(docId: Long) {
        dao.deleteVectorsForDoc(docId)
        dao.deleteDocument(docId)
    }

    suspend fun clearAll() {
        dao.clearVectors()
        dao.clearDocuments()
    }
}
