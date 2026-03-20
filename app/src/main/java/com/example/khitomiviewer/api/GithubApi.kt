package com.example.khitomiviewer.api

import com.example.khitomiviewer.json.GithubReleasesApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

class GithubApi {
    val githubClient = HttpClient(CIO)
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
        prettyPrint = true
    }

    suspend fun getLatestRelease(): GithubReleasesApi {
        val url = "https://api.github.com/repos/crossSiteKikyo/kHitomiViewer/releases/latest"
        val response = githubClient.get(url)
        val body = response.bodyAsText()
        return json.decodeFromString<GithubReleasesApi>(body)
    }
}