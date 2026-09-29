package com.drs.ai.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.drs.ai.data.db.Document
import com.drs.ai.data.db.VectorRow
import kotlinx.coroutines.flow.Flow

@Dao
interface RagDao {
    @Insert
    suspend fun insertDocument(d: Document): Long

    @Insert
    suspend fun insertVectors(rows: List<VectorRow>)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    suspend fun allDocuments(): List<Document>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocument(id: Long): Document?

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: Long)

    @Query("DELETE FROM vector_rows WHERE docId = :docId")
    suspend fun deleteVectorsForDoc(docId: Long)

    @Query("SELECT * FROM vector_rows")
    suspend fun allVectors(): List<VectorRow>

    @Query("SELECT COUNT(*) FROM vector_rows WHERE docId = :docId")
    suspend fun vectorCountForDoc(docId: Long): Int

    @Query("SELECT COUNT(*) FROM vector_rows")
    suspend fun totalVectorCount(): Int

    @Query("DELETE FROM documents")
    suspend fun clearDocuments()

    @Query("DELETE FROM vector_rows")
    suspend fun clearVectors()
}
