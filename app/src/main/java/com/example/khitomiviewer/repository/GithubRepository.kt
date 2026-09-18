package com.example.khitomiviewer.repository

import com.example.khitomiviewer.api.GithubApi
import com.example.khitomiviewer.json.GithubReleasesApi

class GithubRepository(private val githubApi: GithubApi) {
    suspend fun getLatestRelease(): GithubReleasesApi = githubApi.getLatestRelease()
}
