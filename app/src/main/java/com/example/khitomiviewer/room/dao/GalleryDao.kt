package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.khitomiviewer.room.entity.Gallery

@Dao
interface GalleryDao {
    @Insert
    suspend fun insert(gallery: Gallery)

    @Query("update gallery set title = :title where gId = :gId")
    fun updateGalleryTitle(gId: Long, title: String)

    @Query("update gallery set likeStatus = :likeStatus, likeStatusChangedAt = :likeStatusChangedAt where gId = :gId")
    fun updateGalleryLike(gId: Long, likeStatus: Int, likeStatusChangedAt: Long)

    @Query("update gallery set lastReadAt = :timestamp where gId = :gId")
    fun updateLastReadAt(gId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("update gallery set lastReadPage = :page where gId = :gId")
    fun updateLastReadPage(gId: Long, page: Int)

    @Query("update gallery set lastReadAt = :lastReadAt, lastReadPage = :page where gId = :gId")
    fun updateRecord(gId: Long, lastReadAt: Long, page: Int)

    @Query("update gallery set lastReadAt = 0, lastReadPage = 0 where gId = :gId")
    fun resetGalleryRecord(gId: Long)

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

    @Query("select max(gId) from gallery")
    fun findMaxGid(): Long

    @Query("select count(*) from gallery")
    suspend fun count(): Long

    @Query("select * from gallery where gId = :gId")
    suspend fun findById(gId: Long): Gallery

    @Query("select * from gallery where gId = :gId")
    suspend fun findByIdNullable(gId: Long): Gallery?

    @Query("select * from gallery")
    fun findAll(): List<Gallery>

    @Query("select g.gId from gallery g order by gId desc")
    fun findAllGId(): List<Long>

    @Query("select * from gallery where likeStatus != 1 order by likeStatus desc")
    fun findLikeOrDislike(): List<Gallery>

    @Query("select * from gallery where lastReadAt != 0")
    fun findGalleryRecords(): List<Gallery>

    @Query("select * from gallery where likeStatus = 2 order by likeStatusChangedAt desc limit :limit offset :offset")
    fun findLike(limit: Int, offset: Long): List<Gallery>

    @Query("select count(gId) from gallery where likeStatus = 2")
    fun countLikeGalleries(): Long

    @Query("select * from gallery where likeStatus = 0 order by likeStatusChangedAt desc limit :limit offset :offset")
    fun findDislike(limit: Int, offset: Long): List<Gallery>

    @Query("select count(gId) from gallery where likeStatus = 0")
    fun countDislikeGalleries(): Long

    @Query(
        """
        SELECT * FROM gallery 
        WHERE EXISTS (
            SELECT 1 FROM gallery_tag 
            INNER JOIN tag ON gallery_tag.tagId = tag.tagId
            WHERE gallery_tag.gId = gallery.gId 
              AND tag.likeStatus = 2
        )
        ORDER BY gId DESC 
        LIMIT :limit OFFSET :offset
    """
    )
    fun getGalleriesWithLikedTags(limit: Int, offset: Long): List<Gallery>

    @Query(
        """
        SELECT count(gId) FROM gallery 
        WHERE EXISTS (
            SELECT 1 FROM gallery_tag 
            INNER JOIN tag ON gallery_tag.tagId = tag.tagId
            WHERE gallery_tag.gId = gallery.gId 
              AND tag.likeStatus = 2
        )
    """
    )
    fun countGalleriesWithLikedTags(): Long

    @Query(
        """
        SELECT * FROM gallery 
        WHERE lastReadAt > 0
        ORDER BY lastReadAt DESC 
        LIMIT :limit OFFSET :offset
    """
    )
    fun getRecordGalleries(limit: Int, offset: Long): List<Gallery>

    @Query("select count(gId) from gallery where lastReadAt > 0")
    fun countRecordGalleries(): Long

    // gId리스트로 검색
    @Query("select * from gallery where gId in (:gIdList)")
    fun findByGIdList(gIdList: List<Long>): List<Gallery>

    // 동적으로 쿼리를 만든다.
    @RawQuery
    fun findByConditionQuery(query: SupportSQLiteQuery): List<Gallery>

    // 동적으로 쿼리를 만든다.
    @RawQuery
    fun countByConditionQuery(query: SupportSQLiteQuery): Long

    @Query(
        """
        select * from gallery g
        where g.likeStatus != 0
        and g.typeId in (:showTypeIdList)
        and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
            where gt.gId = g.gId and t.likeStatus = 0
        )
        
        order by gId desc limit :limit offset :offset
        """
    )
    fun findByCondition(
        limit: Int,
        offset: Long,
        showTypeIdList: List<Long>
    ): List<Gallery>

    @Query(
        """
        select count(g.gId) from gallery g
        where g.likeStatus != 0
        and g.typeId in (:showTypeIdList)
        and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
            where gt.gId = g.gId and t.likeStatus = 0
        )
    """
    )
    fun countByCondition(
        showTypeIdList: List<Long>
    ): Long

    @Query(
        """
        select * from gallery g
        where g.likeStatus != 0
        and g.typeId in (:showTypeIdList)
        and g.title like :titleKeyword
        and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
            where gt.gId = g.gId and t.likeStatus = 0
        )
        
        order by gId desc limit :limit offset :offset
        """
    )
    fun findByConditionTitleKeyword(
        limit: Int,
        offset: Long,
        showTypeIdList: List<Long>,
        titleKeyword: String
    ): List<Gallery>

    @Query(
        """
        select count(g.gId) from gallery g
        where g.likeStatus != 0
        and g.typeId in (:showTypeIdList)
        and g.title like :titleKeyword
        and not exists (
            select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
            where gt.gId = g.gId and t.likeStatus = 0
        )
    """
    )
    fun countByConditionTitleKeyword(
        showTypeIdList: List<Long>,
        titleKeyword: String
    ): Long
}