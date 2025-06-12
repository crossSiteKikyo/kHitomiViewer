package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.khitomiviewer.room.entity.Gallery
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryDao {
    @Insert
    suspend fun insert(gallery: Gallery)

    @Update
    suspend fun update(gallery: Gallery)

    @Delete
    suspend fun delete(gallery: Gallery)

    @Query("delete from gallery")
    suspend fun deleteAll()

    @Query("select exists(select 1 from gallery where gId = :gId)")
    fun existsById(gId: Long): Boolean

    @Query("delete from gallery where gId = :gId")
    suspend fun deleteById(gId: Long?)

    @Query("select gId from gallery order by gId asc limit 1")
    suspend fun findMinGid(): Long

    @Query("select count(*) from gallery")
    suspend fun count(): Long

    @Query("select * from gallery where gId = :gId")
    suspend fun findById(gId: Long): Gallery

    @Query("select * from gallery where gId = :gId")
    suspend fun findByIdNullable(gId: Long): Gallery?

    @Query("select * from gallery")
    fun findAll(): List<Gallery>

    @Query("select * from gallery where likeStatus != 1 order by likeStatus desc")
    fun findNotNone(): List<Gallery>

    @Query("""
        select * from gallery g 
        where likeStatus in (:galleryLikeList) and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus = 0 and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus in (:tagLikeList) and gt.gId = g.gId
        )
        order by gId desc limit :limit offset :offset""")
    fun findByCondition(limit: Int, offset: Long, galleryLikeList: List<Int>, tagLikeList: List<Int>): List<Gallery>

    @Query("""
        select * from gallery g 
        where likeStatus in (:galleryLikeList) and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus = 0 and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus in (:tagLikeList) and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where gt.gId = g.gId and t.tagId = :tagId
        )
        order by gId desc limit :limit offset :offset""")
    fun findByCondition2(limit: Int, offset: Long, galleryLikeList: List<Int>, tagLikeList: List<Int>, tagId: Long): List<Gallery>

    @Query("""
        select * from gallery g 
        where likeStatus in (:galleryLikeList) and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus = 0 and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus in (:tagLikeList) and gt.gId = g.gId
        )
        and title like :titleKeyword
        order by gId desc limit :limit offset :offset""")
    fun findByCondition3(limit: Int, offset: Long, galleryLikeList: List<Int>, tagLikeList: List<Int>, titleKeyword: String): List<Gallery>

    @Query("""
        select count(*) from gallery g 
        where likeStatus in (:galleryLikeList) and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus = 0 and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus in (:tagLikeList) and gt.gId = g.gId
        )
    """)
    fun countByCondition(galleryLikeList: List<Int>, tagLikeList: List<Int>): Long

    @Query("""
        select count(*) from gallery g 
        where likeStatus in (:galleryLikeList) and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus = 0 and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus in (:tagLikeList) and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where gt.gId = g.gId and t.tagId = :tagId
        )
    """)
    fun countByCondition2(galleryLikeList: List<Int>, tagLikeList: List<Int>, tagId: Long): Long

    @Query("""
        select count(*) from gallery g 
        where likeStatus in (:galleryLikeList) and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus = 0 and gt.gId = g.gId
        )
        and exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId 
            where t.likeStatus in (:tagLikeList) and gt.gId = g.gId
        )
        and title like :titleKeyword
        """)
    fun countByCondition3(galleryLikeList: List<Int>, tagLikeList: List<Int>, titleKeyword: String): Long
}