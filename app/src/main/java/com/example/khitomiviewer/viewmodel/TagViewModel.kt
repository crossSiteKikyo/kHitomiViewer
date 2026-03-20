package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.TagFullDto
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()
    private val galleryTagDao = DatabaseProvider.getDatabase(application).galleryTagDao()

    var maxPage by mutableLongStateOf(1)
    val pageSize = 15
    var tagDtoList by mutableStateOf<List<TagFullDto>>(emptyList())

    // 좋아요한 태그만 로딩
    fun getLikeTags(page: Long) = viewModelScope.launch(Dispatchers.IO) {
        val tagList: List<Tag> = tagDao.findLike(pageSize, (page - 1) * pageSize)

        tagDtoList = tagList.map { t ->
            TagFullDto(
                tagId = t.tagId, name = t.name, likeStatus = t.likeStatus,
                galleries = galleryTagDao.findByTagIdLimit(t.tagId)
                    .map { gt -> galleryDao.findById(gt.gId) }
            )
        }
    }

    fun setMaxPageLikeTags() = viewModelScope.launch(Dispatchers.IO) {
        val count = tagDao.countLikeTags()
        // 올림한다.
        maxPage = count / pageSize + if (count % pageSize > 0) 1 else 0
        if (maxPage == 0L) maxPage = 1
    }

    // 싫어요한 태그만 로딩
    fun getDislikeTags(page: Long) = viewModelScope.launch(Dispatchers.IO) {
        val tagList: List<Tag> = tagDao.findDislike(pageSize, (page - 1) * pageSize)

        tagDtoList = tagList.map { t ->
            TagFullDto(
                tagId = t.tagId, name = t.name, likeStatus = t.likeStatus,
                galleries = galleryTagDao.findByTagIdLimit(t.tagId)
                    .map { gt -> galleryDao.findById(gt.gId) }
            )
        }
    }

    fun setMaxPageDislikeTags() = viewModelScope.launch(Dispatchers.IO) {
        val count = tagDao.countDislikeTags()
        // 올림한다.
        maxPage = count / pageSize + if (count % pageSize > 0) 1 else 0
        if (maxPage == 0L) maxPage = 1
    }

    // 좋아요 상태 변경시 태그 재로딩
    fun tagReLoading() = viewModelScope.launch(Dispatchers.IO) {
        delay(20)
        val tagIdList = tagDtoList.map { t -> t.tagId }
        val tagList: List<Tag> = tagDao.findByTagIdList(tagIdList).sortedBy { tag ->
            tagIdList.indexOf(tag.tagId)
        };

        tagDtoList = tagList.map { t ->
            TagFullDto(
                tagId = t.tagId, name = t.name, likeStatus = t.likeStatus,
                galleries = galleryTagDao.findByTagIdLimit(t.tagId)
                    .map { gt -> galleryDao.findById(gt.gId) }
            )
        }
    }
}