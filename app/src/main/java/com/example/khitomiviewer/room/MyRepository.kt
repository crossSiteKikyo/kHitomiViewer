package com.example.khitomiviewer.room

import androidx.room.Transaction
import com.example.khitomiviewer.json.TagsAndGalleries
import com.example.khitomiviewer.json.GalleryInfo
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.GalleryTag
import com.example.khitomiviewer.room.entity.ImageUrl
import com.example.khitomiviewer.room.entity.Tag

class MyRepository(private val db: KHitomiDatabase) {

    @Transaction
    suspend fun insertGalleryInfo(ginfo:GalleryInfo, thumb1: String, thumb2: String) {
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
        db.galleryDao().insert(Gallery(ginfo.id.toLong(), ginfo.title, thumb1, thumb2, ginfo.date, ginfo.files.size,
            1, db.typeDao().findByName(ginfo.type).typeId))
        // 갤러리토큰 저장.
        // artist, group, parody가 없다면 null tag를 넣어준다.
        if(ginfo.artists == null) {
            db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName("artist:null").tagId))
        }
        else {
            for (artist in ginfo.artists) {
                db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName(artist.toArtistString()).tagId))
            }
        }
        if(ginfo.groups == null) {
            db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName("group:null").tagId))
        }
        else {
            for (group in ginfo.groups) {
                db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName(group.toGroupString()).tagId))
            }
        }
        if(ginfo.parodys == null) {
            db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName("parody:null").tagId))
        }
        else {
            for (parody in ginfo.parodys) {
                db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName(parody.toParodyString()).tagId))
            }
        }
        if(ginfo.characters != null) {
            for (character in ginfo.characters) {
                db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName(character.toCharacterString()).tagId))
            }
        }
        if(ginfo.tags != null) {
            for (tag in ginfo.tags) {
                db.galleryTagDao().insert(GalleryTag(gId = ginfo.id.toLong(), tagId = db.tagDao().findByName(tag.toTagString()).tagId))
            }
        }
        // 이미지 url 저장
        for (idx in ginfo.files.indices) {
            val file = ginfo.files[idx]
            val extension = if(file.hasavif == 1) "avif" else "webp"

            // 이미지 urldao에 저장.
            db.imageUrlDao().insert(ImageUrl(gId = ginfo.id.toLong(), idx = idx.toLong(), hash = file.hash, extension = extension))
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
        // 이미지 삭제
        db.imageUrlDao().deleteByGid(gId)
    }

    @Transaction
    suspend fun updateLikeDislikeInfo(tagsAndGalleries: TagsAndGalleries) {
        // gId는 항상 고정이다. 그러므로 gId를 기준으로 하면 됨.
        for (gallery in tagsAndGalleries.galleries) {
            val g = db.galleryDao().findByIdNullable(gallery.gId)
            if(g != null && g.likeStatus != gallery.likeStatus)
                db.galleryDao().update(Gallery(g.gId, g.title, g.thumb1, g.thumb2, g.date, g.filecount, gallery.likeStatus, g.typeId))
        }
        // tagId는 고정이 아니기 때문에 tagName을 기준으로 찾아야한다.
        for (tag in tagsAndGalleries.tags) {
            val t = db.tagDao().findByNameNullable(tag.name)
            if(t != null && t.likeStatus != tag.likeStatus)
                db.tagDao().update(Tag(t.tagId, t.name, tag.likeStatus))
        }
    }
}