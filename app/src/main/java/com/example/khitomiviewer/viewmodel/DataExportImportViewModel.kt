package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.json.GalleryBrief
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


// 원래 크롤링 중에 데이터 export import가 안되게 했는데 해당 로직 삭제함
class DataExportImportViewModel(application: Application) : AndroidViewModel(application) {
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()

    private val db = DatabaseProvider.getDatabase(application)
    private val repository = MyRepository(db)

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
        prettyPrint = true
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
}