package com.example.khitomiviewer.repository

import androidx.room.Transaction
import com.example.khitomiviewer.json.GalleryBrief
import com.example.khitomiviewer.json.GalleryRecordBrief
import com.example.khitomiviewer.json.PupilBackup
import com.example.khitomiviewer.json.TagBrief
import com.example.khitomiviewer.json.TagsAndGalleries
import com.example.khitomiviewer.room.KHitomiDatabase

class BackupRepository(private val db: KHitomiDatabase) {
    private val tagDao = db.tagDao()
    private val galleryDao = db.galleryDao()

    fun exportTagsAndGalleries(): TagsAndGalleries {
        return TagsAndGalleries(
            galleries = galleryDao.findLikeOrDislike()
                .map { g -> GalleryBrief(g.gId, g.likeStatus, g.likeStatusChangedAt) },
            tags = tagDao.findLikeOrDislike()
                .map { t -> TagBrief(t.name, t.likeStatus, t.likeStatusChangedAt) },
            galleryRecords = galleryDao.findGalleryRecords()
                .map { g -> GalleryRecordBrief(g.gId, g.lastReadAt, g.lastReadPage) }
        )
    }

    @Transaction
    fun updateLikeDislikeInfo(tagsAndGalleries: TagsAndGalleries) {
        for (gallery in tagsAndGalleries.galleries) {
            galleryDao.updateGalleryLike(gallery.gId, gallery.likeStatus, gallery.likeStatusChangedAt)
        }
        for (tag in tagsAndGalleries.tags) {
            tagDao.updateTagLikeByName(tag.name, tag.likeStatus, tag.likeStatusChangedAt)
        }
        if (tagsAndGalleries.galleryRecords != null) {
            for (galleryRecord in tagsAndGalleries.galleryRecords) {
                galleryDao.updateRecord(
                    galleryRecord.gId,
                    galleryRecord.lastReadAt,
                    galleryRecord.lastReadPage
                )
            }
        }
    }

    @Transaction
    fun importPupilBackupInfo(pupilBackup: PupilBackup) {
        for (gId in pupilBackup.favorites) {
            galleryDao.updateGalleryLike(gId, 2, 0L)
        }
        for (t in pupilBackup.favorite_tags) {
            val name = when (t.area) {
                "artist", "group", "character", "female", "male" -> "${t.area}:${t.tag}"
                "series" -> "parody:${t.tag}"
                else -> t.tag
            }
            tagDao.updateTagLikeByName(name, 2, 0L)
        }
    }

    @Transaction
    fun importVioletBookmarks(
        galleryBookmarkInfos: List<GalleryBookmarkInfo>,
        tagBookmarkInfos: List<TagBookmarkInfo>,
        articleReadLogs: List<ArticleReadLog>
    ) {
        for (gbinfo in galleryBookmarkInfos) {
            galleryDao.updateGalleryLike(gbinfo.gId, 2, gbinfo.time)
        }
        for (tbinfo in tagBookmarkInfos) {
            tagDao.updateTagLikeByName(tbinfo.name, 2, tbinfo.time)
        }
        for (a in articleReadLogs) {
            galleryDao.updateRecord(a.gId, a.lastReadAt, a.lastReadPage)
        }
    }
}

data class GalleryBookmarkInfo(
    val gId: Long,
    val time: Long
)

data class TagBookmarkInfo(
    val name: String,
    val time: Long
)

data class ArticleReadLog(
    val gId: Long,
    val lastReadAt: Long,
    val lastReadPage: Int
)
