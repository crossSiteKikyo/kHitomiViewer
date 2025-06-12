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

    @Query("delete from tag")
    suspend fun deleteAll()

    @Query("select * from tag")
    fun findAll(): List<Tag>

    @Query("select * from tag where tagId = :tagId")
    fun findById(tagId: Long): Tag

    @Query("select * from tag where name = :name")
    fun findByName(name: String): Tag

    @Query("select * from tag where name = :name")
    fun findByNameNullable(name: String): Tag?

    @Query("select * from tag where likeStatus != 1 order by likeStatus desc")
    fun findNotNone(): List<Tag>

    @Query("select * from tag where name like :keyword limit 10")
    fun findByKeyword(keyword: String): List<Tag>
}