package com.example.khitomiviewer.room

import com.example.khitomiviewer.room.entity.Tag

data class GalleryFullDto(
    val gId: Long,
    val title: String,
    val thumb1: String,
    val thumb2: String,
    val date: String,
    val filecount: Int,
    val likeStatus: Int,
    val typeId: Long,
    val typeName: String,
    val lastReadAt: Long,
    val lastReadPage: Int,
    val tags: List<Tag>
)