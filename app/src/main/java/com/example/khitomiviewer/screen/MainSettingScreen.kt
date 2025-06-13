package com.example.khitomiviewer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce

@Composable
fun MainSettingScreen(navController: NavHostController, mainViewModel: KHitomiViewerViewModel) {
    var tagLikeCheck = mainViewModel.tagLikeCheck
    var galleryLikeCheck = mainViewModel.galleryLikeCheck
    val showDialog = remember { mutableStateOf(false) }

    var tagSearchKeyword by remember { mutableStateOf("") }
    var titleSearchKeyword by remember { mutableStateOf("") }
    var isTagSearchFocused by remember { mutableStateOf(false) }

    LaunchedEffect(tagSearchKeyword) {
        snapshotFlow { tagSearchKeyword }
            .debounce(300)
            .collectLatest { keyword ->
                mainViewModel.searchTagsFromDb(keyword)
            }
    }


    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 다이얼로그
                if(showDialog.value) {
                    val likeStatusIntToStr = listOf("싫어요 \uD83C\uDD96", "기본", "좋아요 ♥")
                    AlertDialog(
                        onDismissRequest = {},
                        confirmButton = {TextButton(onClick = { showDialog.value = false }) {
                            Text("닫기")
                        }},
                        title = {
                            val what = if(mainViewModel.whatStr.value == "tag") "태그" else "갤러리"
                            Text("${what} - ${mainViewModel.selectedTagNameOrGalleryId.value}")
                        },
                        text = {
                            Column (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                for (x in 0..2) {
                                    if(x == mainViewModel.likeStatus.intValue)
                                        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(likeStatusIntToStr[x])}
                                    else
                                        Button(onClick = {
                                            // db에 상태 변경
                                            mainViewModel.changeLike(x, "MainSettingScreen")
                                            // 다이얼로그 닫기
                                            showDialog.value = false
                                        }) { Text(likeStatusIntToStr[x])}
                                }
                            }
                        })
                }
                Text("검색", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                OutlinedTextField(
                    value = titleSearchKeyword,
                    onValueChange = {
                        titleSearchKeyword = it
                    },
                    label = {Text("제목검색")},
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { navController.navigate("ListScreen/1//${titleSearchKeyword}")}
                ) {
                    Text("제목검색하기")
                }
                OutlinedTextField(
                    value = tagSearchKeyword,
                    onValueChange = {
                        tagSearchKeyword = it
                    },
                    label = {Text("태그검색")},
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().onFocusChanged{isTagSearchFocused = it.isFocused}
                )
                if(isTagSearchFocused) {
                    val artists = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("artist:") }
                    val groups = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("group:") }
                    val parodies = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("parody:") }
                    val characters = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("character:") }
                    val males = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("male:") }
                    val females = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("female:") }
                    val others = mainViewModel.tagSearchList.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                            && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}

                    val titleColorLong = 0xFFCC9999
                    val typeBgColor = Color(titleColorLong + 0x333333)
                    val textColor = Color(titleColorLong - 0x666666)

                    if(artists.isNotEmpty()) {
                        for (i in artists.indices) {
                            MainTagText3(artists[i], textColor, typeBgColor, mainViewModel, showDialog, navController)
                        }
                    }
                    if(groups.isNotEmpty()) {
                        for (i in groups.indices) {
                            MainTagText3(groups[i], textColor, typeBgColor, mainViewModel, showDialog, navController)
                        }
                    }
                    if(parodies.isNotEmpty()) {
                        for (i in parodies.indices) {
                            MainTagText3(parodies[i], textColor, typeBgColor, mainViewModel, showDialog, navController)
                        }
                    }
                    if(characters.isNotEmpty()) {
                        for (i in characters.indices) {
                            MainTagText3(characters[i], textColor, typeBgColor, mainViewModel, showDialog, navController)
                        }
                    }
                    if(males.isNotEmpty()) {
                        for (tag in males) {
                            TagText(tag, mainViewModel, showDialog, navController)
                        }
                    }
                    if(females.isNotEmpty()) {
                        for (tag in females) {
                            TagText(tag, mainViewModel, showDialog, navController)
                        }
                    }
                    if(others.isNotEmpty()) {
                        for (tag in others) {
                            TagText(tag, mainViewModel, showDialog, navController)
                        }
                    }
                }
                HorizontalDivider(thickness = 2.dp)

                Text("UI 세팅", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Text("한번에 로드할 갤러리 수: ${mainViewModel.pageSize}")
                Slider(
                    value = mainViewModel.pageSize.toFloat(),
                    valueRange = 5f..50f,
                    onValueChange = { v->
                        mainViewModel.settingPageSize(v.toInt())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                // 태그 NONE을 보일지 말지 선택
                Row (verticalAlignment = Alignment.CenterVertically){
                    Text("좋아요 태그가 있는 갤러리만 보이기")
                    Checkbox(checked = tagLikeCheck.value, onCheckedChange = { mainViewModel.setTagLikeCheck(it) })
                }
                Row (verticalAlignment = Alignment.CenterVertically){
                    Text("좋아요 갤러리만 보이기")
                    Checkbox(checked = galleryLikeCheck.value, onCheckedChange = { mainViewModel.setGalleryLikeCheck(it) })
                }
                Text("싫어요는 보이지 않습니다", color = Color.Gray)
                HorizontalDivider(thickness = 2.dp)

                Text("태그/갤러리 좋아요 세팅", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Row (
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 태그 좋아요 관리 ui – DISLIKE, LIKE, SUBSCRIBE 태그 리스트 보여준다.
                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = { navController.navigate("LikeSettingScreen/tag") }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("태그 좋아요 관리")
                    }
                    // 작품 좋아요 관리 ui – DISLIKE, LIKE 작품 리스트 보여준다.
                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = { navController.navigate("LikeSettingScreen/gallery") }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("갤러리 좋아요 관리")
                    }
                }
                HorizontalDivider(thickness = 2.dp)

                Text("크롤링 세팅", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Text("크롤링 상태: ${mainViewModel.crawlOn.value}")
                Button(onClick = {
                    mainViewModel.toggleCrawlOn()
                }) { Text("크롤링 켜기/끄기") }
                Text("크롤링 간격: ${mainViewModel.delayS.longValue}초")
                Slider(
                    value = mainViewModel.delayS.longValue.toFloat(),
                    valueRange = 40f..120f,
                    onValueChange = { v->
                        mainViewModel.setDelayS(v.toLong())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                Text("마지막 크롤링 에러")
                Text(mainViewModel.crawlErrorStr.value, color = Color.Gray)
                Text("크롤링 상황")
                Text(mainViewModel.crawlStatusStr.value, color = Color.Gray)
                HorizontalDivider(thickness = 2.dp)

                // 캐시 설정
                Text("캐시 설정", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Text("한번이라도 본 이미지는 캐시에 저장됩니다", color = Color.Gray)
                Text("앱 용량이 너무 커질시 캐시(임시파일)를 설정에서 삭제하세요", color = Color.Gray)
                Text("데이터베이스 (데이터)는 절대 삭제하시면 안됩니다.", color = Color.Gray)
                HorizontalDivider(thickness = 2.dp)

                // 데이터베이스 내보내기/불러오기
                Text("데이터베이스 내보내기/불러오기 설정", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Button(
                    shape = RoundedCornerShape(7.dp),
                    onClick = { navController.navigate("DatabaseExportImportSettingScreen") }
                ) {
                    Icon(Icons.Filled.Settings, "setting")
                    Text("데이터베이스 내보내기/불러오기")
                }
                HorizontalDivider(thickness = 2.dp)
            }
        }
    }
}

@Composable
fun MainTagText3(tag: Tag, textColor:Color, bgColor:Color, mainViewModel: KHitomiViewerViewModel, showDialog: MutableState<Boolean>, navController: NavHostController) {
    fun tagClick() {
        navController.navigate("ListScreen/1/${tag.tagId}/")
    }
    var name = when(tag.likeStatus) {
        2 -> tag.name + "♥"
        else -> tag.name
    }
    var color: Color = textColor
    if(name.startsWith("parody:")) {
        name = name.replace("parody:", "시리즈:")
    }
    else if(name.startsWith("character:")) {
        name = name.replace("character:", "캐릭터:")
    }
    else if(name.startsWith("group:")) {
        name = name.replace("group:", "그룹:")
    }
    else if(name.startsWith("artist:")) {
        name = name.replace("artist:", "작가:")
    }

    Text(
        name,
        color = color,
        modifier = Modifier
            .combinedClickable(
                onClick = { tagClick() },
                onLongClick = {
                    mainViewModel.setDialog("tag", tag.name)
                    showDialog.value = true
                }
            )
            .background(color = bgColor)
            .clip(RoundedCornerShape(5.dp))
            .padding(horizontal = 2.dp)
    )
}