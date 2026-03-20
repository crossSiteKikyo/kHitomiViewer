package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.khitomiviewer.room.entity.Tag

@Dao
interface TagDao {
    @Insert
    fun insert(tag: Tag)

    @Update
    fun update(tag: Tag)

    @Query("update tag set likeStatus = :likeStatus where tag.tagId = :tagId ")
    fun updateTagLike(tagId: Long, likeStatus: Int)

    @Query("update tag set likeStatus = :likeStatus where tag.name = :name ")
    fun updateTagLikeByName(name: String, likeStatus: Int)

    @Query("delete from tag")
    suspend fun deleteAll()

    @Query("select * from tag")
    fun findAll(): List<Tag>

    @Query("select * from tag where tagId = :tagId")
    fun findById(tagId: Long): Tag

    @Query("select * from tag where tagId = :tagId")
    fun findByIdNullable(tagId: Long): Tag?

    @Query("select * from tag where tagId in (:tagIdList)")
    fun findByIds(tagIdList: List<Long>): List<Tag>

    @Query("select * from tag where name = :name")
    fun findByName(name: String): Tag

    @Query("select * from tag where name = :name")
    fun findByNameNullable(name: String): Tag?

    @Query("select * from tag where likeStatus != 1 order by likeStatus desc")
    fun findNotNone(): List<Tag>

    @Query("select * from tag where tagId in (:tagIdList)")
    fun findByTagIdList(tagIdList: List<Long>): List<Tag>

    @Query("select * from tag where likeStatus = 2 order by name limit :limit offset :offset")
    fun findLike(limit: Int, offset: Long): List<Tag>

    @Query("select count(tagId) from tag where likeStatus = 2")
    fun countLikeTags(): Long

    @Query("select * from tag where likeStatus = 0 order by name limit :limit offset :offset")
    fun findDislike(limit: Int, offset: Long): List<Tag>

    @Query("select count(tagId) from tag where likeStatus = 0")
    fun countDislikeTags(): Long

    @Query("select * from tag where name like :keyword and tagId not in (:tagIds) limit 15")
    fun findByKeyword(keyword: String, tagIds: List<Long>): List<Tag>
}