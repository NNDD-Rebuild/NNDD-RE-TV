package com.nnddrebuild.nndd_re_tv

import android.app.Application
import com.nnddrebuild.nndd_re_tv.library.LibraryClient
import com.nnddrebuild.nndd_re_tv.library.LibraryConnectionStore
import com.nnddrebuild.nndd_re_tv.library.LibraryHttpClient
import com.nnddrebuild.nndd_re_tv.nico.NicoCookieJar
import com.nnddrebuild.nndd_re_tv.nico.NicoCookieStore
import com.nnddrebuild.nndd_re_tv.nico.NicoHttpClient
import com.nnddrebuild.nndd_re_tv.nico.auth.AuthManager
import com.nnddrebuild.nndd_re_tv.nico.comment.CommentClient
import com.nnddrebuild.nndd_re_tv.nico.mylist.MyListClient
import com.nnddrebuild.nndd_re_tv.nico.ranking.RankingClient
import com.nnddrebuild.nndd_re_tv.nico.search.SearchClient
import com.nnddrebuild.nndd_re_tv.nico.watch.WatchInfoHandler
import com.nnddrebuild.nndd_re_tv.nico.watch.WatchSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NnddTvApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var nicoCookieJar: NicoCookieJar
        private set
    lateinit var nicoHttpClient: NicoHttpClient
        private set
    lateinit var rankingClient: RankingClient
        private set
    lateinit var authManager: AuthManager
        private set
    lateinit var searchClient: SearchClient
        private set
    lateinit var myListClient: MyListClient
        private set
    lateinit var watchInfoHandler: WatchInfoHandler
        private set
    lateinit var watchSession: WatchSession
        private set
    lateinit var commentClient: CommentClient
        private set
    lateinit var libraryConnectionStore: LibraryConnectionStore
        private set
    lateinit var libraryClient: LibraryClient
        private set

    override fun onCreate() {
        super.onCreate()
        nicoCookieJar = NicoCookieJar()
        nicoHttpClient = NicoHttpClient(nicoCookieJar)
        rankingClient = RankingClient(nicoHttpClient)
        authManager = AuthManager(nicoHttpClient, nicoCookieJar, NicoCookieStore(this))
        searchClient = SearchClient(nicoHttpClient)
        myListClient = MyListClient(nicoHttpClient)
        watchInfoHandler = WatchInfoHandler(nicoHttpClient, nicoCookieJar)
        watchSession = WatchSession(nicoHttpClient)
        commentClient = CommentClient(nicoHttpClient)
        appScope.launch { authManager.restoreSession() }

        libraryConnectionStore = LibraryConnectionStore(this)
        libraryClient = LibraryClient(LibraryHttpClient())
        appScope.launch {
            libraryConnectionStore.connectionFlow.collect { connection ->
                libraryClient.setConnection(connection)
            }
        }
    }
}
