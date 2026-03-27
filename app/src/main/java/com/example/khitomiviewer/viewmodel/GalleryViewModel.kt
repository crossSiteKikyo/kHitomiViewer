package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.api.HitomiApi
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.MyRepository
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.Tag
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.addAll
import kotlin.collections.remove
import kotlin.compareTo
import kotlin.text.clear

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val typeDao = DatabaseProvider.getDatabase(application).typeDao()
    private val tagDao = DatabaseProvider.getDatabase(application).tagDao()
    private val galleryDao = DatabaseProvider.getDatabase(application).galleryDao()
    private val galleryTagDao = DatabaseProvider.getDatabase(application).galleryTagDao()

    private val prefManager = PreferenceManager(application)

    val pageSize =
        prefManager.pageSize.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 20
        )

    //    val pageSize = 15
    var maxPage by mutableLongStateOf(1)

    var galleries by mutableStateOf<List<GalleryFullDto>>(emptyList())
    var loading = mutableStateOf(true)

    val hitomiApi = HitomiApi()

    var showTypeIdList: StateFlow<List<Long>> = prefManager.typeIdList.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // 앱 실행 시 즉시 데이터 로딩 시작
        initialValue = listOf(1L, 2L, 3L, 4L, 5L)
    )

    init {
        // 처음에는 race condition이 생기므로, PreferenceManager에서 값을 불러온 후 다시 호출한다.
        viewModelScope.launch {
            prefManager.typeIdList.first()
            prefManager.pageSize.first()
            setGalleryList(1L, null, null)
            setMaxPage(null, null)
        }
    }

    // 갤러리 id들로 검색할 때.
    fun findByGalleryIds(gIdList: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        val galleryList: List<Gallery> = galleryDao.findByGIdList(gIdList).sortedBy { gallery ->
            gIdList.indexOf(gallery.gId)
        };

        galleries = galleryList.map { g ->
            GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
            )
        }
    }

    // hitomi에서 인기 갤러리 gid를 가져온다
    fun getPopularFromHitomi(page: Long, period: String) = viewModelScope.launch(Dispatchers.IO) {
        val response: HttpResponse = hitomiApi.getPopular(page, period, pageSize.value)
        // bytes 0-99/380288 형식으로 헤더에서 응답온다
        var count = response.headers["content-range"]?.split("/")[1]?.toLong()
        if (count != null) {
            count /= 4
            // 올림한다.
            maxPage = count / pageSize.value + if (count % pageSize.value > 0) 1 else 0
            if (maxPage == 0L) maxPage = 1
        }

        val array = response.bodyAsBytes()
        val gIdList = byteArrayToIntList(array).map { it.toLong() }
        findByGalleryIds(gIdList)
    }

    fun byteArrayToIntList(bytes: ByteArray): List<Int> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val result = mutableListOf<Int>()
        while (buffer.remaining() >= 4) {
            result.add(buffer.int)
        }
        return result
    }

    // 좋아요한 갤러리만 로딩
    fun getLikeGalleries(page: Long) = viewModelScope.launch(Dispatchers.IO) {
        val galleryList: List<Gallery> =
            galleryDao.findLike(pageSize.value, (page - 1) * pageSize.value)

        galleries = galleryList.map { g ->
            GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
            )
        }
    }

    fun setMaxPageLikeGalleries() = viewModelScope.launch(Dispatchers.IO) {
        val count = galleryDao.countLikeGalleries()
        // 올림한다.
        maxPage = count / pageSize.value + if (count % pageSize.value > 0) 1 else 0
        if (maxPage == 0L) maxPage = 1
    }

    // 싫어요한 갤러리만 로딩
    fun getDislikeGalleries(page: Long) = viewModelScope.launch(Dispatchers.IO) {
        val galleryList: List<Gallery> =
            galleryDao.findDislike(pageSize.value, (page - 1) * pageSize.value)

        galleries = galleryList.map { g ->
            GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
            )
        }
    }

    fun setMaxPageDislikeGalleries() = viewModelScope.launch(Dispatchers.IO) {
        val count = galleryDao.countDislikeGalleries()
        // 올림한다.
        maxPage = count / pageSize.value + if (count % pageSize.value > 0) 1 else 0
        if (maxPage == 0L) maxPage = 1
    }

    // 좋아요 태그가 하나라도 있는 갤러리만 로딩
    fun getGalleriesWithLikedTags(page: Long) = viewModelScope.launch(Dispatchers.IO) {
        val galleryList: List<Gallery> =
            galleryDao.getGalleriesWithLikedTags(pageSize.value, (page - 1) * pageSize.value)

        galleries = galleryList.map { g ->
            GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
            )
        }
    }

    fun setMaxPageGalleriesWithLikedTags() = viewModelScope.launch(Dispatchers.IO) {
        val count = galleryDao.countGalleriesWithLikedTags()
        // 올림한다.
        maxPage = count / pageSize.value + if (count % pageSize.value > 0) 1 else 0
        if (maxPage == 0L) maxPage = 1
    }

    // 좋아요 상태 변화로 인해 갤러리 재로딩
    fun galleryReLoading() = viewModelScope.launch(Dispatchers.IO) {
        delay(20)
        val gIdList = galleries.map { g -> g.gId }
        val galleryList: List<Gallery> = galleryDao.findByGIdList(gIdList).sortedBy { gallery ->
            gIdList.indexOf(gallery.gId)
        };

        galleries = galleryList.map { g ->
            GalleryFullDto(
                gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
            )
        }
    }

    // 메인 리스트 페이지에서 실행하는 함수
    fun setGalleryList(page: Long, tagIdList: LongArray?, titleKeyword: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            var galleryList: List<Gallery>
            if (tagIdList == null) {
                if (titleKeyword.isNullOrBlank())
                    galleryList = galleryDao.findByCondition(
                        pageSize.value,
                        (page - 1) * pageSize.value,
                        showTypeIdList.value
                    )
                else
                    galleryList = galleryDao.findByConditionTitleKeyword(
                        pageSize.value,
                        (page - 1) * pageSize.value,
                        showTypeIdList.value,
                        "%${titleKeyword}%"
                    )
            } else {
                galleryList = galleryDao.findByConditionQuery(
                    buildFindByConditionQuery(
                        page,
                        showTypeIdList.value,
                        tagIdList,
                        titleKeyword
                    )
                )
            }

            galleries = galleryList.map { g ->
                GalleryFullDto(
                    gId = g.gId, title = g.title, thumb1 = g.thumb1, thumb2 = g.thumb2,
                    date = g.date, filecount = g.filecount, likeStatus = g.likeStatus,
                    typeId = g.typeId, typeName = typeDao.findById(g.typeId).name,
                    tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
                )
            }
            loading.value = false
        }
    }

    fun setMaxPage(tagIdList: LongArray?, titleKeyword: String?) =
        viewModelScope.launch(Dispatchers.IO) {
            var count: Long
            if (tagIdList == null) {
                if (titleKeyword.isNullOrBlank())
                    count = galleryDao.countByCondition(
                        showTypeIdList.value
                    )
                else
                    count = galleryDao.countByConditionTitleKeyword(
                        showTypeIdList.value,
                        "%${titleKeyword}%"
                    )
            } else {
                count = galleryDao.countByConditionQuery(
                    buildCountByConditionQuery(
                        showTypeIdList.value,
                        tagIdList,
                        titleKeyword
                    )
                )
            }
            // 올림한다.
            maxPage = count / pageSize.value + if (count % pageSize.value > 0) 1 else 0
            if (maxPage == 0L) maxPage = 1
        }

    fun buildFindByConditionQuery(
        page: Long,
        showTypeIdList: List<Long>,
        tagIdList: LongArray,
        titleKeyword: String?
    ): SupportSQLiteQuery {
        val limit = pageSize.value
        val offset = (page - 1) * pageSize.value
        val args = mutableListOf<Any>()

        // 쿼리를 만든다.
        val queryBuilder = StringBuilder(
            """
                select * from gallery g
                where g.likeStatus != 0
                and g.typeId in (${showTypeIdList.joinToString(",") { "?" }})
            """.trimIndent()
        )
        args.addAll(showTypeIdList)

        // 특정 titleKeyword가 포함돼야 함
        if (titleKeyword != null && titleKeyword.isNotEmpty()) {
            queryBuilder.append(""" and g.title like ? """)
            args.add("%${titleKeyword}%")
        }

        queryBuilder.append(
            """
                and not exists (
                    select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.likeStatus = 0
                )
        """.trimIndent()
        )

        // 특정 tagId들이 포함돼야 함
        if (tagIdList.isNotEmpty()) {
            queryBuilder.append(
                """
                and (
                    select count(gt.tagId) from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.tagId in (${tagIdList.joinToString(",") { "?" }}) 
                ) = ?
            """.trimIndent()
            )

            args.addAll(tagIdList.toList())
            args.add(tagIdList.size)
        }

        queryBuilder.append(""" order by gId desc limit ? offset ? """)
        args.add(limit)
        args.add(offset)

        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    fun buildCountByConditionQuery(
        showTypeIdList: List<Long>,
        tagIdList: LongArray,
        titleKeyword: String?
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()

        // 쿼리를 만든다.
        val queryBuilder = StringBuilder(
            """
            select count(*) from (
                select g.gId from gallery g
                where g.likeStatus != 0
                and g.typeId in (${showTypeIdList.joinToString(",") { "?" }})
            """.trimIndent()
        )
        args.addAll(showTypeIdList)

        // 특정 titleKeyword가 포함돼야 함
        titleKeyword?.let {
            queryBuilder.append(""" and g.title like ? """)
            args.add("%${titleKeyword}%")
        }

        queryBuilder.append(
            """
                and not exists (
                    select 1 from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.likeStatus = 0
                )
        """.trimIndent()
        )

        // 특정 tagId들이 포함돼야 함
        if (tagIdList.isNotEmpty()) {
            queryBuilder.append(
                """
                and (
                    select count(gt.tagId) from gallery_tag gt join tag t on gt.tagId = t.tagId
                    where gt.gId = g.gId and t.tagId in (${tagIdList.joinToString(",") { "?" }}) 
                ) = ?
            """.trimIndent()
            )
            args.addAll(tagIdList.toList())
            args.add(tagIdList.size)
        }

        queryBuilder.append(""" ) """)

        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
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
}