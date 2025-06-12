package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "imageurl")
data class ImageUrl(
    @PrimaryKey(autoGenerate = true)
    val imageurlId: Long = 0,
    val gId: Long,
    val idx: Long,
    val hash: String,
    val extension: String
)