package com.example.khitomiviewer.util

import androidx.compose.runtime.snapshots.SnapshotStateList
import coil3.network.NetworkHeaders
import kotlin.text.toInt

val hitomiHeaders = NetworkHeaders.Builder().set("Referer", "https://hitomi.la/").build()

fun decodeThumbnail(
  url: String,
  mList: SnapshotStateList<String>,
  thumbChar2: String,
  thumbChar1: String,
  isAvifFormat: Boolean
): String {
  val hash = """[0-9a-z]{40,}""".toRegex().find(url)?.value
  if (hash == null) {
    return url
  } else {
    val s = "${hash[hash.length - 1]}${hash[hash.length - 3]}${hash[hash.length - 2]}".toInt(16)
      .toString(10)
    val ch = if (s in mList) thumbChar2 else thumbChar1
    val webpUrl = url.replace("tn.hitomi.la", "${ch}tn.gold-usergeneratedcontent.net")
    return if (isAvifFormat) webpUrl.replace("webp", "avif") else webpUrl
  }
}