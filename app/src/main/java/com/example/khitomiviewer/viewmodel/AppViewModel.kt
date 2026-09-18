package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.repository.GalleryRepository
import com.example.khitomiviewer.repository.GithubRepository
import com.example.khitomiviewer.repository.TagRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class AppViewModel(
  application: Application,
  private val tagRepository: TagRepository,
  private val galleryRepository: GalleryRepository,
  private val githubRepository: GithubRepository,
  private val prefManager: PreferenceManager
) : ViewModel() {
  private val context = application.applicationContext
  val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

  private val _uiEvent = MutableSharedFlow<UIEvent>()
  val uiEvent = _uiEvent.asSharedFlow()

  val pageSize = prefManager.pageSize
  fun setPageSize(size: Int) = viewModelScope.launch {
    prefManager.setPageSize(size)
  }

  val isAvifFormat = prefManager.isAvifFormat
  fun setAvifFormat(flag: Boolean) = viewModelScope.launch {
    prefManager.setAvifFormat(flag)
  }

  val galleryListUi = prefManager.galleryListUi
  fun setGalleryListUi(v: String) = viewModelScope.launch {
    prefManager.setGalleryListUi(v)
  }

  val galleryHideMode = prefManager.galleryHideMode
  fun setGalleryhideMode(v: String) = viewModelScope.launch {
    prefManager.setGalleryHideMode(v)
  }

  val galleryLikeStatusOrder = prefManager.galleryLikeStatusOrder
  fun setGalleryLikeStatusOrder(order: String) = viewModelScope.launch {
    prefManager.setGalleryLikeStatusOrder(order)
  }

  val tagLikeStatusOrder = prefManager.tagLikeStatusOrder
  fun setTagLikeStatusOrder(order: String) = viewModelScope.launch {
    prefManager.setTagLikeStatusOrder(order)
  }

  val isVolumeKeyPagingEnabled = prefManager.isVolumeKeyPaging
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  fun toggleVolumeKeyPaging(enabled: Boolean) = viewModelScope.launch {
    prefManager.setVolumeKeyPaging(enabled)
  }

  val tagKorean = prefManager.tagKorean
  fun setTagKorean(enabled: Boolean) = viewModelScope.launch {
    prefManager.setTagKorean(enabled)
  }

  var isPaginationActive = mutableStateOf(false)

  private val _volumeKeyEvent = MutableSharedFlow<VolumeKeyEvent>(extraBufferCapacity = 1)
  val volumeKeyEvent = _volumeKeyEvent.asSharedFlow()
  fun onVolumeKeyPressed(event: VolumeKeyEvent) {
    viewModelScope.launch { _volumeKeyEvent.emit(event) }
  }

  init {
    Log.i("dsf", "asdf")
    makeKoreanTag()
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun makeKoreanTag() = viewModelScope.launch(Dispatchers.IO) {
    val translationMap = context.assets.open("tags_ko.json").use { s1 ->
      Json.decodeFromStream<Map<String, String>>(s1)
    } + context.assets.open("tags_parody_ko.json").use { s2 ->
      Json.decodeFromStream<Map<String, String>>(s2)
    }

    val updatedCount = tagRepository.applyKoreanNames(translationMap)
    Log.i("태그 번역 개수", "$updatedCount")
  }

  fun checkLatestVersion() =
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val urlLink = "https://github.com/crossSiteKikyo/kHitomiViewer/releases/latest"

        val githubReleasesApi = githubRepository.getLatestRelease()
        val latest = githubReleasesApi.tag_name.removePrefix("v")
        if (!currentVersion.isNullOrBlank()) {
          if (isNewerVersion(latest, currentVersion))
            _uiEvent.emit(
              UIEvent.UpdateAvailable(
                version = latest,
                url = urlLink
              )
            )
          else {
            _uiEvent.emit(UIEvent.ShowToast("최신 버전입니다"))
          }
        }
      } catch (e: Exception) {
        Log.i("최신버전 체크 오류", "${e.message}")
        _uiEvent.emit(UIEvent.ShowToast("최신 버전 체크 오류: ${e.message}"))
      }
    }

  fun checkLatestVersionAtAppStart() =
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val urlLink = "https://github.com/crossSiteKikyo/kHitomiViewer/releases/latest"

        val githubReleasesApi = githubRepository.getLatestRelease()
        val latest = githubReleasesApi.tag_name.removePrefix("v")
        if (!currentVersion.isNullOrBlank()) {
          if (isNewerVersion(latest, currentVersion))
            _uiEvent.emit(
              UIEvent.UpdateAvailable(
                version = latest,
                url = urlLink
              )
            )
        }
      } catch (e: Exception) {
        Log.i("최신버전 체크 오류", "${e.message}")
        _uiEvent.emit(UIEvent.ShowToast("최신 버전 체크 오류: ${e.message}"))
      }
    }

  fun isNewerVersion(latest: String, current: String): Boolean {
    val latestParts = latest.split(".").map { it.toInt() }
    val currentParts = current.split(".").map { it.toInt() }

    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
      val l = latestParts.getOrElse(i) { 0 }
      val c = currentParts.getOrElse(i) { 0 }
      if (l > c) return true
      if (l < c) return false
    }
    return false
  }

  fun vacuumDataBase() = viewModelScope.launch(Dispatchers.IO) {
    try {
      _uiEvent.emit(UIEvent.ShowToast("데이터베이스 최적화 중... 기다려주세요"))
      galleryRepository.vacuum()
      _uiEvent.emit(UIEvent.ShowToast("최적화 완료! 용량이 절약되었습니다."))
    } catch (e: Exception) {
      _uiEvent.emit(UIEvent.ShowToast("최적화 실패: ${e.message}"))
    }
  }

  val isDarkMode = prefManager.isDarkMode

  fun toggleDarkMode(isDark: Boolean) = viewModelScope.launch {
    prefManager.setDarkMode(!isDark)
  }
}

enum class VolumeKeyEvent { UP, DOWN }

sealed class UIEvent {
  data class UpdateAvailable(
    val version: String,
    val url: String
  ) : UIEvent()

  data class ShowToast(val message: String) : UIEvent()

  data class ShowSnackBar(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
  ) : UIEvent()
}
