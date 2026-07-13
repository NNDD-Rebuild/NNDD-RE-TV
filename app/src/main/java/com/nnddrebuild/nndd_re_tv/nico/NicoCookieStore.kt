package com.nnddrebuild.nndd_re_tv.nico

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.nicoCookieDataStore by preferencesDataStore(name = "nico_cookies")

@Serializable
data class SerializableCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
)

/**
 * ニコニコログインCookieの永続化。移植元: NNDD-RE(PC版) src/main/nicovideo/auth/CookieStore.ts
 * PC版はJSONファイルにシリアライズして保存する方式。TV版はDataStoreに同等のJSON文字列を保存する。
 */
class NicoCookieStore(private val context: Context) {

    private object Keys {
        val COOKIES_JSON = stringPreferencesKey("cookies_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<SerializableCookie> {
        val raw = context.nicoCookieDataStore.data.first()[Keys.COOKIES_JSON] ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun save(cookies: List<SerializableCookie>) {
        context.nicoCookieDataStore.edit {
            it[Keys.COOKIES_JSON] = json.encodeToString(ListSerializer(SerializableCookie.serializer()), cookies)
        }
    }

    suspend fun clear() {
        context.nicoCookieDataStore.edit { it.clear() }
    }
}
