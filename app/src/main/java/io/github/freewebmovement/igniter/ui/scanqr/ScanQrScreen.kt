package io.github.freewebmovement.igniter.ui.scanqr

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.freewebmovement.igniter.R

private val ScanFrameGreen = Color(0xFF00E676)

/** Full-screen camera preview with the QR focus frame and scan hint. */
@Composable
fun ScanQrScreen(onPreviewReady: (PreviewView) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PreviewView(context).also { view ->
                    onPreviewReady(view)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .border(3.dp, ScanFrameGreen, RoundedCornerShape(8.dp))
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .background(Color(0x88000000))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.qr_scan_hint),
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}
