package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

// ponytail: Canvas-based pause icon to avoid importing heavy material-icons-extended dependencies
@Composable
fun PauseIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = Color.White, topLeft = Offset(w * 0.28f, h * 0.2f), size = Size(w * 0.12f, h * 0.6f))
        drawRect(color = Color.White, topLeft = Offset(w * 0.6f, h * 0.2f), size = Size(w * 0.12f, h * 0.6f))
    }
}
