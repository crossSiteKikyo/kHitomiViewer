package com.example.khitomiviewer.room

import com.example.khitomiviewer.room.entity.Gallery

data class TagFullDto(
    val tagId: Long = 0,
    val name: String,
    val likeStatus: Int,
    val galleries: List<Gallery>
)