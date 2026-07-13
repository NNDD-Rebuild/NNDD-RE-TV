package com.nnddrebuild.nndd_re_tv.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryDataStore by preferencesDataStore(name = "library_connection")

/** PC版NNDD-REライブラリ共有サーバーの接続先(host:port)を永続化する。 */
class LibraryConnectionStore(private val context: Context) {

    private object Keys {
        val HOST = stringPreferencesKey("host")
        val PORT = intPreferencesKey("port")
    }

    val connectionFlow: Flow<LibraryConnection?> = context.libraryDataStore.data.map { prefs ->
        val host = prefs[Keys.HOST]
        val port = prefs[Keys.PORT]
        if (host.isNullOrBlank() || port == null) null else LibraryConnection(host, port)
    }

    suspend fun save(host: String, port: Int) {
        context.libraryDataStore.edit {
            it[Keys.HOST] = host
            it[Keys.PORT] = port
        }
    }

    suspend fun clear() {
        context.libraryDataStore.edit { it.clear() }
    }
}
