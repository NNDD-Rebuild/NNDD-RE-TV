package com.nnddrebuild.nndd_re_tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommand
import com.nnddrebuild.nndd_re_tv.playback.PlaybackCommandBus
import com.nnddrebuild.nndd_re_tv.server.NnddTvServer
import com.nnddrebuild.nndd_re_tv.server.PairingManager
import com.nnddrebuild.nndd_re_tv.ui.nav.Screen
import com.nnddrebuild.nndd_re_tv.ui.screen.home.HomeScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.library.LibraryPlayerScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.library.LibraryPlayerViewModel
import com.nnddrebuild.nndd_re_tv.ui.screen.library.LibraryScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.library.LibraryViewModel
import com.nnddrebuild.nndd_re_tv.ui.screen.mylist.MyListScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.mylist.MyListViewModel
import com.nnddrebuild.nndd_re_tv.ui.screen.pairing.PairingScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.player.PlayerScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.player.PlayerViewModel
import com.nnddrebuild.nndd_re_tv.ui.screen.ranking.RankingScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.ranking.RankingViewModel
import com.nnddrebuild.nndd_re_tv.ui.screen.search.SearchScreen
import com.nnddrebuild.nndd_re_tv.ui.screen.settings.SettingsScreen
import com.nnddrebuild.nndd_re_tv.util.LanAddressResolver
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val pairingManager = PairingManager()
    private lateinit var server: NnddTvServer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as NnddTvApplication
        server = NnddTvServer(
            applicationContext,
            pairingManager,
            app.authManager,
            app.searchClient,
            app.myListClient,
            app.rankingClient,
            app.libraryConnectionStore,
            app.libraryClient
        )
        server.start()

        val lanIp = LanAddressResolver.getLanIpAddress(applicationContext) ?: "0.0.0.0"

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val pairedCount by pairingManager.pairedCount.collectAsState()
                    var pairingUrl by remember {
                        mutableStateOf(
                            "http://$lanIp:${server.getPort()}/pair?token=${pairingManager.currentPairingToken()}"
                        )
                    }
                    // QR画面を表示し続けている間にトークンが期限切れになっても再表示不要で使えるよう、定期的に最新化する
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(30_000)
                            val latestUrl =
                                "http://$lanIp:${server.getPort()}/pair?token=${pairingManager.currentPairingToken()}"
                            if (latestUrl != pairingUrl) {
                                pairingUrl = latestUrl
                            }
                        }
                    }
                    NnddNavHost(
                        navController = navController,
                        pairingUrl = pairingUrl,
                        onRegeneratePairingQr = {
                            pairingManager.regenerateToken()
                            pairingUrl =
                                "http://$lanIp:${server.getPort()}/pair?token=${pairingManager.currentPairingToken()}"
                        },
                        pairedCount = pairedCount,
                        app = app
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }
}

@androidx.compose.runtime.Composable
private fun NnddNavHost(
    navController: NavHostController,
    pairingUrl: String,
    onRegeneratePairingQr: () -> Unit,
    pairedCount: Int,
    app: NnddTvApplication
) {
    val isPaired = pairedCount > 0

    LaunchedEffect(Unit) {
        PlaybackCommandBus.commands.collect { command ->
            when (command) {
                is PlaybackCommand.Play -> navController.navigate(Screen.Player.createRoute(command.videoId))
                is PlaybackCommand.PlayLibraryVideo ->
                    navController.navigate(Screen.LibraryPlayer.createRoute(command.videoId))
                else -> Unit
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Pairing.route) {
        composable(Screen.Pairing.route) {
            PairingScreen(pairingUrl = pairingUrl, isPaired = isPaired)
            LaunchedEffect(isPaired) {
                if (isPaired) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Pairing.route) { inclusive = true }
                    }
                }
            }
        }
        composable(Screen.Home.route) {
            HomeScreen(
                pairingUrl = pairingUrl,
                pairedCount = pairedCount,
                onNavigateRanking = { navController.navigate(Screen.Ranking.route) },
                onNavigateSearch = { navController.navigate(Screen.Search.route) },
                onNavigateMyList = { navController.navigate(Screen.MyList.route) },
                onNavigateLibrary = { navController.navigate(Screen.Library.route) },
                onNavigateSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                authManager = app.authManager,
                onShowPairingQr = {
                    onRegeneratePairingQr()
                    navController.navigate(Screen.QrDisplay.route)
                }
            )
        }
        composable(Screen.QrDisplay.route) {
            PairingScreen(pairingUrl = pairingUrl, isPaired = isPaired)
        }
        composable(Screen.Ranking.route) {
            val viewModel: RankingViewModel = viewModel(
                factory = RankingViewModel.Factory(app.rankingClient)
            )
            RankingScreen(
                viewModel = viewModel,
                onVideoSelected = { navController.navigate(Screen.Player.createRoute(it)) }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onVideoSelected = { navController.navigate(Screen.Player.createRoute(it)) }
            )
        }
        composable(Screen.MyList.route) {
            val viewModel: MyListViewModel = viewModel(
                factory = MyListViewModel.Factory(app.myListClient)
            )
            MyListScreen(
                viewModel = viewModel,
                onVideoSelected = { navController.navigate(Screen.Player.createRoute(it)) }
            )
        }
        composable(
            Screen.Player.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
            val viewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.Factory(
                    app.watchInfoHandler,
                    app.watchSession,
                    app.commentClient,
                    app.nicoCookieJar
                )
            )
            PlayerScreen(
                viewModel = viewModel,
                videoId = videoId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(app.libraryClient, app.libraryConnectionStore)
            )
            LibraryScreen(
                viewModel = viewModel,
                onVideoSelected = { navController.navigate(Screen.LibraryPlayer.createRoute(it)) }
            )
        }
        composable(
            Screen.LibraryPlayer.route,
            arguments = listOf(navArgument("videoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: return@composable
            val viewModel: LibraryPlayerViewModel = viewModel(
                factory = LibraryPlayerViewModel.Factory(app.libraryClient)
            )
            LibraryPlayerScreen(
                viewModel = viewModel,
                videoId = videoId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
