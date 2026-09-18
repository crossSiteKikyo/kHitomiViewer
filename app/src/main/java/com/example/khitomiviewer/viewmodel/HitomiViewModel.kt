package com.example.khitomiviewer.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.repository.HitomiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HitomiViewModel(
  private val hitomiRepository: HitomiRepository
) : ViewModel() {

  val b = mutableStateOf<String?>("1772697601/")
  val thumbChar1 = mutableStateOf<String>("a")
  val thumbChar2 = mutableStateOf<String>("b")
  val o1 = mutableStateOf<String?>("0")
  val o2 = mutableStateOf<String?>("1")
  val mList = mutableStateListOf<String>()

  init {
    viewModelScope.launch(Dispatchers.IO) {
      while (true) {
        try {
          val info = hitomiRepository.fetchGgjsInfo()
          b.value = info.b
          o1.value = info.o1
          thumbChar1.value = info.thumbChar1
          o2.value = info.o2
          thumbChar2.value = info.thumbChar2
          mList.clear()
          mList.addAll(info.mList)
        } catch (e: Exception) {
          Log.i("ggjs 얻기 오류", "${e.message}")
        }
        delay(1000 * 60 * 2)
      }
    }
  }
}
