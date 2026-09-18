package com.example.khitomiviewer.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.khitomiviewer.repository.AppRepositories

class AppViewModelFactory(
  private val application: Application,
  private val repos: AppRepositories = AppRepositories.get(application)
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    val viewModel: ViewModel = when {
      modelClass.isAssignableFrom(AppViewModel::class.java) ->
        AppViewModel(
          application,
          repos.tag,
          repos.gallery,
          repos.github,
          repos.prefs
        )

      modelClass.isAssignableFrom(GalleryViewModel::class.java) ->
        GalleryViewModel(repos.gallery, repos.hitomi, repos.prefs)

      modelClass.isAssignableFrom(HitomiViewModel::class.java) ->
        HitomiViewModel(repos.gallery, repos.hitomi, repos.prefs)

      modelClass.isAssignableFrom(ViewMangaViewModel::class.java) ->
        ViewMangaViewModel(repos.gallery, repos.hitomi, repos.prefs)

      modelClass.isAssignableFrom(TagViewModel::class.java) ->
        TagViewModel(repos.tag)

      modelClass.isAssignableFrom(DialogViewModel::class.java) ->
        DialogViewModel(repos.gallery, repos.tag)

      modelClass.isAssignableFrom(SearchViewModel::class.java) ->
        SearchViewModel(repos.tag)

      modelClass.isAssignableFrom(DataExportImportViewModel::class.java) ->
        DataExportImportViewModel(repos.backup)

      else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
    return viewModel as T
  }
}
