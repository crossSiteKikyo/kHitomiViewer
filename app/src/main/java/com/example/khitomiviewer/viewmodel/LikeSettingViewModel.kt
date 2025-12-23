package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.TagFullDto
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LikeSettingViewModel(application: Application) : AndroidViewModel(application) {
    private val typeDao = DatabaseProvider.getDatabase(application).typeDao()
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()
    private val galleryTagDao = DatabaseProvider.getDatabase(application).galleryTagDao()

    var galleries by mutableStateOf<List<GalleryFullDto>>(emptyList())
        private set
//    var tags by mutableStateOf<List<Tag>>(emptyList())
    var tags by mutableStateOf<List<TagFullDto>>(emptyList())

    // 다이얼로그. 태그 (싫어요,기본,좋아요,구독) 갤러리 DISLIKE, NONE, LIKE선택하는것.
    val selectedTagNameOrGalleryId = mutableStateOf("")
    val likeStatus = mutableIntStateOf(0)
    val whatStr = mutableStateOf("")

    private var lastTagOrGallery: String? = null
    var lastLikeStatus: Int = 2

    fun setTagOrGalleryList(tagOrGallery: String?) {
        lastTagOrGallery = tagOrGallery
        if(tagOrGallery == "tag") {
            loadTag(lastLikeStatus)
        }
        else {
            loadGallery(lastLikeStatus)
        }
    }

    fun loadGallery(likeStatus: Int) = viewModelScope.launch(Dispatchers.IO) {
        lastLikeStatus = likeStatus
        var galleryList: List<Gallery>
        if(likeStatus == 0)
            galleryList = galleryDao.findDislike()
        else
            galleryList = galleryDao.findLike()

        galleries = galleryList.map { g ->
            GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                tags = galleryTagDao.findByGid(g.gId).map { gt -> tagDao.findById(gt.tagId) }
            )
        }
    }

    fun loadTag(likeStatus: Int) = viewModelScope.launch(Dispatchers.IO) {
        lastLikeStatus = likeStatus
        var tagList: List<Tag>
        if(likeStatus == 0)
            tagList = tagDao.findDislike()
        else
            tagList = tagDao.findLike()

        tags = tagList.map { t ->
            TagFullDto(
                tagId = t.tagId, name = t.name, likeStatus = t.likeStatus,
                galleries = galleryTagDao.findByTagIdLimit(t.tagId).map { gt -> galleryDao.findById(gt.gId) }
            )
        }
    }

    // 다이얼로그의 좋아요/싫어요 상태를 누르면 해당 태그/갤러리의 좋아요상태가 변경된다.
    fun setDialog(what: String, tagNameOrGalleryId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            whatStr.value = what
            val realTagNameOrGalleryId = tagNameOrGalleryId.replace("♥", "")
            if(what == "tag") {
                val findByName = tagDao.findByName(realTagNameOrGalleryId)
                likeStatus.intValue = findByName.likeStatus
            }
            else {
                val findById = galleryDao.findById(realTagNameOrGalleryId.toLong())
                likeStatus.intValue = findById.likeStatus
            }
            selectedTagNameOrGalleryId.value = realTagNameOrGalleryId
        }
    }

    fun changeLike(like: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if(whatStr.value == "tag") {
                val findByName = tagDao.findByName(selectedTagNameOrGalleryId.value)
                tagDao.update(Tag(tagId = findByName.tagId, name = findByName.name, likeStatus = like))
            }
            else {
                val g = galleryDao.findById(selectedTagNameOrGalleryId.value.toLong())
                galleryDao.update(Gallery(g.gId, g.title, g.thumb1, g.thumb2, g.date, g.filecount, like, g.typeId))
            }
            // ui 재로딩을 위해 다시 세팅
            setTagOrGalleryList(lastTagOrGallery)
        }
    }
}