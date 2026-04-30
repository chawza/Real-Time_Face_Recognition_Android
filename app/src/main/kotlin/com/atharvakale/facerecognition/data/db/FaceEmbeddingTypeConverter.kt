package com.atharvakale.facerecognition.data.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FaceEmbeddingTypeConverter {
    private val gson = Gson()

    @androidx.room.TypeConverter
    fun fromFloatList(value: List<Float>): String = gson.toJson(value)

    @androidx.room.TypeConverter
    fun toFloatList(value: String): List<Float> {
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(value, type)
    }
}
