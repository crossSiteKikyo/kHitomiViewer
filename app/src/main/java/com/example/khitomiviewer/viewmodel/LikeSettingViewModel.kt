package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.MyRepository
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LikeSettingViewModel(application: Application) : AndroidViewModel(application) {
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()

    var tags by mutableStateOf<List<Tag>>(emptyList())
    var galleries by mutableStateOf<List<Gallery>>(emptyList())

    // 다이얼로그. 태그 (싫어요,기본,좋아요,구독) 갤러리 DISLIKE, NONE, LIKE선택하는것.
    val selectedTagNameOrGalleryId = mutableStateOf("")
    val likeStatus = mutableIntStateOf(0)
    val whatStr = mutableStateOf("")

    private var lastTagOrGallery: String? = null

    fun setTagOrGalleryList(tagOrGallery: String?) {
        lastTagOrGallery = tagOrGallery
        viewModelScope.launch(Dispatchers.IO) {
            if(tagOrGallery == "tag") {
                tags = tagDao.findNotNone()
            }
            else {
                galleries = galleryDao.findNotNone()
            }
        }
    }

    fun setDialog(what: String, tagNameOrGalleryId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            whatStr.value = what
            val realTagNameOrGalleryId = tagNameOrGalleryId.replace("♥", "")
            if(what == "tag") {
                val findByName = tagDao.findByName(realTagNameOrGalleryId)
                likeStatus.intValue = findByName.likeStatus
            }
            else {
                val findById = galleryDao.findById(realTagNameOrGalleryId.toLong())
                likeStatus.intValue = findById.likeStatus
            }
            selectedTagNameOrGalleryId.value = realTagNameOrGalleryId
        }
    }

    fun changeLike(like: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if(whatStr.value == "tag") {
                val findByName = tagDao.findByName(selectedTagNameOrGalleryId.value)
                tagDao.update(Tag(tagId = findByName.tagId, name = findByName.name, likeStatus = like))
            }
            else {
                val g = galleryDao.findById(selectedTagNameOrGalleryId.value.toLong())
                galleryDao.update(Gallery(g.gId, g.title, g.thumb1, g.thumb2, g.date, g.filecount, like, g.download, g.typeId))
            }
            // ui 재로딩을 위해 다시 세팅
            setTagOrGalleryList(lastTagOrGallery)
        }
    }
}