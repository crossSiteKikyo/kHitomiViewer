package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "gallery_tag",
    primaryKeys = ["gId", "tagId"],
    indices = [
        Index(value = ["tagId", "gId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Gallery::class,
            parentColumns = ["gId"],
            childColumns = ["gId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GalleryTag(
    val gId: Long,
    val tagId: Long
)