package com.example.khitomiviewer.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.khitomiviewer.room.entity.Type

@Dao
interface TypeDao {
    @Insert
    suspend fun insert(type: Type)

    @Query("delete from type")
    suspend fun deleteAll()

    @Query("select * from type")
    suspend fun findAll(): List<Type>

    @Query("select * from type where typeId = :typeId")
    suspend fun findById(typeId: Long): Type

    @Query("select * from type where name = :name")
    suspend fun findByName(name: String): Type

    @Query("select * from type where name = :name")
    suspend fun findByNameNullable(name: String): Type?
}