package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

// ponytail: Canvas-based skip-next icon
@Composable
fun SkipNextIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.15f)
            lineTo(w * 0.55f, h * 0.5f)
            lineTo(w * 0.1f, h * 0.85f)
            close()
        }
        drawPath(path, color = tint)
        drawRect(color = tint, topLeft = Offset(w * 0.65f, h * 0.15f), size = Size(w * 0.12f, h * 0.7f))
    }
}
