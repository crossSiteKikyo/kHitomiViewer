package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.khitomiviewer.room.entity.GalleryTag

@Dao
interface GalleryTagDao {
    @Insert
    fun insert(galleryTag: GalleryTag)

    @Query("delete from gallery_tag where gId = :gId")
    suspend fun deleteByGid(gId: Long)

    @Query("select * from gallery_tag where gId = :gId")
    suspend fun findByGid(gId: Long): List<GalleryTag>

    @Query("select * from gallery_tag where tagId = :tagId order by gid desc limit 3")
    suspend fun findByTagIdLimit(tagId: Long): List<GalleryTag>

    @Query("DELETE FROM gallery_tag WHERE gId = :gId AND tagId = :tagId")
    suspend fun deleteByGidAndTagId(gId: Long, tagId: Long)
}