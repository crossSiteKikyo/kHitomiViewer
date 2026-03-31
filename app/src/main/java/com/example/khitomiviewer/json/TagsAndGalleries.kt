package com.example.khitomiviewer.json

import androidx.room.PrimaryKey
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.serialization.Serializable

@Serializable
data class TagsAndGalleries(
    var galleries: List<GalleryBrief>,
    var tags: List<TagBrief>,
    var galleryRecords: List<GalleryRecordBrief>? = null
)

@Serializable
data class GalleryBrief(
    val gId: Long,
    /**
     * 0싫어요 1기본 2좋아요
     */
    val likeStatus: Int,
    val likeStatusChangedAt: Long = 0,
)

@Serializable
data class TagBrief(
    val name: String,
    /**
     * 0싫어요 1기본 2좋아요
     */
    val likeStatus: Int,
    val likeStatusChangedAt: Long = 0,
)

@Serializable
data class GalleryRecordBrief(
    val gId: Long,
    val lastReadAt: Long,
    val lastReadPage: Int
)