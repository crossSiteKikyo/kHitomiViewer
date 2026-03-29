package com.example.khitomiviewer.room

import androidx.room.Transaction
import com.example.khitomiviewer.json.TagsAndGalleries
import com.example.khitomiviewer.json.GalleryInfo
import com.example.khitomiviewer.json.PupilBackup
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.GalleryTag
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.ArticleReadLog

class MyRepository(private val db: KHitomiDatabase) {
    @Transaction
    suspend fun syncGalleryTags(gId: Long, tagsToAdd: List<String>, tagsToRemove: List<String>) {
        // 1. 제거할 태그 처리 (DB에는 있는데 서버에는 없는 것)
        for (tagName in tagsToRemove) {
            val tag = db.tagDao().findByNameNullable(tagName)
            if (tag != null) {
                db.galleryTagDao().deleteByGidAndTagId(gId, tag.tagId)
            }
        }

        // 2. 추가할 태그 처리 (서버에는 있는데 DB에는 없는 것)
        for (tagName in tagsToAdd) {
            // 우선 마스터 Tag 테이블에 해당 이름이 있는지 확인
            var tag = db.tagDao().findByNameNullable(tagName)

            // 만약 처음 보는 태그라면 새로 생성
            if (tag == null) {
                db.tagDao().insert(Tag(name = tagName, likeStatus = 1))
                tag = db.tagDao().findByName(tagName) // 생성 후 다시 가져옴
            }

            // 갤러리와 태그 연결
            db.galleryTagDao().insert(GalleryTag(gId = gId, tagId = tag.tagId))
        }
    }

    // 일부만 db에 저장되는 참사를 막기 위해 트랜잭션 단위로 처리한다.
    @Transaction
    suspend fun insertGalleryInfo(ginfo: GalleryInfo, thumb1: String, thumb2: String) {
        // 태그들이 존재하는지 확인. 없다면 태그를 만든다.
        if (ginfo.tags != null) {
            for (tag in ginfo.tags) {
                if (db.tagDao().findByNameNullable(tag.toTagString()) == null) {
                    db.tagDao().insert(Tag(name = tag.toTagString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.artists != null) {
            for (artist in ginfo.artists) {
                if (db.tagDao().findByNameNullable(artist.toArtistString()) == null) {
                    db.tagDao().insert(Tag(name = artist.toArtistString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.groups != null) {
            for (group in ginfo.groups) {
                if (db.tagDao().findByNameNullable(group.toGroupString()) == null) {
                    db.tagDao().insert(Tag(name = group.toGroupString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.parodys != null) {
            for (parody in ginfo.parodys) {
                if (db.tagDao().findByNameNullable(parody.toParodyString()) == null) {
                    db.tagDao().insert(Tag(name = parody.toParodyString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.characters != null) {
            for (character in ginfo.characters) {
                if (db.tagDao().findByNameNullable(character.toCharacterString()) == null) {
                    db.tagDao().insert(Tag(name = character.toCharacterString(), likeStatus = 1))
                }
            }
        }
        // 갤러리 저장.
        db.galleryDao().insert(
            Gallery(
                ginfo.id.toLong(), ginfo.title, thumb1, thumb2, ginfo.date, ginfo.files.size,
                1, db.typeDao().findByName(ginfo.type).typeId, 0L, 0
            )
        )
        // 갤러리토큰 저장.
        // artist, group, parody가 없다면 null tag를 넣어준다.
        if (ginfo.artists == null) {
            db.galleryTagDao().insert(
                GalleryTag(
                    gId = ginfo.id.toLong(),
                    tagId = db.tagDao().findByName("artist:null").tagId
                )
            )
        } else {
            for (artist in ginfo.artists) {
                db.galleryTagDao().insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = db.tagDao().findByName(artist.toArtistString()).tagId
                    )
                )
            }
        }
        if (ginfo.groups == null) {
            db.galleryTagDao().insert(
                GalleryTag(
                    gId = ginfo.id.toLong(),
                    tagId = db.tagDao().findByName("group:null").tagId
                )
            )
        } else {
            for (group in ginfo.groups) {
                db.galleryTagDao().insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = db.tagDao().findByName(group.toGroupString()).tagId
                    )
                )
            }
        }
        if (ginfo.parodys == null) {
            db.galleryTagDao().insert(
                GalleryTag(
                    gId = ginfo.id.toLong(),
                    tagId = db.tagDao().findByName("parody:null").tagId
                )
            )
        } else {
            for (parody in ginfo.parodys) {
                db.galleryTagDao().insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = db.tagDao().findByName(parody.toParodyString()).tagId
                    )
                )
            }
        }
        if (ginfo.characters != null) {
            for (character in ginfo.characters) {
                db.galleryTagDao().insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = db.tagDao().findByName(character.toCharacterString()).tagId
                    )
                )
            }
        }
        if (ginfo.tags != null) {
            for (tag in ginfo.tags) {
                db.galleryTagDao().insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = db.tagDao().findByName(tag.toTagString()).tagId
                    )
                )
            }
        }
    }

    // 삭제된 갤러리 삭제할때 사용
    @Transaction
    suspend fun removeGalleryInfo(gId: Long) {
        // 갤러리 삭제
        val g = db.galleryDao().findById(gId)
        db.galleryDao().delete(g)
        // 갤러리 태그 삭제
        db.galleryTagDao().deleteByGid(gId)
    }

    // db 에 좋아요/싫어요 정보 넣기
    @Transaction
    fun updateLikeDislikeInfo(tagsAndGalleries: TagsAndGalleries) {
        // gId는 항상 고정이다. 그러므로 gId를 기준으로 하면 됨.
        for (gallery in tagsAndGalleries.galleries) {
            db.galleryDao().updateGalleryLike(gallery.gId, gallery.likeStatus)
        }
        // tagId는 내 앱이 아니라면 고정이 아니기 때문에 name을 기준으로 찾아야한다
        for (tag in tagsAndGalleries.tags) {
            db.tagDao().updateTagLikeByName(tag.name, tag.likeStatus)
        }
    }

    // pupil 즐겨찾기 백업 임포트
    @Transaction
    fun importPupilBackupInfo(pupilBackup: PupilBackup) {
        // gId는 항상 고정이다. 그러므로 gId를 기준으로 하면 됨.
        for (gId in pupilBackup.favorites) {
            db.galleryDao().updateGalleryLike(gId, 2)
        }
        // tagId는 내 앱이 아니라면 고정이 아니기 때문에 name을 기준으로 찾아야한다
        for (t in pupilBackup.favorite_tags) {
            val name = when (t.area) {
                "artist", "group", "character", "female", "male" -> "${t.area}:${t.tag}"
                "series" -> "parody:${t.tag}"
                else -> t.tag
            }
            db.tagDao().updateTagLikeByName(name, 2)
        }
    }

    // violet 북마크 임포트
    @Transaction
    fun importVioletBookmarks(
        gIdList: List<Long>,
        tagNameList: List<String>,
        articleReadLogs: List<ArticleReadLog>
    ) {
        // 갤러리 북마크
        for (gId in gIdList) {
            db.galleryDao().updateGalleryLike(gId, 2)
        }
        // 태그 북마크
        for (name in tagNameList) {
            db.tagDao().updateTagLikeByName(name, 2)
        }
        // 갤러리 히스토리 (기록)
        for (a in articleReadLogs) {
            db.galleryDao().updateRecord(a.gId, a.lastReadAt, a.lastReadPage)
        }
    }
}