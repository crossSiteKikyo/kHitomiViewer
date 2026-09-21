package com.example.khitomiviewer.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.khitomiviewer.room.GalleryFullDto
import com.example.khitomiviewer.room.KHitomiDatabase
import com.example.khitomiviewer.room.entity.Gallery

class GalleryRepository(private val db: KHitomiDatabase) {
    private val galleryDao = db.galleryDao()
    private val tagDao = db.tagDao()
    private val galleryTagDao = db.galleryTagDao()
    private val typeDao = db.typeDao()
    private val matchSetLock = Any()
    private var matchSetCacheKey: MatchSetCacheKey? = null
    private var matchSetCache: MutableSet<Long>? = null

    suspend fun findFullDtosByIds(gIdList: List<Long>): List<GalleryFullDto> {
        val galleryList = galleryDao.findByGIdList(gIdList).sortedBy { gallery ->
            gIdList.indexOf(gallery.gId)
        }
        return toFullDtos(galleryList)
    }

    suspend fun findLikeGalleries(page: Long, pageSize: Int, order: String): List<GalleryFullDto> {
        val limit = pageSize
        val offset = (page - 1) * pageSize
        val galleryList = when (order) {
            "statusChangedAt" -> galleryDao.findLikeOrderByLikeStatusChangedAt(limit, offset)
            else -> galleryDao.findLikeOrderBygId(limit, offset)
        }
        return toFullDtos(galleryList)
    }

    fun countLikeGalleries(): Long = galleryDao.countLikeGalleries()

    suspend fun findDislikeGalleries(page: Long, pageSize: Int, order: String): List<GalleryFullDto> {
        val limit = pageSize
        val offset = (page - 1) * pageSize
        val galleryList = when (order) {
            "statusChangedAt" -> galleryDao.findDislikeOrderByLikeStatusChangedAt(limit, offset)
            else -> galleryDao.findDislikeOrderBygId(limit, offset)
        }
        return toFullDtos(galleryList)
    }

    fun countDislikeGalleries(): Long = galleryDao.countDislikeGalleries()

    suspend fun findGalleriesWithLikedTags(page: Long, pageSize: Int): List<GalleryFullDto> {
        val galleryList = galleryDao.getGalleriesWithLikedTags(pageSize, (page - 1) * pageSize)
        return toFullDtos(galleryList)
    }

    fun countGalleriesWithLikedTags(): Long = galleryDao.countGalleriesWithLikedTags()

    suspend fun findRecordGalleries(page: Long, pageSize: Int): List<GalleryFullDto> {
        val galleryList = galleryDao.getRecordGalleries(pageSize, (page - 1) * pageSize)
        return toFullDtos(galleryList)
    }

    fun countRecordGalleries(): Long = galleryDao.countRecordGalleries()

    suspend fun findByCondition(
        page: Long,
        pageSize: Int,
        showTypeIdList: List<Long>,
        tagIdList: LongArray?,
        titleKeyword: String?
    ): List<GalleryFullDto> {
        val galleryList = if (tagIdList == null) {
            if (titleKeyword.isNullOrBlank())
                galleryDao.findByCondition(pageSize, (page - 1) * pageSize, showTypeIdList)
            else
                galleryDao.findByConditionTitleKeyword(
                    pageSize,
                    (page - 1) * pageSize,
                    showTypeIdList,
                    "%${titleKeyword}%"
                )
        } else {
            galleryDao.findByConditionQuery(
                buildFindByConditionQuery(page, pageSize, showTypeIdList, tagIdList, titleKeyword)
            )
        }
        return toFullDtos(galleryList)
    }

    fun countByCondition(
        showTypeIdList: List<Long>,
        tagIdList: LongArray?,
        titleKeyword: String?
    ): Long {
        return if (tagIdList == null) {
            if (titleKeyword.isNullOrBlank())
                galleryDao.countByCondition(showTypeIdList)
            else
                galleryDao.countByConditionTitleKeyword(showTypeIdList, "%${titleKeyword}%")
        } else {
            galleryDao.countByConditionQuery(
                buildCountByConditionQuery(showTypeIdList, tagIdList, titleKeyword)
            )
        }
    }

    fun findGidsByCondition(
        showTypeIdList: List<Long>,
        tagIdList: LongArray?,
        titleKeyword: String?
    ): Set<Long> {
        val gIds = if (tagIdList == null) {
            if (titleKeyword.isNullOrBlank())
                galleryDao.findGidsByCondition(showTypeIdList)
            else
                galleryDao.findGidsByConditionTitleKeyword(showTypeIdList, "%${titleKeyword}%")
        } else {
            galleryDao.findGidsByConditionQuery(
                buildFindGidsByConditionQuery(showTypeIdList, tagIdList, titleKeyword)
            )
        }
        return gIds.toHashSet()
    }

    suspend fun findPopularFilteredPage(
        popularGids: List<Long>,
        page: Long,
        pageSize: Int,
        showTypeIdList: List<Long>,
        tagIdList: LongArray?,
        titleKeyword: String?
    ): PopularFilteredPage {
        val matchSet = cachedOrLoadMatchSet(showTypeIdList, tagIdList, titleKeyword)
        val ranked = popularGids.filter { it in matchSet }
        val offset = ((page - 1) * pageSize).toInt().coerceAtLeast(0)
        val pageGids = ranked.drop(offset).take(pageSize)
        return PopularFilteredPage(
            galleries = findFullDtosByIds(pageGids),
            totalCount = ranked.size.toLong()
        )
    }

