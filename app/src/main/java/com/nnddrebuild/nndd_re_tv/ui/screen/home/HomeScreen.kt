package com.nnddrebuild.nndd_re_tv.ui.screen.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.nnddrebuild.nndd_re_tv.util.QrCodeGenerator

@Composable
fun HomeScreen(
    pairingUrl: String,
    pairedCount: Int,
    onNavigateRanking: () -> Unit,
    onNavigateSearch: () -> Unit,
    onNavigateMyList: () -> Unit,
    onNavigateLibrary: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    // 複数人が入れ違いでペアリングできるよう、トップページに常時QRを表示しておく
    val qrBitmap = remember(pairingUrl) { QrCodeGenerator.generate(pairingUrl, sizePx = 220) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "ペアリング用QRコード")
        Text(text = pairingUrl, fontSize = 12.sp)
        Text(text = "ペアリング中: ${pairedCount}台", fontSize = 14.sp)

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = onNavigateRanking) { Text("ランキング") }
            Button(onClick = onNavigateSearch) { Text("検索結果") }
            Button(onClick = onNavigateMyList) { Text("マイリスト") }
            Button(onClick = onNavigateLibrary) { Text("NNDD-REライブラリ") }
            Button(onClick = onNavigateSettings) { Text("設定") }
        }
    }
}
