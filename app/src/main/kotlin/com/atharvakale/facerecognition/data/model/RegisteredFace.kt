package com.atharvakale.facerecognition.data.model

data class RegisteredFace(
    val name: String,
    val embedding: List<Float>
)
