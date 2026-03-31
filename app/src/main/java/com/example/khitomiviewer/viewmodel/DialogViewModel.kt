package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DialogViewModel(application: Application) : AndroidViewModel(application) {
    private val typeDao = DatabaseProvider.getDatabase(application).typeDao()
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()
    private val galleryTagDao = DatabaseProvider.getDatabase(application).galleryTagDao()

    // 다이얼로그. 태그 (싫어요,기본,좋아요,구독) 갤러리 DISLIKE, NONE, LIKE선택하는것.
    var selectedTag by mutableStateOf<Tag?>(null)
    var selectedGallery by mutableStateOf<Gallery?>(null)

    // 갤러리 디테일 정보 보여주는 다이얼로그

    val selectedGalleryDetail = mutableStateOf<GalleryFullDto?>(null)

    fun setGalleryDetail(galleryFullDto: GalleryFullDto) {
        selectedGalleryDetail.value = galleryFullDto
    }

    // 갤러리 정보 재로딩
    fun galleryDetailReloading() = viewModelScope.launch(Dispatchers.IO) {
        if (selectedGalleryDetail.value != null) {
            delay(20)
            val g = galleryDao.findById(selectedGalleryDetail.value!!.gId)
            selectedGalleryDetail.value = GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                lastReadAt = g.lastReadAt, lastReadPage = g.lastReadPage,
                tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
            )
        }
    }

    fun setTag(tag: Tag) {
        selectedTag = tag
    }

    fun setGallery(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
        selectedGallery = galleryDao.findById(gId)
    }

    fun changeTagLike(like: Int) = viewModelScope.launch(Dispatchers.IO) {
        val tagId = selectedTag?.tagId
        if (tagId != null)
            if (like == 1)
                tagDao.updateTagLike(tagId, like, 0L)
            else
                tagDao.updateTagLike(tagId, like, System.currentTimeMillis())
    }

    fun changeGalleryLike(like: Int) = viewModelScope.launch(Dispatchers.IO) {
        val gId = selectedGallery?.gId
        if (gId != null)
            if (like == 1)
                galleryDao.updateGalleryLike(gId, like, 0L)
            else
                galleryDao.updateGalleryLike(gId, like, System.currentTimeMillis())
    }
}