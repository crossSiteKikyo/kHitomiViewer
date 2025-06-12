package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.khitomiviewer.room.entity.GalleryTag

@Dao
interface GalleryTagDao {
    @Insert
    fun insert(galleryTag: GalleryTag)

    @Query("delete from gallery_tag")
    suspend fun deleteAll()

    @Query("select * from gallery_tag where gid = :gid")
    suspend fun findByGid(gid: Long): List<GalleryTag>

    @Query("select * from gallery_tag")
    fun findAll(): List<GalleryTag>
}