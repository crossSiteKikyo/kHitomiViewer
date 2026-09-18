package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.repository.AppRepositories
import com.example.khitomiviewer.room.GalleryFullDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
  private val galleryRepository = AppRepositories.get(application).gallery
  private val hitomiRepository = AppRepositories.get(application).hitomi
  private val prefManager = PreferenceManager(application)

  val pageSize =
    prefManager.pageSize.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = 20
    )

  var maxPage by mutableLongStateOf(1)

  var galleries by mutableStateOf<List<GalleryFullDto>>(emptyList())
  var loading = mutableStateOf(true)

  var showTypeIdList: StateFlow<List<Long>> = prefManager.typeIdList.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = listOf(1L, 2L, 3L, 4L, 5L)
  )

  fun resetGalleryRecord(gId: Long) = viewModelScope.launch(Dispatchers.IO) {
    galleryRepository.resetGalleryRecord(gId)
    galleryReLoading()
  }

  fun findByGalleryIds(gIdList: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
    galleries = galleryRepository.findFullDtosByIds(gIdList)
  }

  fun getPopularFromHitomi(page: Long, period: String) = viewModelScope.launch(Dispatchers.IO) {
    val popular = hitomiRepository.getPopularGidPage(page, period, pageSize.value)
    popular.totalCount?.let { count -> updateMaxPage(count) }
    findByGalleryIds(popular.gIds)
  }

  fun getLikeGalleries(page: Long, galleryLikeStatusOrder: String) =
    viewModelScope.launch(Dispatchers.IO) {
      galleries = galleryRepository.findLikeGalleries(page, pageSize.value, galleryLikeStatusOrder)
    }

  fun setMaxPageLikeGalleries() = viewModelScope.launch(Dispatchers.IO) {
    updateMaxPage(galleryRepository.countLikeGalleries())
  }

  fun getDislikeGalleries(page: Long, galleryLikeStatusOrder: String) =
    viewModelScope.launch(Dispatchers.IO) {
      galleries =
        galleryRepository.findDislikeGalleries(page, pageSize.value, galleryLikeStatusOrder)
    }

  fun setMaxPageDislikeGalleries() = viewModelScope.launch(Dispatchers.IO) {
    updateMaxPage(galleryRepository.countDislikeGalleries())
  }

  fun getGalleriesWithLikedTags(page: Long) = viewModelScope.launch(Dispatchers.IO) {
    galleries = galleryRepository.findGalleriesWithLikedTags(page, pageSize.value)
  }

  fun setMaxPageGalleriesWithLikedTags() = viewModelScope.launch(Dispatchers.IO) {
    updateMaxPage(galleryRepository.countGalleriesWithLikedTags())
  }

  fun getRecordGalleries(page: Long) = viewModelScope.launch(Dispatchers.IO) {
    galleries = galleryRepository.findRecordGalleries(page, pageSize.value)
  }

  fun setMaxPageRecordGalleries() = viewModelScope.launch(Dispatchers.IO) {
    updateMaxPage(galleryRepository.countRecordGalleries())
  }

  fun galleryReLoading() = viewModelScope.launch(Dispatchers.IO) {
    delay(20)
    val gIdList = galleries.map { g -> g.gId }
    galleries = galleryRepository.findFullDtosByIds(gIdList)
  }

  fun setGalleryList(page: Long, tagIdList: LongArray?, titleKeyword: String?) {
    viewModelScope.launch(Dispatchers.IO) {
      galleries = galleryRepository.findByCondition(
        page,
        pageSize.value,
        showTypeIdList.value,
        tagIdList,
        titleKeyword
      )
      loading.value = false
    }
  }

  fun setMaxPage(tagIdList: LongArray?, titleKeyword: String?) =
    viewModelScope.launch(Dispatchers.IO) {
      updateMaxPage(
        galleryRepository.countByCondition(showTypeIdList.value, tagIdList, titleKeyword)
      )
    }

  fun typeOnOff(typeId: Long, flag: Boolean) = viewModelScope.launch {
    if (flag) {
      if (typeId !in showTypeIdList.value) {
        val newList = showTypeIdList.value.toMutableList()
        newList.add(typeId)
        prefManager.setTypeIdList(newList)
      }
    } else {
      if (typeId in showTypeIdList.value) {
        val newList = showTypeIdList.value.toMutableList()
        newList.remove(typeId)
        prefManager.setTypeIdList(newList)
      }
    }
  }

  private fun updateMaxPage(count: Long) {
    maxPage = count / pageSize.value + if (count % pageSize.value > 0) 1 else 0
    if (maxPage == 0L) maxPage = 1
  }
}
