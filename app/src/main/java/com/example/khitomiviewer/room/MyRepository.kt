package com.example.khitomiviewer.room

import androidx.room.Transaction
import com.example.khitomiviewer.json.AllData
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
            1, false, db.typeDao().findByName(ginfo.type).typeId))
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

    @Transaction
    suspend fun restoreDatabase(allData: AllData) {
        db.galleryDao().deleteAll()
        db.galleryTagDao().deleteAll()
        db.imageUrlDao().deleteAll()
        db.tagDao().deleteAll()
        db.typeDao().deleteAll()

        for (type in allData.types) {
            db.typeDao().insert(type)
        }
        for (gallery in allData.galleries) {
            db.galleryDao().insert(gallery)
        }
        for (galleryTag in allData.galleryTags) {
            db.galleryTagDao().insert(galleryTag)
        }
        for (imageUrl in allData.imageUrls) {
            db.imageUrlDao().insert(imageUrl)
        }
        for (tag in allData.tags) {
            db.tagDao().insert(tag)
        }
    }
}