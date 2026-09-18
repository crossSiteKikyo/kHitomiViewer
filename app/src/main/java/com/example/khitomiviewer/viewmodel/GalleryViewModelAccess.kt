package com.example.khitomiviewer.viewmodel

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel

object GalleryViewModelKeys {
  const val LIST = "list"
  const val MY_GALLERY = "myGallery"
  const val DISLIKE_GALLERY = "dislikeGallery"
  const val SUBSCRIPTION = "subscription"
  const val RECORD = "record"
  const val RANK = "rank"

  fun fromRoute(route: String?): String? {
    if (route == null) return null
    return when {
      route.startsWith("ListScreen") -> LIST
      route.startsWith("MyGalleryScreen") -> MY_GALLERY
      route.startsWith("DislikeGalleryScreen") -> DISLIKE_GALLERY
      route.startsWith("SubscriptionScreen") -> SUBSCRIPTION
      route.startsWith("RecordScreen") -> RECORD
      route.startsWith("RankScreen") -> RANK
      else -> null
    }
  }
}

val LocalGalleryViewModelKey = staticCompositionLocalOf<String?> { null }

@Composable
fun ProvideGalleryViewModelKey(key: String, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalGalleryViewModelKey provides key, content = content)
}

@Composable
fun activityGalleryViewModel(key: String): GalleryViewModel {
  val activity = LocalActivity.current as ComponentActivity
  return viewModel(viewModelStoreOwner = activity, key = key)
}

@Composable
fun activityGalleryViewModelOrNull(): GalleryViewModel? {
  val key = LocalGalleryViewModelKey.current ?: return null
  return activityGalleryViewModel(key)
}

@Composable
fun activityGalleryViewModel(): GalleryViewModel {
  val key = checkNotNull(LocalGalleryViewModelKey.current) {
    "GalleryViewModel is only available on gallery list screens"
  }
  return activityGalleryViewModel(key)
}
