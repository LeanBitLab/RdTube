package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// ponytail: Canvas-based download icon
@Composable
fun DownloadIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.65f), strokeWidth = 2.dp.toPx())
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx()))
        drawLine(color = color, start = Offset(w * 0.2f, h * 0.85f), end = Offset(w * 0.8f, h * 0.85f), strokeWidth = 2.dp.toPx())
    }
}
