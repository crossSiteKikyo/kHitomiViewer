package com.example.khitomiviewer.viewmodel

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.khitomiviewer.json.AllData
import com.example.khitomiviewer.room.DatabaseProvider
import com.example.khitomiviewer.room.MyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.IOException
import kotlin.system.exitProcess

class FirstScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getDatabase(application)
    private val repository = MyRepository(db)

    private val prefs = application.getSharedPreferences("MySettings", MODE_PRIVATE)

    // db 내보내기 불러오기에 사용되는 변수
    val dbExportImportStr = mutableStateOf("db 를 불러와야 합니다.")

    fun importFromJsonFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            dbExportImportStr.value = "불러오기중"
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val allData = Json.decodeFromStream<AllData>(inputStream)
                    // db 모든 정보 삭제후 넣기
                    repository.restoreDatabase(allData)
                    
                    // db 정보 받아왔다고 표시. commit말고 apply를 쓰고 앱을 바로 종료시키면 실패한다.
                    prefs.edit().putBoolean("hasLaunched", true).commit()

                    delay(500)
                    // 앱을 종료시킨다.
                    exitProcess(0)
                } ?: throw IOException("InputStream이 null입니다")
            } catch (e: Exception) {
                dbExportImportStr.value = "불러오기 실패: ${e.message}"
            }
        }
    }
}