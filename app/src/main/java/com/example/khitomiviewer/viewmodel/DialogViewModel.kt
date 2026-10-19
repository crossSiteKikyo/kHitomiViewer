package com.example.khitomiviewer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.repository.GalleryRepository
import com.example.khitomiviewer.repository.HitomiRepository
import com.example.khitomiviewer.repository.TagRepository
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

data class GalleryPreviewFile(
    val hash: String,
    val hasAvif: Boolean,
    val width: Int,
    val height: Int
)

class DialogViewModel(
    private val galleryRepository: GalleryRepository,
    private val tagRepository: TagRepository,
    private val hitomiRepository: HitomiRepository
) : ViewModel() {

    var selectedTag by mutableStateOf<Tag?>(null)
    var selectedGallery by mutableStateOf<Gallery?>(null)
    var previewFiles by mutableStateOf<List<GalleryPreviewFile>>(emptyList())
    var previewLoading by mutableStateOf(false)
    var previewError by mutableStateOf<String?>(null)
    var relatedGalleries by mutableStateOf<List<GalleryFullDto>>(emptyList())
    private val previewRequest = AtomicInteger(0)

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

    fun setGallery(gId: Long, resetPreviewImmediately: Boolean = true) {
        val request = previewRequest.incrementAndGet()
        if (resetPreviewImmediately) {
            previewFiles = emptyList()
            relatedGalleries = emptyList()
        }
        previewError = null
        previewLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val gallery = galleryRepository.findById(gId)
            if (request != previewRequest.get()) return@launch
            selectedGallery = gallery
            try {
                val info = hitomiRepository.getGalleryInfo(gId.toInt())
                if (request != previewRequest.get()) return@launch
                previewFiles = info.files
                    .filter { it.hash.length >= 3 }
                    .map { GalleryPreviewFile(it.hash, it.hasavif == 1, it.width, it.height) }
                relatedGalleries = if (info.related.isEmpty()) {
                    emptyList()
                } else {
                    galleryRepository.findFullDtosByIds(info.related)
                }
            } catch (e: Exception) {
                if (request == previewRequest.get()) {
                    previewFiles = emptyList()
                    relatedGalleries = emptyList()
                    previewError = "미리보기를 가져오지 못했습니다."
                }
            } finally {
                if (request == previewRequest.get()) previewLoading = false
            }
        }
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
