package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// ponytail: Canvas-based brightness/sun icon
@Composable
fun BrightnessIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 4
        drawCircle(color = Color.White, radius = radius)

        val rayStart = size.minDimension * 0.35f
        val rayEnd = size.minDimension * 0.48f
        for (i in 0 until 8) {
            val angle = i * Math.PI / 4
            val start = Offset(
                (center.x + rayStart * cos(angle)).toFloat(),
                (center.y + rayStart * sin(angle)).toFloat()
            )
            val end = Offset(
                (center.x + rayEnd * cos(angle)).toFloat(),
                (center.y + rayEnd * sin(angle)).toFloat()
            )
            drawLine(color = Color.White, start = start, end = end, strokeWidth = 2.dp.toPx())
        }
    }
}
