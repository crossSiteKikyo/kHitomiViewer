package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gallery", indices = [
        Index(value = ["likeStatus"]),
        Index(value = ["lastReadAt"]),  // 기록 정렬용으로 추가.
        Index(value = ["typeId", "gId"]) // 추가: 필터링 + 정렬 성능 최적화
    ]
)
data class Gallery(
    @PrimaryKey
    val gId: Long,
    val title: String,
    val thumb1: String,
    val thumb2: String,
    val date: String,
    val filecount: Int,
    /**
     * 0싫어요 1기본 2좋아요
     */
    val likeStatus: Int,
    val typeId: Long,
    val lastReadAt: Long,
    val lastReadPage: Int
)