package com.example.khitomiviewer.json

import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.serialization.Serializable

@Serializable
data class TagsAndGalleries(
    var galleries: List<Gallery>,
    var tags: List<Tag>
)
