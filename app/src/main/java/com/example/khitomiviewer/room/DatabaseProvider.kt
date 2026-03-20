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

    // SQLite는 ALTER TABLE로 기본키 수정이 불가능하다
    // 그래서 "새 테이블 생성 -> 데이터 복사 -> 기존 테이블 삭제 -> 이름 변경"이라는 노가다(?) 과정을 거친다
    val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. 새로운 구조의 임시 테이블 생성 (PK가 gId, tagId 복합키)
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gallery_tag_new` (
                    `gId` INTEGER NOT NULL, 
                    `tagId` INTEGER NOT NULL, 
                    PRIMARY KEY(`gId`, `tagId`)
                )
            """.trimIndent()
            )

            // 2. 기존 데이터 복사 (galleryTagId는 버리고 필요한 데이터만 이동)
            // 중복 데이터가 있을 수 있으므로 INSERT OR IGNORE를 사용하면 안전합니다.
            database.execSQL(
                """
                INSERT OR IGNORE INTO `gallery_tag_new` (gId, tagId)
                SELECT gId, tagId FROM gallery_tag
            """.trimIndent()
            )

            // 3. 기존 테이블 삭제
            database.execSQL("DROP TABLE gallery_tag")

            // 4. 임시 테이블 이름을 원래 이름으로 변경
            database.execSQL("ALTER TABLE gallery_tag_new RENAME TO gallery_tag")

            // 5. 요청하신 새로운 최적화 인덱스 생성 (tagId, gId)
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_tag_tagId_gId` ON `gallery_tag` (`tagId`, `gId`)")

            // --- ImageUrl 테이블 삭제 ---
            database.execSQL("DROP TABLE IF EXISTS `imageurl`")

            // 필요없는 기존의 단일 컬럼 인덱스들 삭제
            database.execSQL("DROP INDEX IF EXISTS `index_gallery_gId`")
            database.execSQL("DROP INDEX IF EXISTS `index_tag_tagId`")
            // typeId로 필터링하고 gId로 정렬하는 쿼리용
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_typeId_gId` ON `gallery` (`typeId`, `gId`)")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 필요없는 기존의 단일 컬럼 인덱스들 삭제
            database.execSQL("DROP INDEX IF EXISTS `index_gallery_gId`")
            database.execSQL("DROP INDEX IF EXISTS `index_tag_tagId`")
            // typeId로 필터링하고 gId로 정렬하는 쿼리용
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_typeId_gId` ON `gallery` (`typeId`, `gId`)")
        }
    }

    fun getDatabase(context: Context): KHitomiDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                KHitomiDatabase::class.java,
                "khitomi-db"
            )
                .createFromAsset("database/khitomi-db-260321")
                .addMigrations(MIGRATION_2_4)
                .build().also { INSTANCE = it }
        }
    }
}