package com.example.khitomiviewer.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.repository.TagRepository
import com.example.khitomiviewer.room.TagFullDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TagViewModel(
  private val tagRepository: TagRepository
) : ViewModel() {

  var maxPage by mutableLongStateOf(1)
  val pageSize = 15
  var tagDtoList by mutableStateOf<List<TagFullDto>>(emptyList())

  fun getLikeTags(page: Long, tagLikeStatusOrder: String) = viewModelScope.launch(Dispatchers.IO) {
    tagDtoList = tagRepository.findLikeTags(page, pageSize, tagLikeStatusOrder)
  }

  fun setMaxPageLikeTags() = viewModelScope.launch(Dispatchers.IO) {
    val count = tagRepository.countLikeTags()
    maxPage = count / pageSize + if (count % pageSize > 0) 1 else 0
    if (maxPage == 0L) maxPage = 1
  }

  fun getDislikeTags(page: Long, tagLikeStatusOrder: String) =
    viewModelScope.launch(Dispatchers.IO) {
      tagDtoList = tagRepository.findDislikeTags(page, pageSize, tagLikeStatusOrder)
    }

  fun setMaxPageDislikeTags() = viewModelScope.launch(Dispatchers.IO) {
    val count = tagRepository.countDislikeTags()
    maxPage = count / pageSize + if (count % pageSize > 0) 1 else 0
    if (maxPage == 0L) maxPage = 1
  }

  fun tagReLoading() = viewModelScope.launch(Dispatchers.IO) {
    delay(20)
    val tagIdList = tagDtoList.map { t -> t.tagId }
    tagDtoList = tagRepository.findFullDtosByIdsPreserveOrder(tagIdList)
  }
}
