package com.example.khitomiviewer.repository

import androidx.room.Transaction
import com.example.khitomiviewer.api.HitomiApi
import com.example.khitomiviewer.api.PopularNozomiResponse
import com.example.khitomiviewer.json.GalleryInfo
import com.example.khitomiviewer.room.KHitomiDatabase
import com.example.khitomiviewer.room.entity.Gallery
import com.example.khitomiviewer.room.entity.GalleryTag
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.room.entity.Type
import org.jsoup.Jsoup

class HitomiRepository(
    private val db: KHitomiDatabase,
    private val hitomiApi: HitomiApi
) {
    private val typeDao = db.typeDao()
    private val tagDao = db.tagDao()
    private val galleryDao = db.galleryDao()
    private val galleryTagDao = db.galleryTagDao()

    suspend fun initType() {
        val types = listOf("doujinshi", "manga", "artistcg", "gamecg", "imageset")
        for (type in types) {
            if (typeDao.findByNameNullable(type) == null)
                typeDao.insert(Type(name = type))
        }
    }

    fun initTag() {
        val tags = listOf("artist:null", "group:null", "parody:null")
        for (tag in tags) {
            if (tagDao.findByNameNullable(tag) == null)
                tagDao.insert(Tag(name = tag, likeStatus = 1))
        }
    }

    suspend fun fetchGgjsInfo(): GgjsInfo {
        val ggjs = hitomiApi.getGgjs()
        val o1 = """(?<=var o = )\d""".toRegex().find(ggjs)?.value
        val o2 = """(?<=o = )\d(?=; break;)""".toRegex().find(ggjs)?.value
        return GgjsInfo(
            b = """(?<=b: ')[^']+""".toRegex().find(ggjs)?.value,
            o1 = o1,
            o2 = o2,
            thumbChar1 = if (o1 == "0") "a" else "b",
            thumbChar2 = if (o2 == "0") "a" else "b",
            mList = """(?<=case )\d+(?=:)""".toRegex().findAll(ggjs)
                .map { it.value }
                .toList()
        )
    }

    suspend fun fetchKoreanGids(): List<Int> {
        val array = hitomiApi.getKoreanGids()
        return hitomiApi.parseNozomiIds(array)
    }

    suspend fun fetchGalleryFullInfo(gId: Int): GalleryFullInfo {
        val galleryInfo = hitomiApi.getGalleryInfo(gId)
        val html = hitomiApi.getThumbnail(galleryInfo.id)
        val doc = Jsoup.parse(html)
        val imgs = doc.select(".dj-img-cont picture img")
        val thumb1 = "https:${imgs[0].attr("data-src")}"
        val thumb2 = "https:${imgs[1].attr("data-src")}"
        return GalleryFullInfo(galleryInfo, thumb1, thumb2)
    }

    suspend fun getGalleryInfo(gId: Int): GalleryInfo = hitomiApi.getGalleryInfo(gId)

    suspend fun getPopularGidPage(page: Long, period: String, pageSize: Int): PopularGidPage {
        val response: PopularNozomiResponse = hitomiApi.getPopular(page, period, pageSize)
        val totalCount = response.contentRange
            ?.split("/")
            ?.getOrNull(1)
            ?.toLong()
            ?.div(4)
        val gIds = hitomiApi.parseNozomiIds(response.bytes).map { it.toLong() }
        return PopularGidPage(gIds = gIds, totalCount = totalCount)
    }

    @Transaction
    suspend fun insertGalleryInfo(ginfo: GalleryInfo, thumb1: String, thumb2: String) {
        if (ginfo.tags != null) {
            for (tag in ginfo.tags) {
                if (tagDao.findByNameNullable(tag.toTagString()) == null) {
                    tagDao.insert(Tag(name = tag.toTagString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.artists != null) {
            for (artist in ginfo.artists) {
                if (tagDao.findByNameNullable(artist.toArtistString()) == null) {
                    tagDao.insert(Tag(name = artist.toArtistString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.groups != null) {
            for (group in ginfo.groups) {
                if (tagDao.findByNameNullable(group.toGroupString()) == null) {
                    tagDao.insert(Tag(name = group.toGroupString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.parodys != null) {
            for (parody in ginfo.parodys) {
                if (tagDao.findByNameNullable(parody.toParodyString()) == null) {
                    tagDao.insert(Tag(name = parody.toParodyString(), likeStatus = 1))
                }
            }
        }
        if (ginfo.characters != null) {
            for (character in ginfo.characters) {
                if (tagDao.findByNameNullable(character.toCharacterString()) == null) {
                    tagDao.insert(Tag(name = character.toCharacterString(), likeStatus = 1))
                }
            }
        }
        galleryDao.insert(
            Gallery(
                ginfo.id.toLong(), ginfo.title, thumb1, thumb2, ginfo.date, ginfo.files.size,
                1, 0L, typeDao.findByName(ginfo.type).typeId, 0L, 0
            )
        )
        if (ginfo.artists == null) {
            galleryTagDao.insert(
                GalleryTag(
                    gId = ginfo.id.toLong(),
                    tagId = tagDao.findByName("artist:null").tagId
                )
            )
        } else {
            for (artist in ginfo.artists) {
                galleryTagDao.insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = tagDao.findByName(artist.toArtistString()).tagId
                    )
                )
            }
        }
        if (ginfo.groups == null) {
            galleryTagDao.insert(
                GalleryTag(
                    gId = ginfo.id.toLong(),
                    tagId = tagDao.findByName("group:null").tagId
                )
            )
        } else {
            for (group in ginfo.groups) {
                galleryTagDao.insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = tagDao.findByName(group.toGroupString()).tagId
                    )
                )
            }
        }
        if (ginfo.parodys == null) {
            galleryTagDao.insert(
                GalleryTag(
                    gId = ginfo.id.toLong(),
                    tagId = tagDao.findByName("parody:null").tagId
                )
            )
        } else {
            for (parody in ginfo.parodys) {
                galleryTagDao.insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = tagDao.findByName(parody.toParodyString()).tagId
                    )
                )
            }
        }
        if (ginfo.characters != null) {
            for (character in ginfo.characters) {
                galleryTagDao.insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = tagDao.findByName(character.toCharacterString()).tagId
                    )
                )
            }
        }
        if (ginfo.tags != null) {
            for (tag in ginfo.tags) {
                galleryTagDao.insert(
                    GalleryTag(
                        gId = ginfo.id.toLong(),
                        tagId = tagDao.findByName(tag.toTagString()).tagId
                    )
                )
            }
        }
    }

    @Transaction
    suspend fun removeGalleryInfo(gId: Long) {
        val g = galleryDao.findById(gId)
        galleryDao.delete(g)
        galleryTagDao.deleteByGid(gId)
    }

    @Transaction
    suspend fun syncGalleryTags(
        gId: Long,
        tagsToAdd: List<String>,
        tagsToRemove: List<String>,
        title: String? = null
    ) {
        for (tagName in tagsToRemove) {
            val tag = tagDao.findByNameNullable(tagName)
            if (tag != null) {
                galleryTagDao.deleteByGidAndTagId(gId, tag.tagId)
            }
        }

        for (tagName in tagsToAdd) {
            var tag = tagDao.findByNameNullable(tagName)
            if (tag == null) {
                tagDao.insert(Tag(name = tagName, likeStatus = 1))
                tag = tagDao.findByName(tagName)
            }
            galleryTagDao.insert(GalleryTag(gId = gId, tagId = tag.tagId))
        }

        if (title != null) {
            galleryDao.updateGalleryTitle(gId, title)
        }
    }

    suspend fun syncGalleryTagsFromRemote(gId: Long) {
        val galleryInfo = hitomiApi.getGalleryInfo(gId.toInt())
        val serverTagNames = mutableListOf<String>()
        if (galleryInfo.artists.isNullOrEmpty()) serverTagNames.add("artist:null")
        else serverTagNames.addAll(galleryInfo.artists.map { it.toArtistString() })
        if (galleryInfo.groups.isNullOrEmpty()) serverTagNames.add("group:null")
        else serverTagNames.addAll(galleryInfo.groups.map { it.toGroupString() })
        if (galleryInfo.parodys.isNullOrEmpty()) serverTagNames.add("parody:null")
        else serverTagNames.addAll(galleryInfo.parodys.map { it.toParodyString() })
        galleryInfo.tags?.let { serverTagNames.addAll(it.map { t -> t.toTagString() }) }
        galleryInfo.characters?.let { serverTagNames.addAll(it.map { c -> c.toCharacterString() }) }

        val galleryTagList = galleryTagDao.findByGid(gId)
        val dbTagNames = tagDao.findByIds(galleryTagList.map { it.tagId }).map { it.name }
        val tagsToAdd = serverTagNames.toSet() - dbTagNames.toSet()
        val tagsToRemove = dbTagNames.toSet() - serverTagNames.toSet()
        val title =
            if (galleryDao.findById(gId).title == galleryInfo.title) null else galleryInfo.title
        if (tagsToAdd.isNotEmpty() || tagsToRemove.isNotEmpty() || title != null) {
            syncGalleryTags(gId, tagsToAdd.toList(), tagsToRemove.toList(), title)
        }
    }
}

data class GgjsInfo(
    val b: String?,
    val o1: String?,
    val o2: String?,
    val thumbChar1: String,
    val thumbChar2: String,
    val mList: List<String>
)

data class PopularGidPage(
    val gIds: List<Long>,
    val totalCount: Long?
)

data class GalleryFullInfo(
    val galleryInfo: GalleryInfo,
    val thumb1: String,
    val thumb2: String
)
