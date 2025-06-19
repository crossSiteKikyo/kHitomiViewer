package com.example.khitomiviewer.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.khitomiviewer.room.entity.Tag
import com.example.khitomiviewer.viewmodel.KHitomiViewerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingScreen(navController: NavHostController, mainViewModel: KHitomiViewerViewModel, isDark: MutableState<Boolean>) {
    val context = LocalContext.current
    val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
    var tagLikeCheck = mainViewModel.tagLikeCheck
    var galleryLikeCheck = mainViewModel.galleryLikeCheck

    var tagSearchKeyword = remember { mutableStateOf("") }
    var titleSearchKeyword by remember { mutableStateOf("") }
    var isTagSearchFocused by remember { mutableStateOf(false) }

    var selectedTagList = remember { mutableStateListOf<Tag>() }

    // 태그 색깔
    val titleColorLong = 0xFFCC9999
    val typeBgColor = Color(titleColorLong + 0x333333)
    val textColor = Color(titleColorLong - 0x666666)

    // 타입 선택 관련 변수들
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary
    val disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val typeColor: Map<Int, Long> = remember { mapOf<Int, Long>(
        1 to 0xFFCC9999,  // c99 "doujinshi"
        2 to 0xFFCC99CC,  // c9c "manga"
        3 to 0xFF99CCCC,  // 9cc "artistcg"
        4 to 0xFF9999CC,  // 99c "gamecg"
        5 to 0xFF999999,  // 999 "imageset"
        // CC보다 크면 안되고 66보다 작으면 안된다.
    ) }
    var flagDoujinshi by remember { mutableStateOf(1 in mainViewModel.showTypeIdList) }
    var flagManga by remember { mutableStateOf(2 in mainViewModel.showTypeIdList) }
    var flagArtistcg by remember { mutableStateOf(3 in mainViewModel.showTypeIdList) }
    var flagGamecg by remember { mutableStateOf(4 in mainViewModel.showTypeIdList) }
    var flagImageset by remember { mutableStateOf(5 in mainViewModel.showTypeIdList) }


    LaunchedEffect(tagSearchKeyword.value) {
        snapshotFlow { tagSearchKeyword.value }
            .debounce(200)
            .collectLatest { keyword ->
                mainViewModel.searchTagsFromDb(keyword, selectedTagList)
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
                // 태그검색으로 선택한 태그들을 보여준다.
                if(selectedTagList.isNotEmpty()) {
                    val artists = selectedTagList.filter { tag -> tag.name.startsWith("artist:") }
                    val groups = selectedTagList.filter { tag -> tag.name.startsWith("group:") }
                    val parodies = selectedTagList.filter { tag -> tag.name.startsWith("parody:") }
                    val characters = selectedTagList.filter { tag -> tag.name.startsWith("character:") }
                    val males = selectedTagList.filter { tag -> tag.name.startsWith("male:") }
                    val females = selectedTagList.filter { tag -> tag.name.startsWith("female:") }
                    val others = selectedTagList.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                            && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}

                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(3.dp)
                    ) {
                        if(artists.isNotEmpty()) {
                            for (i in artists.indices) {
                                MainTagRow(artists[i], textColor, typeBgColor, selectedTagList)
                            }
                        }
                        if(groups.isNotEmpty()) {
                            for (i in groups.indices) {
                                MainTagRow(groups[i], textColor, typeBgColor, selectedTagList)
                            }
                        }
                        if(parodies.isNotEmpty()) {
                            for (i in parodies.indices) {
                                MainTagRow(parodies[i], textColor, typeBgColor, selectedTagList)
                            }
                        }
                        if(characters.isNotEmpty()) {
                            for (i in characters.indices) {
                                MainTagRow(characters[i], textColor, typeBgColor, selectedTagList)
                            }
                        }
                        if(males.isNotEmpty()) {
                            for (tag in males) {
                                TagTextRow(tag, selectedTagList)
                            }
                        }
                        if(females.isNotEmpty()) {
                            for (tag in females) {
                                TagTextRow(tag, selectedTagList)
                            }
                        }
                        if(others.isNotEmpty()) {
                            for (tag in others) {
                                TagTextRow(tag, selectedTagList)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tagSearchKeyword.value,
                    onValueChange = {
                        tagSearchKeyword.value = it
                    },
                    label = {Text("태그검색")},
                    maxLines = 1,
//                    colors = OutlinedTextFieldDefaults.colors(mainTextColor, mainTextColor),
                    modifier = Modifier.fillMaxWidth().onFocusChanged{isTagSearchFocused = it.isFocused}
                )

                // 태그검색에 입력된 키워드가 포함되어있는 태그 10개를 보여준다.
                if(isTagSearchFocused) {
                    val artists = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("artist:") }
                    val groups = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("group:") }
                    val parodies = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("parody:") }
                    val characters = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("character:") }
                    val males = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("male:") }
                    val females = mainViewModel.tagSearchList.filter { tag -> tag.name.startsWith("female:") }
                    val others = mainViewModel.tagSearchList.filter { tag -> (!tag.name.startsWith("artist:") && !tag.name.startsWith("group:") && !tag.name.startsWith("parody:")
                            && !tag.name.startsWith("character:") && !tag.name.startsWith("male:") && !tag.name.startsWith("female:"))}

                    if(artists.isNotEmpty()) {
                        for (i in artists.indices) {
                            MainTagText3(artists[i], textColor, typeBgColor, selectedTagList, tagSearchKeyword)
                        }
                    }
                    if(groups.isNotEmpty()) {
                        for (i in groups.indices) {
                            MainTagText3(groups[i], textColor, typeBgColor, selectedTagList, tagSearchKeyword)
                        }
                    }
                    if(parodies.isNotEmpty()) {
                        for (i in parodies.indices) {
                            MainTagText3(parodies[i], textColor, typeBgColor, selectedTagList, tagSearchKeyword)
                        }
                    }
                    if(characters.isNotEmpty()) {
                        for (i in characters.indices) {
                            MainTagText3(characters[i], textColor, typeBgColor, selectedTagList, tagSearchKeyword)
                        }
                    }
                    if(females.isNotEmpty()) {
                        for (tag in females) {
                            TagText3(tag, selectedTagList, tagSearchKeyword)
                        }
                    }
                    if(males.isNotEmpty()) {
                        for (tag in males) {
                            TagText3(tag, selectedTagList, tagSearchKeyword)
                        }
                    }
                    if(others.isNotEmpty()) {
                        for (tag in others) {
                            TagText3(tag, selectedTagList, tagSearchKeyword)
                        }
                    }
                }
                Button(
                    onClick = {
                        val tagIdListJson = Json.encodeToString(selectedTagList.map { tag -> tag.tagId })
                        navController.navigate("ListScreen/1/$tagIdListJson/${titleSearchKeyword}")
                    }
                ) {
                    Text("종합 검색")
                }
                HorizontalDivider(thickness = 2.dp)

                Text("UI 세팅", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                // 보일 타입 선택
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            flagDoujinshi = !flagDoujinshi
                            mainViewModel.typeOnOff(1, flagDoujinshi)
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flagDoujinshi) Color(0xFFCC9999 + 0x333333) else disabledContainerColor,
                            contentColor = if (flagDoujinshi) Color(0xFFCC9999 - 0x666666) else disabledContentColor
                        ),
                        modifier = Modifier.fillMaxWidth(0.33f)
                    ) { Text("doujinshi") }
                    Button(
                        onClick = {
                            flagManga = !flagManga
                            mainViewModel.typeOnOff(2, flagManga)
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flagManga) Color(0xFFCC99CC + 0x333333) else disabledContainerColor,
                            contentColor = if (flagManga) Color(0xFFCC99CC - 0x666666) else disabledContentColor
                        ),
                        modifier = Modifier.fillMaxWidth(0.33f)
                    ) { Text("manga") }
                    Button(
                        onClick = {
                            flagArtistcg = !flagArtistcg
                            mainViewModel.typeOnOff(3, flagArtistcg)
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flagArtistcg) Color(0xFF99CCCC + 0x333333) else disabledContainerColor,
                            contentColor = if (flagArtistcg) Color(0xFF99CCCC - 0x666666) else disabledContentColor
                        ),
                        modifier = Modifier.fillMaxWidth(0.33f)
                    ) { Text("artistcg") }
                    Button(
                        onClick = {
                            flagGamecg = !flagGamecg
                            mainViewModel.typeOnOff(4, flagGamecg)
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flagGamecg) Color(0xFF9999CC + 0x333333) else disabledContainerColor,
                            contentColor = if (flagGamecg) Color(0xFF9999CC - 0x666666) else disabledContentColor
                        ),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) { Text("gamecg") }
                    Button(
                        onClick = {
                            flagImageset = !flagImageset
                            mainViewModel.typeOnOff(5, flagImageset)
                                  },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flagImageset) Color(0xFF999999 + 0x333333) else disabledContainerColor,
                            contentColor = if (flagImageset) Color(0xFF999999 - 0x666666) else disabledContentColor
                        ),
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) { Text("imageset") }
                }
                Text("한번에 로드할 갤러리 수: ${mainViewModel.pageSize}")
                Slider(
                    value = mainViewModel.pageSize.toFloat(),
                    valueRange = 5f..50f,
                    onValueChange = { v->
                        mainViewModel.settingPageSize(v.toInt())
                    },
                    thumb = {
                        Box(modifier = Modifier.padding(0.dp).size(25.dp).background(MaterialTheme.colorScheme.primary,
                            CircleShape
                        ))
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(sliderState = sliderState, thumbTrackGapSize = 0.dp, modifier = Modifier.height(10.dp))
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
                Button(
                    onClick = {
                        mainViewModel.toggleDarkMode(isDark)
                    }
                ) {
                    Text("다크모드 토클")
                }
                HorizontalDivider(thickness = 2.dp)

                Text("태그/갤러리 좋아요/싫어요 관리", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
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
                        Text("태그  관리")
                    }
                    // 작품 좋아요 관리 ui – DISLIKE, LIKE 작품 리스트 보여준다.
                    Button(
                        shape = RoundedCornerShape(7.dp),
                        onClick = { navController.navigate("LikeSettingScreen/gallery") }
                    ) {
                        Icon(Icons.Filled.Settings, "setting")
                        Text("갤러리  관리")
                    }
                }
                HorizontalDivider(thickness = 2.dp)

                Text("크롤링 세팅", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Text("크롤링 상태: ${if(mainViewModel.crawlOn.value) "켜짐" else "꺼짐"}")
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
                    thumb = {
                        Box(modifier = Modifier.padding(0.dp).size(25.dp).background(MaterialTheme.colorScheme.primary,
                            CircleShape
                        ))
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(sliderState = sliderState, thumbTrackGapSize = 0.dp, modifier = Modifier.height(10.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                Text("마지막 크롤링 에러")
                Text(mainViewModel.crawlErrorStr.value, color = Color.Gray)
                Text("크롤링 상황")
                Text(mainViewModel.crawlStatusStr.value, color = Color.Gray)
                Text("남은 크롤링 개수")
                Text("${mainViewModel.gIdList.size}", color = Color.Gray)
                HorizontalDivider(thickness = 2.dp)

                // 캐시 설정
                Text("캐시 설정", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Text("한번이라도 본 이미지는 캐시에 저장됩니다", color = Color.Gray)
                Text("앱 용량이 너무 커질시 캐시(임시파일)를 설정에서 삭제하세요", color = Color.Gray)
                Text("데이터베이스 (데이터)는 절대 삭제하시면 안됩니다.", color = Color.Gray)
                HorizontalDivider(thickness = 2.dp)

                // 데이터베이스 내보내기/불러오기
                Text("내보내기/불러오기 설정", fontWeight = FontWeight.Bold, style = TextStyle(shadow = Shadow(Color.Cyan, blurRadius = 5f), fontSize = 21.sp))
                Button(
                    shape = RoundedCornerShape(7.dp),
                    onClick = { navController.navigate("DatabaseExportImportSettingScreen") }
                ) {
                    Icon(Icons.Filled.Settings, "setting")
                    Text("좋아요/싫어요 정보 내보내기/불러오기")
                }
                HorizontalDivider(thickness = 2.dp)
                Text("kHitomiViewer 버전: ${versionName}", color = Color.Gray)
            }
        }
    }
}

@Composable
fun MainTagText3(tag: Tag, textColor:Color, bgColor:Color, selectedTagList: MutableList<Tag>, tagSearchKeyword: MutableState<String>) {
    fun tagClick() {
        selectedTagList.add(tag)
        tagSearchKeyword.value = ""
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
            )
            .background(color = bgColor)
            .clip(RoundedCornerShape(5.dp))
            .padding(horizontal = 2.dp)
    )
}

@Composable
fun MainTagRow(tag: Tag, textColor:Color, bgColor:Color, selectedTagList: MutableList<Tag>) {
    fun removeTagFromList() {
        selectedTagList.remove(tag)
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color = bgColor, RoundedCornerShape(5.dp))
    ) {
        Text(
            name,
            color = color,
            modifier = Modifier.padding(start = 5.dp)
        )
        IconButton (
            onClick = {removeTagFromList()}
        ){
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
    }
}

@Composable
fun TagText3(tag: Tag, selectedTagList: MutableList<Tag>, tagSearchKeyword: MutableState<String>) {
    fun tagClick() {
        selectedTagList.add(tag)
        tagSearchKeyword.value = ""
    }
    var name = when(tag.likeStatus) {
        2 -> tag.name + "♥"
        else -> tag.name
    }
    var bgColor: Color = Color.Gray
    if(name.startsWith("male:")) {
        name = name.replace("male:", "")
        bgColor = Color(0xFF0080FF)
    }
    else if(name.startsWith("female:")) {
        name = name.replace("female:", "")
        bgColor = Color(0xFFFF66B2)
    }

    Text(
        name,
        color = Color.White,
        modifier = Modifier
            .combinedClickable(
                onClick = { tagClick() },
            )
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .padding(horizontal = 2.dp)
    )
}

@Composable
fun TagTextRow(tag: Tag, selectedTagList: MutableList<Tag>) {
    fun removeTagFromList() {
        selectedTagList.remove(tag)
    }
    var name = when(tag.likeStatus) {
        2 -> tag.name + "♥"
        else -> tag.name
    }
    var bgColor: Color = Color.Gray
    if(name.startsWith("male:")) {
        name = name.replace("male:", "")
        bgColor = Color(0xFF0080FF)
    }
    else if(name.startsWith("female:")) {
        name = name.replace("female:", "")
        bgColor = Color(0xFFFF66B2)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color = bgColor, RoundedCornerShape(5.dp))
    ) {
        Text(
            name,
            color = Color.White,
            modifier = Modifier.padding(start = 5.dp)
        )
        IconButton (
            onClick = {removeTagFromList()}
        ){
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
        }
    }
}