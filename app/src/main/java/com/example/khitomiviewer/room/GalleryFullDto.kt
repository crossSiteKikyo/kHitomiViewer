package com.example.khitomiviewer.room

import com.example.khitomiviewer.room.entity.Tag

data class GalleryFullDto (
    val gId: Long,
    val title: String,
    val thumb1: String,
    val thumb2: String,
    val date: String,
    val filecount: Int,
    val likeStatus: Int,
    val download: Boolean,
    val typeId: Long,
    val typeName: String,
    val tags: List<Tag>
)