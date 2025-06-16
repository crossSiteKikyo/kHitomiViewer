package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "gallery_tag", indices = [
    Index(value = ["gId", "tagId"]),
    Index(value = ["gId"]), Index(value = ["tagId"])])
data class GalleryTag(
    @PrimaryKey(autoGenerate = true)
    val galleryTagId: Long = 0,
    val gId: Long,
    val tagId: Long
)