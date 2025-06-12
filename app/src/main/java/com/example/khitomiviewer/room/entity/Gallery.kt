package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "gallery", indices = [Index(value = ["gId"])])
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
    val download: Boolean,
    val typeId: Long,
)