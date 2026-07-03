package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
fun PlayArrowIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(path, color = tint)
    }
}
