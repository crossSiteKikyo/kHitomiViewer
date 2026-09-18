package com.example.khitomiviewer.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.PreferenceManager
import com.example.khitomiviewer.repository.GalleryRepository
import com.example.khitomiviewer.repository.HitomiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HitomiViewModel(
  private val galleryRepository: GalleryRepository,
  private val hitomiRepository: HitomiRepository,
  private val prefManager: PreferenceManager
) : ViewModel() {

  val b = mutableStateOf<String?>("1772697601/")
  val thumbChar1 = mutableStateOf<String>("a")
  val thumbChar2 = mutableStateOf<String>("b")
  val o1 = mutableStateOf<String?>("0")
  val o2 = mutableStateOf<String?>("1")
  val mList = mutableStateListOf<String>()

  val serverGidList = mutableListOf<Int>()
  val crawlAmount = 40
  val filteredIds = mutableStateListOf<Int>()

  val crawlErrorStr = mutableStateOf("아직까지 에러가 없습니다")
  val crawlStatusStr = mutableStateOf("...")

  val dbAllGId = mutableListOf<Long>()

  init {
    viewModelScope.launch(Dispatchers.IO) {
      hitomiRepository.initType()
      hitomiRepository.initTag()
      getGgjs()
      getGIdsAndFilterGids()

      crawlNewGalleries()
      crawlMissedGalleries()
      deleteDeletedGalleries()
      syncGalleryTag1000()
      syncGalleryTag2000()
      syncGalleryTag3000()
      syncGalleryTag4000()
      syncGalleryTag5000()
      syncGalleryTag6000()
      syncGalleryTag7000()
      syncGalleryTag8000()
    }
  }

  fun getGgjs() = viewModelScope.launch(Dispatchers.IO) {
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
        crawlErrorStr.value = "${getCurrentFormattedTime()}: ggjs 얻기 오류: ${e.message}"
      }
      delay(1000 * 60 * 2)
    }
  }

  suspend fun getGIdsAndFilterGids() = withContext(Dispatchers.IO) {
    var isSuccess = false

    while (!isSuccess) {
      try {
        serverGidList.addAll(hitomiRepository.fetchKoreanGids())
        Log.i("서버에 있는 갤러리 총 수", "${serverGidList.size}")
        val targetGid = galleryRepository.findMaxGid().toInt()
        val searchIndex = serverGidList.binarySearch(targetGid, reverseOrder())
        val limitIndex = if (searchIndex >= 0) {
          searchIndex
        } else {
          -(searchIndex + 1)
        }
        filteredIds.addAll(serverGidList.subList(0, limitIndex))
        filteredIds.reverse()
        isSuccess = true
      } catch (e: Exception) {
        Log.i("갤러리 gId 리스트 얻기 오류", "${e.message}")
        crawlErrorStr.value =
          "${getCurrentFormattedTime()}: 갤러리 gId 리스트 얻기 오류: ${e.message}"
        delay(5000)
      }
    }
  }

  suspend fun crawlNewGalleries() {
    while (true) {
      if (filteredIds.isNotEmpty()) {
        crawlList()
        delay(20)
      }
      if (filteredIds.isEmpty()) break
    }
  }

  suspend fun crawlList() = withContext(Dispatchers.IO) {
    crawlStatusStr.value = "${getCurrentFormattedTime()}: 크롤링 중"
    val gIds = filteredIds.take(crawlAmount)

    val deferredList = gIds.map { gId ->
      async {
        try {
          hitomiRepository.fetchGalleryFullInfo(gId)
        } catch (e: Exception) {
          Log.i("각종 정보 얻기 오류", "${e.message}")
          crawlErrorStr.value =
            "${getCurrentFormattedTime()}: crawlErrorStr: ${e.message}"
          null
        }
      }
    }
    val galleryFullInfos = deferredList.awaitAll()
    val successfulInfos = galleryFullInfos.filterNotNull()
    val successfulGIds = successfulInfos.map { it.galleryInfo.id.toInt() }
    for (gFI in successfulInfos) {
      try {
        hitomiRepository.insertGalleryInfo(gFI.galleryInfo, gFI.thumb1, gFI.thumb2)
      } catch (e: Exception) {
        Log.i("썸네일 크롤링 오류", "${e.message}")
        crawlErrorStr.value = "${getCurrentFormattedTime()}: 썸네일 크롤링 오류: ${e.message}"
      }
    }
    filteredIds.removeAll(successfulGIds)
    crawlStatusStr.value = "${getCurrentFormattedTime()}: 크롤링 끝"
    if (filteredIds.isEmpty()) {
      crawlStatusStr.value = "${getCurrentFormattedTime()}: 모든 크롤링을 마쳤습니다"
    }
  }

  suspend fun crawlMissedGalleries() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastCrawlMissedGalleries.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24) {
      crawlStatusStr.value = "미처 크롤링하지 못한 갤러리 크롤링"
      filteredIds.clear()
      filteredIds.addAll(serverGidList)
      filteredIds.removeIf { galleryRepository.existsById(it.toLong()) }
      crawlNewGalleries()
      prefManager.updateLastCrawlMissedGalleries(now)
      crawlStatusStr.value = "미처 크롤링하지 못한 갤러리 크롤링 완료"
    }
  }

  suspend fun deleteDeletedGalleries() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastDeleteDeletedGallery.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 2) {
      crawlStatusStr.value = "hitomi에서 삭제된 갤러리 db에서 삭제중"
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      val idsToDelete = dbAllGId.toSet() - serverGidList.map { it.toLong() }.toSet()
      idsToDelete.map { gId ->
        hitomiRepository.removeGalleryInfo(gId)
      }
      prefManager.updateLastDeleteDeletedGallery(now)
      crawlStatusStr.value = "삭제된 갤러리 db에서 삭제 완료"
    }
  }

  suspend fun syncGalleryTag1000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag1000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 0..999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag1000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 1000까지)"
    }
  }

  suspend fun syncGalleryTag2000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag2000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 2) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 1000..1999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag2000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 2000까지)"
    }
  }

  suspend fun syncGalleryTag3000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag3000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 4) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 2000..2999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag3000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 3000까지)"
    }
  }

  suspend fun syncGalleryTag4000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag4000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 4) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 3000..3999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag4000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 4000까지)"
    }
  }

  suspend fun syncGalleryTag5000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag5000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 4000..4999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag5000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 5000까지)"
    }
  }

  suspend fun syncGalleryTag6000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag6000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 5000..5999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag6000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 6000까지)"
    }
  }

  suspend fun syncGalleryTag7000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag7000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 6000..6999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag7000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 7000까지)"
    }
  }

  suspend fun syncGalleryTag8000() = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val lastDaily = prefManager.lastSyncGalleryTag8000.first()
    if (now - lastDaily > 1000L * 60 * 60 * 24 * 8) {
      if (dbAllGId.isEmpty())
        dbAllGId.addAll(galleryRepository.findAllGId())
      for (i: Int in 7000..7999 step (crawlAmount)) {
        crawlStatusStr.value = "갤러리_태그 업데이트 동기화 - offset $i"
        syncGalleryTag(dbAllGId.subList(i, i + crawlAmount))
      }
      prefManager.updateLastSyncGalleryTag8000(now)
      crawlStatusStr.value = "갤러리_태그 업데이트 동기화 완료(offset 8000까지)"
    }
  }

  suspend fun syncGalleryTag(gIdList: List<Long>) = withContext(Dispatchers.IO) {
    val deferredList = gIdList.map { gId ->
      async {
        try {
          hitomiRepository.syncGalleryTagsFromRemote(gId)
        } catch (e: Exception) {
          Log.i("갤러리태그 동기화 $gId 중 오류", "${e.message}")
        }
      }
    }
    deferredList.awaitAll()
  }

  fun getCurrentFormattedTime(): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yy년 MM월 dd일 HH시 mm분 ss초")
    return current.format(formatter)
  }
}
