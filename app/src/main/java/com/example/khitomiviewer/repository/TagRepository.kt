package com.example.khitomiviewer.repository

import com.example.khitomiviewer.room.KHitomiDatabase
import com.example.khitomiviewer.room.TagFullDto
import com.example.khitomiviewer.room.entity.Tag

class TagRepository(private val db: KHitomiDatabase) {
    private val tagDao = db.tagDao()
    private val galleryDao = db.galleryDao()
    private val galleryTagDao = db.galleryTagDao()

    suspend fun findLikeTags(page: Long, pageSize: Int, order: String): List<TagFullDto> {
        val limit = pageSize
        val offset = (page - 1) * pageSize
        val tagList = when (order) {
            "statusChangedAt" -> tagDao.findLikeOrderByLikeStatusChangedAt(limit, offset)
            else -> tagDao.findLikeOrderByName(limit, offset)
        }
        return toFullDtos(tagList)
    }

    fun countLikeTags(): Long = tagDao.countLikeTags()

    suspend fun findDislikeTags(page: Long, pageSize: Int, order: String): List<TagFullDto> {
        val limit = pageSize
        val offset = (page - 1) * pageSize
        val tagList = when (order) {
            "statusChangedAt" -> tagDao.findDislikeOrderByLikeStatusChangedAt(limit, offset)
            else -> tagDao.findDislikeOrderByName(limit, offset)
        }
        return toFullDtos(tagList)
    }

    fun countDislikeTags(): Long = tagDao.countDislikeTags()

    suspend fun findFullDtosByIdsPreserveOrder(tagIdList: List<Long>): List<TagFullDto> {
        val tagList = tagDao.findByTagIdList(tagIdList).sortedBy { tag ->
            tagIdList.indexOf(tag.tagId)
        }
        return toFullDtos(tagList)
    }

    fun findByIds(tagIdList: List<Long>): List<Tag> = tagDao.findByIds(tagIdList)

    fun findByKeyword(keyword: String, excludedTagIds: List<Long>, limit: Int): List<Tag> {
        return tagDao.findByKeyword("%${keyword}%", excludedTagIds, limit)
    }

    fun updateTagLike(tagId: Long, likeStatus: Int, likeStatusChangedAt: Long) =
        tagDao.updateTagLike(tagId, likeStatus, likeStatusChangedAt)

    fun applyKoreanNames(translationMap: Map<String, String>): Int {
        val targetTags = tagDao.getTagsWithNoKoreanName()
        val updatedTags = targetTags.mapNotNull { tag ->
            val translated = translationMap[tag.name]
            if (translated != null) tag.copy(koreanName = translated)
            else null
        }
        if (updatedTags.isNotEmpty()) {
            updatedTags.chunked(500).forEach { tagDao.updateTags(it) }
        }
        return updatedTags.size
    }

    private suspend fun toFullDtos(tagList: List<Tag>): List<TagFullDto> {
        return tagList.map { t ->
            TagFullDto(
                tagId = t.tagId,
                name = t.name,
                koreanName = t.koreanName,
                likeStatus = t.likeStatus,
                galleries = galleryTagDao.findByTagIdLimit(t.tagId)
                    .map { gt -> galleryDao.findById(gt.gId) }
            )
        }
    }
}
