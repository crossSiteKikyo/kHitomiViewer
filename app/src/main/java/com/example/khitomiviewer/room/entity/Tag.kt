package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "tag", indices = [Index(value = ["likeStatus"])])
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val tagId: Long = 0,
    val name: String,
    /**
     * 0싫어요 1기본 2좋아요
     */
    val likeStatus: Int = 1,
    val likeStatusChangedAt: Long = 0L
)