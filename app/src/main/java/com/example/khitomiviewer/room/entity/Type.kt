package com.example.khitomiviewer.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "type")
data class Type(
    @PrimaryKey(autoGenerate = true)
    val typeId: Long = 0,
    val name: String
)