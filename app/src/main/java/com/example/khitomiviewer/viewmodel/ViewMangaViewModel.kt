package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.api.HitomiApi
import com.example.khitomiviewer.json.GalleryInfo
import com.example.khitomiviewer.room.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ViewMangaViewModel(application: Application) : AndroidViewModel(application) {
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()

    // 만화 보기 화면에서 사용되는 변수
    private var lastGid: Long? = null
    var imageHashes = mutableStateListOf<String>()
    var imagesLoading = mutableStateOf(true)
    var notExist by mutableStateOf(false)
    val title = mutableStateOf("제목")

    val lastPage = mutableIntStateOf(1)

    // 사용자 설정
    private val prefManager = PreferenceManager(application)
    val isRtlMode = prefManager.isRtlMode
    fun toggleRtlMode(isRtl: Boolean) = viewModelScope.launch {
        prefManager.setRtl(!isRtl)
    }

    // 보는 방법. scroll인지 swipe인지
    val viewMethod = prefManager.viewMethod
    fun setViewMethod(method: String) = viewModelScope.launch {
        prefManager.setViewMethod(method)
    }

    // 자동 넘기기 관련 변수들
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

    val hitomiApi = HitomiApi()

    // 만화 보기 화면 함수. 원래 ggjs를 크롤링 한번 했지만, 안하는 걸로 로직 변경
    fun setGalleryImages(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
        // 갤러리에 마지막 읽은 시간을 갱신한다.
        galleryDao.updateLastReadAt(gId)
        // 마지막으로 본 페이지를
        val g = galleryDao.findById(gId)
        lastPage.intValue = g.lastReadPage
        // 중복실행 방지
        if (lastGid == gId) {
            imagesLoading.value = false
            return@launch
        }
        notExist = false
        lastGid = gId
        imagesLoading.value = true
        imageHashes.clear()
        try {
            // 이제는 hitomi에서 이미지 리스트들을 직접 요청한다.
            val galleryInfo: GalleryInfo = hitomiApi.getGalleryInfo(gId.toInt())
            title.value = galleryInfo.title
            imageHashes.addAll(galleryInfo.files.map { it.hash })
            Log.i("갤러리 이미지 개수", "${galleryInfo.files.size}")
        } catch (e: Exception) {
            notExist = true
            Log.e("뭔가오류", "존재하지 않는다?")
        }

        imagesLoading.value = false
    }

    fun updateLastPage(gId: Long, page: Int) = viewModelScope.launch(Dispatchers.IO) {
        galleryDao.updateLastReadPage(gId, page)
    }
}