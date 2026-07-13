package com.nnddrebuild.nndd_re_tv.ui.screen.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnddrebuild.nndd_re_tv.util.QrCodeGenerator

@Composable
fun PairingScreen(pairingUrl: String, isPaired: Boolean) {
    val qrBitmap = remember(pairingUrl) { QrCodeGenerator.generate(pairingUrl) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "スマホ・PCのブラウザでQRコードを読み取ってください",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Image(bitmap = qrBitmap.asImageBitmap(), contentDescription = "ペアリング用QRコード")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = pairingUrl, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))
        if (isPaired) {
            Text(text = "ペアリングされたブラウザがあります", fontSize = 18.sp)
        } else {
            Text(text = "同一Wi-Fi内のブラウザからアクセスしてください", fontSize = 16.sp)
        }
    }
}
