package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.repository.AppRepositories
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
  private val tagRepository = AppRepositories.get(application).tag

  val currentSearchTags = mutableStateListOf<Tag>()
  fun setCurrentSearchTags(tagIdList: LongArray?) = viewModelScope.launch(Dispatchers.IO) {
    currentSearchTags.clear()
    selectedTags.clear()
    if (tagIdList != null && tagIdList.isNotEmpty()) {
      val tags = tagRepository.findByIds(tagIdList.toList())
      currentSearchTags.addAll(tags)
      selectedTags.addAll(tags)
    }
  }

  fun currentSearchTagsReLoading() = viewModelScope.launch(Dispatchers.IO) {
    if (currentSearchTags.isNotEmpty()) {
      delay(50)
      val tagIdList = currentSearchTags.map { t -> t.tagId }
      val tags = tagRepository.findByIds(tagIdList).sortedBy { tag -> tagIdList.indexOf(tag.tagId) }
      currentSearchTags.run {
        clear()
        addAll(tags)
      }
    }
  }

  val filteredTags = mutableStateListOf<Tag>()
  val selectedTags = mutableStateListOf<Tag>()

  val searchLimit = 15

  fun filterTagsByKeyword(keyword: String) = viewModelScope.launch(Dispatchers.IO) {
    if (keyword.isNotBlank()) {
      val elements = tagRepository.findByKeyword(
        keyword,
        selectedTags.map { tag -> tag.tagId },
        searchLimit
      )
      filteredTags.run {
        clear()
        addAll(elements)
      }
    } else {
      filteredTags.clear()
    }
  }
}

fun hasKorean(text: String): Boolean {
  val regex = "[ㄱ-ㅎㅏ-ㅣ가-힣]".toRegex()
  return regex.containsMatchIn(text)
}