    fun clearMatchSetCache() {
        synchronized(matchSetLock) {
            matchSetCacheKey = null
            matchSetCache = null
        }
    }

    suspend fun findFullDtoById(gId: Long): GalleryFullDto {
        return toFullDto(galleryDao.findById(gId))
    }

    suspend fun findById(gId: Long): Gallery = galleryDao.findById(gId)

    fun resetGalleryRecord(gId: Long) = galleryDao.resetGalleryRecord(gId)

    fun updateGalleryLike(gId: Long, likeStatus: Int, likeStatusChangedAt: Long) {
        galleryDao.updateGalleryLike(gId, likeStatus, likeStatusChangedAt)
        synchronized(matchSetLock) {
            if (likeStatus == 0) matchSetCache?.remove(gId)
            else {
                matchSetCacheKey = null
                matchSetCache = null
            }
        }
    }

    fun updateLastReadAt(gId: Long) = galleryDao.updateLastReadAt(gId)

    fun updateLastReadPage(gId: Long, page: Int) = galleryDao.updateLastReadPage(gId, page)

    fun findMaxGid(): Long = galleryDao.findMaxGid()

    fun existsById(gId: Long): Boolean = galleryDao.existsById(gId)

    fun findAllGId(): List<Long> = galleryDao.findAllGId()

    fun vacuum() {
        db.openHelper.writableDatabase.execSQL("VACUUM")
    }

    private fun cachedOrLoadMatchSet(
        showTypeIdList: List<Long>,
        tagIdList: LongArray?,
        titleKeyword: String?
    ): Set<Long> {
        val key = MatchSetCacheKey(
            typeIds = showTypeIdList.sorted(),
            tagIds = tagIdList?.sorted() ?: emptyList(),
            titleKeyword = titleKeyword?.takeIf { it.isNotBlank() } ?: ""
        )
        synchronized(matchSetLock) {
            val cached = matchSetCache
            if (cached != null && matchSetCacheKey == key) return cached
        }
        val loaded = findGidsByCondition(showTypeIdList, tagIdList, titleKeyword).toMutableSet()
        synchronized(matchSetLock) {
            matchSetCacheKey = key
            matchSetCache = loaded
        }
        return loaded
    }

    private suspend fun toFullDtos(galleryList: List<Gallery>): List<GalleryFullDto> {
        return galleryList.map { toFullDto(it) }
    }

    private suspend fun toFullDto(g: Gallery): GalleryFullDto {
        return GalleryFullDto(
            gId = g.gId,
            title = g.title,
            thumb1 = g.thumb1,
            thumb2 = g.thumb2,
            date = g.date,
            filecount = g.filecount,
            likeStatus = g.likeStatus,
            typeId = g.typeId,
            typeName = typeDao.findById(g.typeId).name,
            lastReadAt = g.lastReadAt,
            lastReadPage = g.lastReadPage,
            tags = tagDao.findByIds(galleryTagDao.findByGid(g.gId).map { gt -> gt.tagId })
        )
    }

    private fun buildFindByConditionQuery(
        page: Long,
        pageSize: Int,
        showTypeIdList: List<Long>,
        tagIdList: LongArray,
        titleKeyword: String?
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val queryBuilder = StringBuilder("select * from gallery g ")
        appendConditionWhere(queryBuilder, args, showTypeIdList, tagIdList, titleKeyword)
        queryBuilder.append(""" order by gId desc limit ? offset ? """)
        args.add(pageSize)
        args.add((page - 1) * pageSize)
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    private fun buildCountByConditionQuery(
        showTypeIdList: List<Long>,
        tagIdList: LongArray,
        titleKeyword: String?
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val queryBuilder = StringBuilder("select count(*) from (select g.gId from gallery g ")
        appendConditionWhere(queryBuilder, args, showTypeIdList, tagIdList, titleKeyword)
        queryBuilder.append(" )")
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    private fun buildFindGidsByConditionQuery(
        showTypeIdList: List<Long>,
        tagIdList: LongArray,
        titleKeyword: String?
    ): SupportSQLiteQuery {
        val args = mutableListOf<Any>()
        val queryBuilder = StringBuilder("select g.gId from gallery g ")
        appendConditionWhere(queryBuilder, args, showTypeIdList, tagIdList, titleKeyword)
        return SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
    }

    private fun appendConditionWhere(
        queryBuilder: StringBuilder,
        args: MutableList<Any>,
        showTypeIdList: List<Long>,
        tagIdList: LongArray,
        titleKeyword: String?
    ) {
        queryBuilder.append(
            """
                where g.likeStatus != 0
                and g.typeId in (${showTypeIdList.joinToString(",") { "?" }})
            """.trimIndent()
        )
        args.addAll(showTypeIdList)

        if (!titleKeyword.isNullOrEmpty()) {
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
    }
}

data class PopularFilteredPage(
    val galleries: List<GalleryFullDto>,
    val totalCount: Long
)

private data class MatchSetCacheKey(
    val typeIds: List<Long>,
    val tagIds: List<Long>,
    val titleKeyword: String
)
