package com.atharvakale.facerecognition.ml

import com.atharvakale.facerecognition.data.model.RegisteredFace
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class FaceVerifier @Inject constructor() {

    data class MatchResult(
        val name: String,
        val similarity: Float
    )

    fun findNearest(embedding: FloatArray, registered: List<RegisteredFace>): MatchResult? {
        var best: MatchResult? = null
        for (face in registered) {
            val sim = cosineSimilarity(embedding, face.embedding.toFloatArray())
            if (best == null || sim > best.similarity) {
                best = MatchResult(face.name, sim)
            }
        }
        return best
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator < 1e-10f) return 0f
        return (dot / denominator).coerceIn(-1f, 1f)
    }
}
