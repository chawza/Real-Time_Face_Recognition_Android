package com.atharvakale.facerecognition.data

import android.content.Context
import com.atharvakale.facerecognition.data.model.RegisteredFace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceEmbeddingStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dir = File(context.filesDir, "face_embeddings").apply { mkdirs() }
    private val refreshTrigger = MutableStateFlow(0)

    val faces: Flow<List<RegisteredFace>> = refreshTrigger.map { readAll() }.flowOn(Dispatchers.IO)

    suspend fun getAll(): List<RegisteredFace> = withContext(Dispatchers.IO) { readAll() }

    suspend fun save(name: String, embedding: FloatArray) = withContext(Dispatchers.IO) {
        val fileName = "${sanitize(name)}.bin"
        val file = File(dir, fileName)
        file.outputStream().use { fos ->
            DataOutputStream(fos).use { dos ->
                val nameBytes = name.toByteArray(Charsets.UTF_8)
                dos.writeInt(nameBytes.size)
                dos.write(nameBytes)
                for (f in embedding) {
                    dos.writeFloat(f)
                }
            }
        }
        refreshTrigger.value += 1
    }

    suspend fun delete(name: String) = withContext(Dispatchers.IO) {
        val fileName = "${sanitize(name)}.bin"
        File(dir, fileName).delete()
        refreshTrigger.value += 1
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
        refreshTrigger.value += 1
    }

    private fun readAll(): List<RegisteredFace> {
        val files = dir.listFiles { _, filename -> filename.endsWith(".bin") } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                val totalBytes = file.length().toInt()
                file.inputStream().use { fis ->
                    DataInputStream(fis).use { dis ->
                        val nameLength = dis.readInt()
                        if (nameLength < 0 || nameLength > totalBytes - 4) {
                            return@mapNotNull null
                        }
                        val nameBytes = ByteArray(nameLength)
                        dis.readFully(nameBytes)
                        val storedName = String(nameBytes, Charsets.UTF_8)

                        val floatBytes = totalBytes - 4 - nameLength
                        if (floatBytes < 0 || floatBytes % 4 != 0) {
                            return@mapNotNull null
                        }
                        val floatCount = floatBytes / 4
                        val floats = FloatArray(floatCount) { dis.readFloat() }
                        RegisteredFace(storedName, floats.toList())
                    }
                }
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.name }
    }

    private fun sanitize(name: String): String {
        return name.replace("/", "_").replace("\u0000", "_")
    }
}
