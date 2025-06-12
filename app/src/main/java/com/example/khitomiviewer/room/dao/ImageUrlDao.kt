package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.khitomiviewer.room.entity.ImageUrl

@Dao
interface ImageUrlDao {
    @Insert
    fun insert(imageUrl: ImageUrl)

    @Query("delete from imageurl")
    suspend fun deleteAll()

    @Query("select * from imageurl where gId = :gId order by idx")
    suspend fun findByGId(gId: Long): List<ImageUrl>

    @Query("select * from imageurl")
    fun findAll(): List<ImageUrl>
}