package com.example.khitomiviewer.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen
import com.example.khitomiviewer.ui.common.CustomTextField
import com.example.khitomiviewer.ui.tag.FilteredTag
import com.example.khitomiviewer.ui.tag.SearchedTag
import com.example.khitomiviewer.ui.tag.SelectedTag
import com.example.khitomiviewer.viewmodel.DialogViewModel
import com.example.khitomiviewer.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Search(
  navController: NavHostController,
  isSearchSheetVisible: MutableState<Boolean>,
  titleKeyword: String?,
  onTitleTagSearch: ((tagIdList: LongArray, titleKeyword: String) -> Unit)? = null,
  onGIdSearch: ((gId: Long) -> Unit)? = null
) {
  val activity = LocalActivity.current as ComponentActivity
  val searchViewModel: SearchViewModel = viewModel(activity)

  var gIdSearch by remember { mutableStateOf("") }
  var titleSearchKeyword by remember { mutableStateOf("") }
//  var tagSearchKeyword by remember { mutableStateOf("") }
  var tagSearchKeyword by remember { mutableStateOf(TextFieldValue("")) }
  var isTagSearchFocused by remember { mutableStateOf(false) }

  LaunchedEffect(tagSearchKeyword.text) {
    searchViewModel.filterTagsByKeyword(tagSearchKeyword.text)
  }

  LaunchedEffect(titleKeyword) {
    if (titleKeyword.isNullOrBlank())
      titleSearchKeyword = ""
    else
      titleSearchKeyword = titleKeyword
  }

  AnimatedVisibility(
    visible = isSearchSheetVisible.value,
    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(),
      tonalElevation = 8.dp,
      shadowElevation = 8.dp
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
      ) {
        SelectType()
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            CustomTextField(
              value = titleSearchKeyword,
              onValueChange = {
                titleSearchKeyword = it
              },
              placeholder = { Text("제목검색") },
              maxLines = 1,
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth(),
              contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
            )
            if (searchViewModel.selectedTags.isNotEmpty()) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                  .padding(1.dp)
                  .horizontalScroll(rememberScrollState())
              ) {
                searchViewModel.selectedTags.forEach { tag ->
                  SelectedTag(tag) {
                    searchViewModel.selectedTags.remove(tag)
                  }
                }
              }
            }
          }
          Button(
            onClick = {
              val tagIdList =
                searchViewModel.selectedTags.map { tag -> tag.tagId }.toLongArray()
              // 아무 것도 안치면 검색하지 않는다.
              if (tagIdList.isNotEmpty() || titleSearchKeyword.isNotBlank()) {
                val search = onTitleTagSearch
                if (search != null)
                  search(tagIdList, titleSearchKeyword)
                else
                  navController.navigate(
                    Screen.List.createRoute(
                      1L,
                      tagIdList,
                      titleSearchKeyword
                    )
                  )
              }
            },
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
              .width(40.dp)
              .fillMaxHeight()
              .padding(0.dp)
          ) { Icon(Icons.Filled.Search, "search") }
        }

        CustomTextField(
          value = tagSearchKeyword,
          onValueChange = {
            tagSearchKeyword = it
          },
          placeholder = { Text("태그 찾기 - 최대 15개만 표시합니다") },
          maxLines = 1,
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isTagSearchFocused = it.isFocused },
          contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
        )
        // 포커스되어있다면 추천 접두사를 보여주거나 필터된 태그들을 보여준다.
        if (isTagSearchFocused) {
          if (searchViewModel.filteredTags.isEmpty()) {
            SearchRecommendedList { prefix ->
              tagSearchKeyword = TextFieldValue(text = prefix, selection = TextRange(prefix.length))
            }
          } else {
            searchViewModel.filteredTags.forEach { tag ->
              FilteredTag(tag) {
                searchViewModel.selectedTags.add(tag)
                tagSearchKeyword = TextFieldValue("")
              }
            }
          }
        }

        HorizontalDivider(thickness = 2.dp, modifier = Modifier.padding(vertical = 10.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CustomTextField(
            value = gIdSearch,
            onValueChange = {
              if (it.isEmpty() || it.matches("""^\d+$""".toRegex())) {
                gIdSearch = it
              }
            },
            placeholder = { Text("갤러리 아이디로 검색") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            maxLines = 1,
            singleLine = true,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp)
          )
          Button(
            onClick = {
              val gId = gIdSearch.toLong()
              val search = onGIdSearch
              if (search != null)
                search(gId)
              else
                navController.navigate(Screen.List.createRoute(gId = gId))
            },
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
              .width(40.dp)
              .padding(0.dp)
          ) { Icon(Icons.Filled.Search, "search") }
        }

        Button(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 15.dp),
          onClick = { isSearchSheetVisible.value = false }
        ) {
          Icon(Icons.Filled.ExpandLess, null)
        }
      }
    }
  }
}

@Composable
fun SearchResultBar(
  navController: NavHostController,
  isTagDialogOpen: MutableState<Boolean>,
  isSearchSheetVisible: MutableState<Boolean>,
  titleKeyword: String?,
  gId: Long?
) {
  val activity = LocalActivity.current as ComponentActivity
  val searchViewModel: SearchViewModel = viewModel(activity)
  val dialogViewModel: DialogViewModel = viewModel(activity)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .border(
          width = 1.dp,
          color = Color.Gray,
          shape = RoundedCornerShape(2.dp)
        )
    ) {
      if (gId != null && gId != 0L) {
        Text(
          "id검색 - $gId",
          fontWeight = FontWeight.Bold,
        )
      } else {
        if (!titleKeyword.isNullOrBlank()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Text(
              "제목검색 - $titleKeyword",
              fontWeight = FontWeight.Bold,
            )
          }
        }
        if (searchViewModel.currentSearchTags.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Text(
              "태그검색 - ",
              fontWeight = FontWeight.Bold,
            )
            searchViewModel.currentSearchTags.map { tag ->
              SearchedTag(
                tag,
                dialogViewModel,
                isTagDialogOpen,
                navController
              )
            }
          }
        }
      }
    }
    Button(
      onClick = { isSearchSheetVisible.value = true },
      shape = RoundedCornerShape(4.dp),
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier
        .width(40.dp)
        .fillMaxHeight()
        .padding(0.dp)
    ) { Icon(Icons.Filled.Search, "search") }
  }
}

@Composable
fun SearchSortBar(
  isPopular: Boolean,
  onNewest: () -> Unit,
  onPopular: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(5.dp)
  ) {
    Button(
      onClick = onNewest,
      enabled = isPopular,
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(2.dp)
    ) { Text("최신순") }
    Button(
      onClick = onPopular,
      enabled = !isPopular,
      modifier = Modifier.weight(1f),
      shape = RoundedCornerShape(12.dp),
      contentPadding = PaddingValues(2.dp)
    ) { Text("인기순") }
  }
}