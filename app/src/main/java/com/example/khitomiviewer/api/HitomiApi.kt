package com.example.khitomiviewer.api

import com.example.khitomiviewer.json.GalleryInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HitomiApi {
    val hitomiClient = HttpClient(CIO) {
        defaultRequest {
            headers {
                append("Referer", "https://hitomi.la/")
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

    suspend fun getPopular(page: Long, period: String, pageSize: Int): PopularNozomiResponse {
        val startOffset = (page - 1) * pageSize * 4
        val endOffset = page * pageSize * 4 - 1
        val url = "https://ltn.gold-usergeneratedcontent.net/popular/$period-korean.nozomi"
        val response = hitomiClient.get(url) {
            headers {
                append("Range", "bytes=$startOffset-$endOffset")
            }
        }
        return PopularNozomiResponse(
            bytes = response.bodyAsBytes(),
            contentRange = response.headers["content-range"]
        )
    }

    fun parseNozomiIds(bytes: ByteArray): List<Int> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val result = mutableListOf<Int>()
        while (buffer.remaining() >= 4) {
            result.add(buffer.int)
        }
        return result
    }
}

data class PopularNozomiResponse(
    val bytes: ByteArray,
    val contentRange: String?
)
