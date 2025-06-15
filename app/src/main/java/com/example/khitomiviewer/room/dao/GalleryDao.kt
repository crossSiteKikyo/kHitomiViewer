package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.khitomiviewer.room.entity.Gallery

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

    @Query("select g.gId from gallery g")
    fun findAllGId(): List<Long>

    @Query("select * from gallery where likeStatus != 1 order by likeStatus desc")
    fun findNotNone(): List<Gallery>

    // 동적으로 쿼리를 만든다.
    @RawQuery
    fun findByCondition2(query: SupportSQLiteQuery): List<Gallery>

    // 동적으로 쿼리를 만든다.
    @RawQuery
    fun countByCondition2(query: SupportSQLiteQuery): Long

    @Query("""
        select g.* from gallery g
        join gallery_tag gt on gt.gId = g.gId
        join tag t on t.tagId = gt.tagId
        where g.likeStatus in (:galleryLikeList)
        group by g.gId
        having
            -- t.likeStatus가 0인 태그가 없어야함 --
            sum(case when t.likeStatus = 0 then 1 else 0 end) = 0
            -- tagLikeList에 있는 태그가 포함되어야함 --
            and sum(case when t.likeStatus in (:tagLikeList) then 1 else 0 end) > 0
        order by gId desc limit :limit offset :offset
        """)
    fun findByCondition(limit: Int, offset: Long, galleryLikeList: List<Int>, tagLikeList: List<Int>): List<Gallery>

    @Query("""
        select count(*) 
        from (
            select g.gId from gallery g
            join gallery_tag gt on gt.gId = g.gId
            join tag t on t.tagId = gt.tagId
            where g.likeStatus in (:galleryLikeList)
            group by g.gId
            having
                -- t.likeStatus가 0인 태그가 없어야함 --
                sum(case when t.likeStatus = 0 then 1 else 0 end) = 0
                -- tagLikeList에 있는 태그가 포함되어야함 --
                and sum(case when t.likeStatus in (:tagLikeList) then 1 else 0 end) > 0
        )
    """)
    fun countByCondition(galleryLikeList: List<Int>, tagLikeList: List<Int>): Long
}