package com.nnddrebuild.nndd_re_tv.ui.nav

sealed class Screen(val route: String) {
    data object Pairing : Screen("pairing")
    data object Home : Screen("home")
    data object Ranking : Screen("ranking")
    data object Search : Screen("search")
    data object MyList : Screen("mylist")
    data object Settings : Screen("settings")
    /** ペアリング済み状態でQRのみ再表示する画面(自動遷移なし)。 */
    data object QrDisplay : Screen("qr_display")
    data object Player : Screen("player/{videoId}") {
        fun createRoute(videoId: String) = "player/$videoId"
    }
    data object Library : Screen("library")
    data object LibraryPlayer : Screen("library_player/{videoId}") {
        fun createRoute(videoId: String) = "library_player/$videoId"
    }
}
