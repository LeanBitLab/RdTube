package com.lean.reddittube.ui.main.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lean.reddittube.theme.BrandRed

// ponytail: reusable animated "loading next section" indicator (bouncing dots + optional label)
@Composable
fun SectionLoadingIndicator(
    modifier: Modifier = Modifier,
    label: String = "Loading more…"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val transition = rememberInfiniteTransition(label = "section-loading")
        repeat(3) { i ->
            val scale by transition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, delayMillis = i * 130, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot-$i"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(if (i == 1) BrandRed else Color.White.copy(alpha = 0.7f))
            )
            if (i < 2) Spacer(Modifier.width(6.dp))
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}
