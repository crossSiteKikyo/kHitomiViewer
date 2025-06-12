package com.example.khitomiviewer.json

import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.GalleryTag
import com.example.khitomiviewer.room.entity.ImageUrl
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.room.entity.Type
import kotlinx.serialization.Serializable

@Serializable
data class AllData(
    val types: List<Type>,
    var galleries: List<Gallery>,
    var tags: List<Tag>,
    val galleryTags: List<GalleryTag>,
    val imageUrls: List<ImageUrl>
)
