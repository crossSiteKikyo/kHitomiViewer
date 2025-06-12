package com.example.khitomiviewer.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.khitomiviewer.room.dao.GalleryDao
import com.example.khitomiviewer.room.dao.GalleryTagDao
import com.example.khitomiviewer.room.dao.ImageUrlDao
import com.example.khitomiviewer.room.dao.TagDao
import com.example.khitomiviewer.room.dao.TypeDao
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.GalleryTag
import com.example.khitomiviewer.room.entity.ImageUrl
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.room.entity.Type

@Database(entities = [Type::class, Gallery::class, Tag::class, GalleryTag::class, ImageUrl::class], version = 1, exportSchema = false)
abstract class KHitomiDatabase: RoomDatabase() {
    abstract fun typeDao(): TypeDao
    abstract fun tagDao(): TagDao
    abstract fun galleryDao(): GalleryDao
    abstract fun galleryTagDao(): GalleryTagDao
    abstract fun imageUrlDao(): ImageUrlDao
}