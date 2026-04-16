package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.api.GithubApi
import com.example.khitomiviewer.room.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

// "UI 관련 전역 상태"만 관리하는 viewModel
class AppViewModel(application: Application) : AndroidViewModel(application) {
  private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
  private val prefManager = PreferenceManager(application)

  // Application Context를 안전하게 가져오기
  private val context get() = getApplication<Application>().applicationContext
  val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

  // ui 이벤트를 위한 변수들. toast를 띄우거나 snackbar를 띄우는데 사용한다.
  private val _uiEvent = MutableSharedFlow<UIEvent>()
  val uiEvent = _uiEvent.asSharedFlow()

  // 로드할 갤러리 수 관련
  val pageSize = prefManager.pageSize
  fun setPageSize(size: Int) = viewModelScope.launch {
    prefManager.setPageSize(size)
  }

  // avif 포맷 관련
  val isAvifFormat = prefManager.isAvifFormat
  fun setAvifFormat(flag: Boolean) = viewModelScope.launch {
    prefManager.setAvifFormat(flag)
  }

  // 갤러리 리스트 ui 관련
  val galleryListUi = prefManager.galleryListUi
  fun setGalleryListUi(v: String) = viewModelScope.launch {
    prefManager.setGalleryListUi(v)
  }

  // 갤러리 숨기기 모드
  val galleryHideMode = prefManager.galleryHideMode
  fun setGalleryhideMode(v: String) = viewModelScope.launch {
    prefManager.setGalleryHideMode(v)
  }

  // 갤러리 좋아요 상태 정렬 기준
  val galleryLikeStatusOrder = prefManager.galleryLikeStatusOrder
  fun setGalleryLikeStatusOrder(order: String) = viewModelScope.launch {
    prefManager.setGalleryLikeStatusOrder(order)
  }

  // 태그 좋아요 상태 정렬 기준
  val tagLikeStatusOrder = prefManager.tagLikeStatusOrder
  fun setTagLikeStatusOrder(order: String) = viewModelScope.launch {
    prefManager.setTagLikeStatusOrder(order)
  }

  // 볼륨키로 페이징하기 관련
  // 볼륨 키 페이징 설정값 (Activity에서 즉시 확인하기 위해 StateFlow로 변환)
  val isVolumeKeyPagingEnabled = prefManager.isVolumeKeyPaging
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  fun toggleVolumeKeyPaging(enabled: Boolean) = viewModelScope.launch {
    prefManager.setVolumeKeyPaging(enabled)
  }

  // 태그 한글화
  val tagKorean = prefManager.tagKorean
  fun setTagKorean(enabled: Boolean) = viewModelScope.launch {
    prefManager.setTagKorean(enabled)
  }

  var isPaginationActive = mutableStateOf(false)  // 현재 화면에 Pagination 컴포저블이 활성화되어 있는지 여부

  // 볼륨 키 이벤트 스트림
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
//    val inputStream = context.assets.open("tags_ko.json")
//    val inputStream2 = context.assets.open("tags_parody_ko.json")
    val translationMap = context.assets.open("tags_ko.json").use { s1 ->
      Json.decodeFromStream<Map<String, String>>(s1)
    } + context.assets.open("tags_parody_ko.json").use { s2 ->
      Json.decodeFromStream<Map<String, String>>(s2)
    }

    val targetTags = tagDao.getTagsWithNoKoreanName()
    // 번역 가능한 태그들 필터링
    val updatedTags = targetTags.mapNotNull { tag ->
      val translated = translationMap[tag.name]
      if (translated != null) tag.copy(koreanName = translated)
      else null
    }
    // DB에 일괄 반영 (Transaction 처리됨)
    if (updatedTags.isNotEmpty())
    // 너무 많을 경우를 대비해 500개씩 작업한다.
      updatedTags.chunked(500).forEach { tagDao.updateTags(it) }
    Log.i("태그 번역 개수", "${updatedTags.size}")
  }

  // 최신 버전인지 체크
  private val githubApi = GithubApi()

  fun checkLatestVersion() =
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val urlLink = "https://github.com/crossSiteKikyo/kHitomiViewer/releases/latest"

        val githubReleasesApi = githubApi.getLatestRelease()
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

        val githubReleasesApi = githubApi.getLatestRelease()
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

  // 데이터베이스 용량 최적화
  fun vacuumDataBase() = viewModelScope.launch(Dispatchers.IO) {
    try {
      // 1. 진행 중임을 알림 (선택 사항)
      _uiEvent.emit(UIEvent.ShowToast("데이터베이스 최적화 중... 기다려주세요"))
      // 2. DB 인스턴스 가져오기
      val db = DatabaseProvider.getDatabase(context)
      // 3. Room의 추상화 계층을 통하지 않고 직접 SQLite 명령 실행
      // openHelper를 사용하면 DAO 정의 없이도 모든 SQL 명령이 가능합니다.
      db.openHelper.writableDatabase.execSQL("VACUUM")
      // 4. 완료 알림
      _uiEvent.emit(UIEvent.ShowToast("최적화 완료! 용량이 절약되었습니다."))
    } catch (e: Exception) {
      _uiEvent.emit(UIEvent.ShowToast("최적화 실패: ${e.message}"))
    }
  }

  // 다크모드 관련. UI에서 관찰할 데이터들
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

  // 태그 차단 -> 차단 헤제나 차단 목록으로 가기
  // 태그 구독 -> 구독 태그 목록 보기
  // 갤러리 차단 -> 차단 헤제나 차단 목록으로 가기
  // 갤러리 북마크 -> 북마크 갤러리 목록으로 가기
  // 등등의 onAction으로 할 수 있는 것들이다.
  data class ShowSnackBar(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
  ) : UIEvent()
}