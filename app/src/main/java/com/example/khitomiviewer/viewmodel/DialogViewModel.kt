package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DialogViewModel(application: Application) : AndroidViewModel(application) {
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()

    // 다이얼로그. 태그 (싫어요,기본,좋아요,구독) 갤러리 DISLIKE, NONE, LIKE선택하는것.
    var selectedTag by mutableStateOf<Tag?>(null)
    var selectedGallery by mutableStateOf<Gallery?>(null)

    fun setTag(tag: Tag) {
        selectedTag = tag
    }

    fun setGallery(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
        selectedGallery = galleryDao.findById(gId)
    }

    fun changeTagLike(like: Int) = viewModelScope.launch(Dispatchers.IO) {
        val tagId = selectedTag?.tagId
        if (tagId != null)
            tagDao.updateTagLike(tagId, like)
    }

    fun changeGalleryLike(like: Int) = viewModelScope.launch(Dispatchers.IO) {
        val gId = selectedGallery?.gId
        if (gId != null)
            galleryDao.updateGalleryLike(gId, like)
    }
}