package com.example.khitomiviewer.json

import kotlinx.serialization.Serializable

@Serializable
data class GalleryInfo(
    val id: String,
    val title: String,
//    val japanese_title: String? = null,
//    val language: String? = null,
    val type: String,
    val date: String,
    val artists: List<Artist>? = null,
    val groups: List<Group>? = null,
    val parodys: List<Parody>? = null,
    val tags: List<Tag>? = null,
//    val related: List<Int> = emptyList(),
//    val languages: List<Language> = emptyList(),
    val characters: List<Character>? = null,
//    val scene_indexes: List<Int>? = emptyList(),
    val files: List<GalleryFiles> = emptyList(),
)

@Serializable
data class Artist(
    val artist: String,
    val url: String,
) {
    fun toArtistString():String {
        return "artist:${artist}"
    }
}

@Serializable
data class Group(
    val group: String,
    val url: String,
) {
    fun toGroupString():String {
        return "group:${group}"
    }
}

@Serializable
data class Parody(
    val parody: String,
    val url: String,
) {
    fun toParodyString():String {
        return "parody:${parody}"
    }
}

@Serializable
data class Character(
    val character: String,
    val url: String,
) {
    fun toCharacterString():String {
        return "character:${character}"
    }
}

@Serializable
data class Tag(
    val tag: String,
    val url: String,
    val female: String? = null,
    val male: String? = null,
) {
    fun toTagString(): String {
        return if(female == "1")
            "female:$tag"
        else if(male == "1")
            "male:$tag"
        else
            tag
    }
}

@Serializable
data class Language(
    val galleryid: String,
    val url: String,
    val language_localname: String,
    val name: String,
)

@Serializable
data class GalleryFiles(
    val width: Int,
    val hash: String,
    val haswebp: Int = 0,
    val name: String,
    val height: Int,
    val hasavif: Int = 0,
    val hasavifsmalltn: Int? = 0
)