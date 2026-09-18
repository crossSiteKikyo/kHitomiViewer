package com.example.khitomiviewer.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.repository.GalleryRepository
import com.example.khitomiviewer.repository.HitomiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewMangaViewModel(
    private val galleryRepository: GalleryRepository,
    private val hitomiRepository: HitomiRepository,
    private val prefManager: PreferenceManager
) : ViewModel() {

    private var lastGid: Long? = null
    var imageHashes = mutableStateListOf<String>()
    var imagesLoading = mutableStateOf(true)
    var notExist by mutableStateOf(false)
    val title = mutableStateOf("제목")

    val lastPage = mutableIntStateOf(1)

    val isRtlMode = prefManager.isRtlMode
    fun toggleRtlMode(isRtl: Boolean) = viewModelScope.launch {
        prefManager.setRtl(!isRtl)
    }

    val viewMethod = prefManager.viewMethod
    fun setViewMethod(method: String) = viewModelScope.launch {
        prefManager.setViewMethod(method)
    }

    val isAutoPlayLoop = prefManager.isAutoPlayLoop
    fun toggleIsAutoPlayLoop(isAutoPlayLoop: Boolean) = viewModelScope.launch {
        prefManager.setAutoPlayLoop(isAutoPlayLoop)
    }

    val autoPlayPeriod = prefManager.autoPlayPeriod
    fun updateAutoPlayPeriod(period: Int) = viewModelScope.launch {
        prefManager.updateAutoPlayPeriod(period)
    }

    val tempIsLoop = mutableStateOf(false)
    val tempPeriod = mutableStateOf("")
    val isAutoPlaying = mutableStateOf(false)

    fun setGalleryImages(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
        galleryRepository.updateLastReadAt(gId)
        val g = galleryRepository.findById(gId)
        lastPage.intValue = g.lastReadPage
        if (lastGid == gId) {
            imagesLoading.value = false
            return@launch
        }
        notExist = false
        lastGid = gId
        imagesLoading.value = true
        imageHashes.clear()
        try {
            val galleryInfo = hitomiRepository.getGalleryInfo(gId.toInt())
            title.value = galleryInfo.title
            imageHashes.addAll(galleryInfo.files.map { it.hash })
            Log.i("갤러리 이미지 개수", "${galleryInfo.files.size}")
        } catch (e: Exception) {
            notExist = true
            Log.e("뭔가오류", "존재하지 않는다?")
        }
        imagesLoading.value = false
    }

    fun reloadLastPage(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
        val g = galleryRepository.findById(gId)
        lastPage.intValue = g.lastReadPage
    }

    fun updateLastPage(gId: Long, page: Int) = viewModelScope.launch(Dispatchers.IO) {
        galleryRepository.updateLastReadPage(gId, page)
    }
}
