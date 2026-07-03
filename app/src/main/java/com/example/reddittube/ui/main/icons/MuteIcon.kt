package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

// ponytail: Canvas-based muted speaker icon (speaker with X)
@Composable
fun MuteIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            lineTo(w * 0.45f, h * 0.35f)
            lineTo(w * 0.7f, h * 0.15f)
            lineTo(w * 0.7f, h * 0.85f)
            lineTo(w * 0.45f, h * 0.65f)
            lineTo(w * 0.2f, h * 0.65f)
            close()
        }
        drawPath(path, color = tint)
        drawLine(tint, Offset(w * 0.75f, h * 0.3f), Offset(w * 0.95f, h * 0.7f), strokeWidth = 2.dp.toPx())
        drawLine(tint, Offset(w * 0.95f, h * 0.3f), Offset(w * 0.75f, h * 0.7f), strokeWidth = 2.dp.toPx())
    }
}
