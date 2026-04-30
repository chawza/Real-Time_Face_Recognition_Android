package com.atharvakale.facerecognition.ml

import com.atharvakale.facerecognition.data.db.FaceEmbeddingEntity
import android.util.Pair
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class FaceVerifier @Inject constructor() {

    data class MatchResult(
        val name: String,
        val distance: Float
    )

    fun findNearest(embedding: FloatArray, registered: List<FaceEmbeddingEntity>): MatchResult? {
        var best: MatchResult? = null
        for (face in registered) {
            val dist = euclideanDistance(embedding, face.embedding.toFloatArray())
            if (best == null || dist < best.distance) {
                best = MatchResult(face.name, dist)
            }
        }
        return best
    }

    fun findNearestTwo(embedding: FloatArray, registered: List<FaceEmbeddingEntity>): List<MatchResult?> {
        var first: MatchResult? = null
        var second: MatchResult? = null
        for (face in registered) {
            val dist = euclideanDistance(embedding, face.embedding.toFloatArray())
            if (first == null || dist < first.distance) {
                second = first
                first = MatchResult(face.name, dist)
            } else if (second == null || dist < second.distance) {
                second = MatchResult(face.name, dist)
            }
        }
        return listOf(first, second ?: first)
    }

    fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0f
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }
}
