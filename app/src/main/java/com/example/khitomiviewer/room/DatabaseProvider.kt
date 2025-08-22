package com.example.khitomiviewer.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: KHitomiDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 인덱스 추가
            database.execSQL("CREATE INDEX IF NOT EXISTS index_gallery_tag_gId_tagId ON gallery_tag(gId, tagId)")
        }
    }

    fun getDatabase(context: Context): KHitomiDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, KHitomiDatabase::class.java, "khitomi-db")
                .createFromAsset("database/khitomi-db-250823")
//                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it}
        }
    }
}