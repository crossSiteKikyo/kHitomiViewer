package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.json.GalleryBrief
import com.example.khitomiviewer.json.PupilBackup
import com.example.khitomiviewer.json.TagBrief
import com.example.khitomiviewer.json.TagsAndGalleries
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.MyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File
import java.time.ZoneId


// 원래 크롤링 중에 데이터 export import가 안되게 했는데 해당 로직 삭제함
class DataExportImportViewModel(application: Application) : AndroidViewModel(application) {
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()

    private val db = DatabaseProvider.getDatabase(application)
    private val repository = MyRepository(db)

    val json = Json {
        isLenient = true    //따옴표가 없는 key나 문자열 허용
        ignoreUnknownKeys = true //data class에 필요 없는 필드가 json에 있어도 SerializationException을 내지 않고 무시함
        useArrayPolymorphism = true
        prettyPrint = true  // 이쁘게 출력
    }

    // db 내보내기 불러오기에 사용되는 변수
    val dbExportImportProgress = mutableStateOf(false)
    val statusString = mutableStateOf("")

    fun getCurrentFormattedTime(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yy년MM월dd일 HH시mm분ss초")
        return current.format(formatter)
    }

    fun backupDatabaseToUri(context: Context, fileUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            dbExportImportProgress.value = true
            try {
                val tagsAndGalleries = TagsAndGalleries(
                    galleries = galleryDao.findNotNone()
                        .map { g -> GalleryBrief(g.gId, g.likeStatus) },
                    tags = tagDao.findNotNone().map { t -> TagBrief(t.name, t.likeStatus) }
                )
                context.contentResolver
                    .openOutputStream(fileUri, "w")
                    ?.use { outputStream ->
                        json.encodeToStream(tagsAndGalleries, outputStream)
                    }
                statusString.value = "내보내기 완료: ${getCurrentFormattedTime()}"
            } catch (e: Exception) {
                Log.e("내보내기 실패:", "${e.message}")
                statusString.value = "내보내기 실패: ${e.message}"
            }
            dbExportImportProgress.value = false
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun importFromJsonFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            dbExportImportProgress.value = true
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val tagsAndGalleries = json.decodeFromStream<TagsAndGalleries>(inputStream)
                    // db 에 좋아요/싫어요 정보 넣기
                    repository.updateLikeDislikeInfo(tagsAndGalleries)
                } ?: throw IOException("InputStream이 null입니다")
                statusString.value = "불러오기 완료: ${getCurrentFormattedTime()}"
            } catch (e: Exception) {
                Log.e("불러오기 실패:", "${e.message}")
                statusString.value = "불러오기 실패: ${e.message}"
            }
            dbExportImportProgress.value = false
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun importFromJsonFilePupil(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            dbExportImportProgress.value = true
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val pupilBackup = json.decodeFromStream<PupilBackup>(inputStream)
                    // db 에 좋아요/싫어요 정보 넣기
                    repository.importPupilBackupInfo(pupilBackup)
                } ?: throw IOException("InputStream이 null입니다")
                statusString.value = "불러오기 완료: ${getCurrentFormattedTime()}"
            } catch (e: Exception) {
                Log.e("불러오기 실패:", "${e.message}")
                statusString.value = "불러오기 실패: ${e.message}"
            }
            dbExportImportProgress.value = false
        }
    }

    fun importFromVioletDatabase(context: Context, uri: Uri) = viewModelScope.launch(Dispatchers.IO)
    {
        dbExportImportProgress.value = true
        // 1. 임시 파일 생성
        val tempFile = File(context.cacheDir, "violet_temp.db")
        try {
            // Uri로부터 임시 파일로 복사
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            // SQLiteDatabase로 임시 파일 열기. .use를 사용하면 close()를 알아서 해준다.
            SQLiteDatabase.openDatabase(
                tempFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { db ->
                val gIdList = mutableListOf<Long>() // 북마크 갤러리 아이디들
                val tagNameList = mutableListOf<String>() // 작가, 그룹 북마크 이름들
                val articleReadLogs = mutableListOf<ArticleReadLog>() // 읽은 히스토리(기록)
                // 쿼리 실행
                if (isTableExists(db, "BookmarkArticle")) {
                    db.rawQuery("select Article from BookmarkArticle", null).use { cursor ->
                        val index = cursor.getColumnIndex("Article")
                        if (index != -1) {
                            while (cursor.moveToNext()) {
                                gIdList.add(cursor.getLong(index))
                            }
                        }
                    }
                }
                if (isTableExists(db, "LOCAL_FAVORITES")) {
                    db.rawQuery("select GID from LOCAL_FAVORITES", null).use { cursor ->
                        val index = cursor.getColumnIndex("GID")
                        if (index != -1) {
                            while (cursor.moveToNext()) {
                                gIdList.add(cursor.getLong(index))
                            }
                        }
                    }
                }
                if (isTableExists(db, "BookmarkArtist")) {
                    db.rawQuery("select Artist from BookmarkArtist WHERE IsGroup = 0", null)
                        .use { cursor ->
                            val index = cursor.getColumnIndex("Artist")
                            if (index != -1) {
                                while (cursor.moveToNext()) {
                                    tagNameList.add("artist:${cursor.getString(index)}")
                                }
                            }
                        }
                    db.rawQuery("select Artist from BookmarkArtist WHERE IsGroup = 1", null)
                        .use { cursor ->
                            val index = cursor.getColumnIndex("Artist")
                            if (index != -1) {
                                while (cursor.moveToNext()) {
                                    tagNameList.add("group:${cursor.getString(index)}")
                                }
                            }
                        }
                }
                if (isTableExists(db, "ArticleReadLog")) {
                    db.rawQuery("select Article, DateTimeStart, LastPage from ArticleReadLog", null)
                        .use { cursor ->
                            val idx1 = cursor.getColumnIndex("Article")
                            val idx2 = cursor.getColumnIndex("DateTimeStart")
                            val idx3 = cursor.getColumnIndex("LastPage")
                            if (idx1 != -1) {
                                while (cursor.moveToNext()) {
                                    articleReadLogs.add(
                                        ArticleReadLog(
                                            cursor.getLong(idx1),
                                            convertDateStringToMillis(cursor.getString(idx2)),
                                            cursor.getInt(idx3)
                                        )
                                    )
                                }
                            }
                        }
                }
                // db에 저장
                repository.importVioletBookmarks(gIdList, tagNameList, articleReadLogs)

                statusString.value =
                    "Violet 갤러리 북마크 , 아티스트 or 그룹 북마크, 기록 불러오기 성공. 한국어 hitomi 정보에 없는 정보들은 무시됩니다."
            }
        } catch (e: Exception) {
            Log.e("Violet Import 실패:", "${e.message}")
            statusString.value = "Violet DB 불러오기 실패: ${e.message}"
        } finally {
            //임시 파일 삭제
            if (tempFile.exists())
                tempFile.delete()
            dbExportImportProgress.value = false
        }
    }

    private fun isTableExists(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    private fun convertDateStringToMillis(dateString: String): Long {
        return try {
            // 1. 입력받은 형식에 맞는 포맷터 정의 (S가 6개인 이유는 마이크로초 때문입니다)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            // 2. 문자열을 LocalDateTime 객체로 파싱
            val localDateTime = LocalDateTime.parse(dateString, formatter)
            // 3. 시스템 기본 시간대를 적용하여 Instant로 변환 후 밀리초 추출
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L // 파싱 실패 시 기본값 반환 (또는 예외를 던지셔도 됩니다)
        }
    }
}

data class ArticleReadLog(
    val gId: Long,
    val lastReadAt: Long,
    val lastReadPage: Int
)