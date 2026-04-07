package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 검색하는데 사용되는 변수들
class SearchViewModel(application: Application) : AndroidViewModel(application) {
  private val tagDao = DatabaseProvider.getDatabase(application).tagDao()

  // 리스트 화면 맨 위에서 검색 결과를 보여줄 때 사용하는 변수들
  // 현재 화면에서 검색에 사용된 태그들
  val currentSearchTags = mutableStateListOf<Tag>()
  fun setCurrentSearchTags(tagIdList: LongArray?) = viewModelScope.launch(Dispatchers.IO) {
    currentSearchTags.clear()
    selectedTags.clear()
    if (tagIdList != null && tagIdList.isNotEmpty()) {
      currentSearchTags.addAll(tagDao.findByIds(tagIdList.toList()))
      selectedTags.addAll(tagDao.findByIds(tagIdList.toList()))
    }
  }

  fun currentSearchTagsReLoading() = viewModelScope.launch(Dispatchers.IO) {
    if (currentSearchTags.isNotEmpty()) {
      delay(50)
      val tagIdList = currentSearchTags.map { t -> t.tagId }
      val tags = tagDao.findByIds(tagIdList).sortedBy { tag -> tagIdList.indexOf(tag.tagId) }
      currentSearchTags.run {
        clear()
        addAll(tags)
      }
    }
  }

  // 검색 시트에서 사용되는 변수들
  val filteredTags = mutableStateListOf<Tag>() // 이름으로 필터링된 추천 태그들
  val selectedTags = mutableStateListOf<Tag>() // 선택된 태그들. 검색에 사용할 태그들

  fun filterTagsByKeyword(keyword: String) = viewModelScope.launch(Dispatchers.IO) {
    filteredTags.clear()
    if (keyword.isNotBlank()) {
      filteredTags.addAll(
        tagDao.findByKeyword("%${keyword}%", selectedTags.map { tag -> tag.tagId })
      )
    }
  }
}