package com.example.khitomiviewer.json

import kotlinx.serialization.Serializable

@Serializable
class GithubReleasesApi(
    var tag_name: String,
    var name: String
)