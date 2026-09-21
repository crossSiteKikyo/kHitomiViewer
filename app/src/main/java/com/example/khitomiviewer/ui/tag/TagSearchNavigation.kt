package com.example.khitomiviewer.ui.tag

import androidx.navigation.NavHostController
import com.example.khitomiviewer.Screen

fun navigateToTagSearch(navController: NavHostController, tagId: Long) {
  val entry = navController.currentBackStackEntry
  val isRank = entry?.destination?.route?.startsWith("RankScreen") == true
  if (isRank) {
    val period = entry?.arguments?.getString("period") ?: "week"
    navController.navigate(Screen.Rank.createRoute(1L, period, longArrayOf(tagId)))
  } else {
    navController.navigate(Screen.List.createRoute(1L, longArrayOf(tagId)))
  }
}
