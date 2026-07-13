package com.nnddrebuild.nndd_re_tv.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.nnddrebuild.nndd_re_tv.nico.auth.AuthManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(authManager: AuthManager, onShowPairingQr: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(authManager.isLoggedIn()) }
    var nickname by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(loggedIn) {
        nickname = if (loggedIn) authManager.fetchNickname() else null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = if (loggedIn) "${nickname ?: "…"}でログイン中" else "未ログイン",
            modifier = Modifier.fillMaxWidth(0.8f),
            textAlign = TextAlign.Center
        )

        Button(onClick = {
            scope.launch {
                authManager.logout()
                loggedIn = authManager.isLoggedIn()
            }
        }) {
            Text("ログアウト")
        }

        Button(onClick = onShowPairingQr) {
            Text("ペアリングQRを再表示")
        }
    }
}
