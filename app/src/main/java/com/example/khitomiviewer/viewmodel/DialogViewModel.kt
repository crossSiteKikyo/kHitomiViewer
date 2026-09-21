package com.example.khitomiviewer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.repository.GalleryRepository
import com.example.khitomiviewer.repository.TagRepository
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DialogViewModel(
    private val galleryRepository: GalleryRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    var selectedTag by mutableStateOf<Tag?>(null)
    var selectedGallery by mutableStateOf<Gallery?>(null)

    val selectedGalleryDetail = mutableStateOf<GalleryFullDto?>(null)

    fun setGalleryDetail(galleryFullDto: GalleryFullDto) {
        selectedGalleryDetail.value = galleryFullDto
    }

    fun galleryDetailReloading() = viewModelScope.launch(Dispatchers.IO) {
        if (selectedGalleryDetail.value != null) {
            delay(20)
            selectedGalleryDetail.value =
                galleryRepository.findFullDtoById(selectedGalleryDetail.value!!.gId)
        }
    }

    fun setTag(tag: Tag) {
        selectedTag = tag
    }

    fun setGallery(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
        selectedGallery = galleryRepository.findById(gId)
    }

    fun changeTagLike(like: Int) = viewModelScope.launch(Dispatchers.IO) {
        val tagId = selectedTag?.tagId
        if (tagId != null) {
            if (like == 1)
                tagRepository.updateTagLike(tagId, like, 0L)
            else
                tagRepository.updateTagLike(tagId, like, System.currentTimeMillis())
            galleryRepository.clearMatchSetCache()
        }
    }

    fun changeGalleryLike(like: Int) = viewModelScope.launch(Dispatchers.IO) {
        val gId = selectedGallery?.gId
        if (gId != null)
            if (like == 1)
                galleryRepository.updateGalleryLike(gId, like, 0L)
            else
                galleryRepository.updateGalleryLike(gId, like, System.currentTimeMillis())
    }
}
