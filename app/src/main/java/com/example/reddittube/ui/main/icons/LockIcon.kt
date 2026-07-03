package com.example.reddittube.ui.main.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

// ponytail: Canvas lock icon — cleaner geometry for small sizes
@Composable
fun LockIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val stroke = s * 0.12f
        drawArc(tint, 160f, 220f, false,
            topLeft = Offset(s * 0.3f, s * 0.08f),
            size = Size(s * 0.4f, s * 0.38f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(tint, Offset(s * 0.2f, s * 0.38f), Size(s * 0.6f, s * 0.55f),
            cornerRadius = CornerRadius(s * 0.08f)
        )
        drawCircle(tint, s * 0.06f, Offset(s * 0.5f, s * 0.58f))
        drawLine(tint, Offset(s * 0.5f, s * 0.64f), Offset(s * 0.5f, s * 0.78f), stroke * 0.8f)
    }
}

@Composable
fun LockOpenIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val stroke = s * 0.12f
        drawArc(tint, 150f, 230f, false,
            topLeft = Offset(s * 0.3f, s * 0.08f),
            size = Size(s * 0.4f, s * 0.38f),
            style = Stroke(width = stroke)
        )
        drawRoundRect(tint, Offset(s * 0.2f, s * 0.38f), Size(s * 0.6f, s * 0.55f),
            cornerRadius = CornerRadius(s * 0.08f)
        )
        drawCircle(tint, s * 0.06f, Offset(s * 0.5f, s * 0.58f))
        drawLine(tint, Offset(s * 0.5f, s * 0.64f), Offset(s * 0.5f, s * 0.78f), stroke * 0.8f)
    }
}
