package com.example.khitomiviewer.room

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: KHitomiDatabase? = null

    fun getDatabase(context: Context): KHitomiDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, KHitomiDatabase::class.java, "khitomi-db")
                .createFromAsset("database/khitomi-db-250615")
                .build().also { INSTANCE = it}
        }
    }
}