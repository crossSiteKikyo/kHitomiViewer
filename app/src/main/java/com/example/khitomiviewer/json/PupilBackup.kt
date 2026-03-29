package com.example.khitomiviewer.json

import kotlinx.serialization.Serializable

@Serializable
data class PupilBackup(
    var favorites: List<Long>,
    var favorite_tags: List<PupilTagInfo>
)

@Serializable
data class PupilTagInfo(
    val area: String,   // artist group series character female male 같은 접두사
    val tag: String     // 태그 이름
)