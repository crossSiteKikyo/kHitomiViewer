package com.example.khitomiviewer.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: KHitomiDatabase? = null

    // SQLite는 ALTER TABLE로 기본키 수정이 불가능하다
    // 그래서 "새 테이블 생성 -> 데이터 복사 -> 기존 테이블 삭제 -> 이름 변경"이라는 노가다(?) 과정을 거친다
    val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 버전2에서 버전3
            // 1. 새로운 구조의 임시 테이블 생성 (PK가 gId, tagId 복합키)
            db.execSQL(
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
            db.execSQL(
                """
                INSERT OR IGNORE INTO `gallery_tag_new` (gId, tagId)
                SELECT gId, tagId FROM gallery_tag
            """.trimIndent()
            )

            // 3. 기존 테이블 삭제
            db.execSQL("DROP TABLE gallery_tag")

            // 4. 임시 테이블 이름을 원래 이름으로 변경
            db.execSQL("ALTER TABLE gallery_tag_new RENAME TO gallery_tag")

            // 5. 요청하신 새로운 최적화 인덱스 생성 (tagId, gId)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_tag_tagId_gId` ON `gallery_tag` (`tagId`, `gId`)")

            // --- ImageUrl 테이블 삭제 ---
            db.execSQL("DROP TABLE IF EXISTS `imageurl`")

            // 버전3에서 버전4
            // 필요없는 기존의 단일 컬럼 인덱스들 삭제
            db.execSQL("DROP INDEX IF EXISTS `index_gallery_gId`")
            db.execSQL("DROP INDEX IF EXISTS `index_tag_tagId`")
            // typeId로 필터링하고 gId로 정렬하는 쿼리용
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_typeId_gId` ON `gallery` (`typeId`, `gId`)")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. lastReadAt 컬럼 추가 (기본값 0). kotlin Long도 sqlite의 INTEGER에 해당된다.
            db.execSQL("ALTER TABLE `gallery` ADD COLUMN `lastReadAt` INTEGER NOT NULL DEFAULT 0")
            // 2. lastReadPage 컬럼 추가 (기본값 0)
            db.execSQL("ALTER TABLE `gallery` ADD COLUMN `lastReadPage` INTEGER NOT NULL DEFAULT 0")
            // 3. 기록 정렬용 인덱스 추가
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_lastReadAt` ON `gallery` (`lastReadAt`)")
        }
    }

    // SQLite는 ALTER TABLE로 외래키 추가가 불가능하다
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. 새로운 구조의 임시 테이블 생성 (외래키 제약조건 포함)
            // 주의: Room이 기대하는 외래키 문법(onDelete CASCADE 등)을 정확히 맞춰야 합니다.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `gallery_tag_new` (
                    `gId` INTEGER NOT NULL, 
                    `tagId` INTEGER NOT NULL, 
                    PRIMARY KEY(`gId`, `tagId`),
                    FOREIGN KEY(`gId`) REFERENCES `gallery`(`gId`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent()
            )
            // 2. 고아 데이터(Orphaned Data)를 제외하고 데이터 복사
            // 사용자님이 제안하신 NOT EXISTS를 사용하여 성능 최적화와 데이터 정제를 동시에 수행합니다.
            db.execSQL(
                """
                INSERT INTO `gallery_tag_new` (gId, tagId)
                SELECT gId, tagId FROM gallery_tag
                WHERE EXISTS (
                    SELECT 1 FROM gallery 
                    WHERE gallery.gId = gallery_tag.gId
                )
            """.trimIndent()
            )
            // 3. 기존 테이블 삭제
            db.execSQL("DROP TABLE gallery_tag")
            // 4. 임시 테이블 이름을 원래 이름으로 변경
            db.execSQL("ALTER TABLE gallery_tag_new RENAME TO gallery_tag")
            // 5. 인덱스 재생성 (테이블이 삭제될 때 인덱스도 같이 날아가므로 다시 만들어야 합니다)
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_gallery_tag_tagId_gId` ON `gallery_tag` (`tagId`, `gId`)")
        }
    }

    fun getDatabase(context: Context): KHitomiDatabase {
        return INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                KHitomiDatabase::class.java,
                "khitomi-db"
            )
                .createFromAsset("database/khitomi-db-260329")
                .addMigrations(MIGRATION_2_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { INSTANCE = it }
        }
    }
}