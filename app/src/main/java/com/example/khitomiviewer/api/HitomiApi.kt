package com.example.khitomiviewer.api

import com.example.khitomiviewer.json.GalleryInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class HitomiApi {
    val hitomiClient = HttpClient(CIO) {
        defaultRequest {
            headers {
                append("Referer", "https://hitomi.la/")
//                append("Origin", "https://hitomi.la/")
//                append("Range", "bytes=0-99")
            }
        }
    }

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
        prettyPrint = true
    }

    suspend fun getKoreanGids(): ByteArray {
        val url = "https://ltn.gold-usergeneratedcontent.net/index-korean.nozomi"
        val response = hitomiClient.get(url)
        return response.bodyAsBytes()
    }

    suspend fun getGgjs(): String {
        val url = "https://ltn.gold-usergeneratedcontent.net/gg.js"
        val response = hitomiClient.get(url)
        return response.bodyAsText()
    }

    suspend fun getGalleryInfo(gId: Int): GalleryInfo {
        val url = "https://ltn.gold-usergeneratedcontent.net/galleries/${gId}.js"
        val response = hitomiClient.get(url)
        val body = response.bodyAsText().replace("var galleryinfo = ", "")
        return json.decodeFromString<GalleryInfo>(body)
    }

    suspend fun getThumbnail(id: String): String {
        val url = "https://ltn.gold-usergeneratedcontent.net/galleryblock/${id}.html"
        val response = hitomiClient.get(url)
        return response.bodyAsText()
    }

    suspend fun getPopular(page: Long, period: String, pageSize: Int): HttpResponse {
        val startOffset = (page - 1) * pageSize * 4
        val endOffset = page * pageSize * 4 - 1
        val client = HttpClient(CIO) {
            defaultRequest {
                headers {
                    append("Referer", "https://hitomi.la/")
                    append("Range", "bytes=$startOffset-$endOffset")
                }
            }
        }
        val url = "https://ltn.gold-usergeneratedcontent.net/popular/$period-korean.nozomi"
        return client.get(url)
    }
}