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

  val searchLimit = 15

  fun filterTagsByKeyword(keyword: String) = viewModelScope.launch(Dispatchers.IO) {
    // 한글이 있는지 검사하고, 있다면 15까지 검색 후 가져온다.
    // 따로 변수에 저장해서 검색한다.
    // db에 잘못 번역한 것을 저장하면 어떡하나? 그러니 따로 변수에 저장하는게 맞는 것 같다.
    if (keyword.isNotBlank()) {
      var elements = listOf<Tag>();
      if (hasKorean(keyword)) {
        elements =
          tagDao.findByKeyword("%${keyword}%", selectedTags.map { tag -> tag.tagId }, searchLimit)
      } else {
        elements =
          tagDao.findByKeyword("%${keyword}%", selectedTags.map { tag -> tag.tagId }, searchLimit)
      }
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