package com.example.khitomiviewer.viewmodel

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel

object TagViewModelKeys {
  const val MY_TAG = "myTag"
  const val DISLIKE_TAG = "dislikeTag"

  fun fromRoute(route: String?): String? {
    if (route == null) return null
    return when {
      route.startsWith("MyTagScreen") -> MY_TAG
      route.startsWith("DislikeTagScreen") -> DISLIKE_TAG
      else -> null
    }
  }
}

val LocalTagViewModelKey = staticCompositionLocalOf<String?> { null }

@Composable
fun ProvideTagViewModelKey(key: String, content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalTagViewModelKey provides key, content = content)
}

@Composable
fun activityTagViewModel(key: String): TagViewModel {
  val activity = LocalActivity.current as ComponentActivity
  return viewModel(
    viewModelStoreOwner = activity,
    key = key,
    factory = activity.defaultViewModelProviderFactory
  )
}

@Composable
fun activityTagViewModelOrNull(): TagViewModel? {
  val key = LocalTagViewModelKey.current ?: return null
  return activityTagViewModel(key)
}

@Composable
fun activityTagViewModel(): TagViewModel {
  val key = checkNotNull(LocalTagViewModelKey.current) {
    "TagViewModel is only available on tag list screens"
  }
  return activityTagViewModel(key)
}
