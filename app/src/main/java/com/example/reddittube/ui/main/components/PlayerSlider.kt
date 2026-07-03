package com.example.reddittube.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import java.util.Locale

// ponytail: Thin draggable seek bar with current position / duration display
@Composable
fun PlayerSlider(
    player: Player,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var dragFraction by remember { mutableFloatStateOf(-1f) } // -1 = not dragging

    LaunchedEffect(player) {
        while (true) {
            delay(250)
            val dur = player.duration
            if (dur > 0) {
                duration = dur.toFloat()
                if (dragFraction < 0f) {
                    currentPosition = player.currentPosition.toFloat()
                }
            }
        }
    }

    if (duration <= 0f) return

    val progress = if (dragFraction >= 0f) dragFraction else currentPosition / duration
    val clampedProgress = progress.coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime((clampedProgress * duration).toLong()),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            player.seekTo((clampedProgress * duration).toLong())
                            dragFraction = -1f
                        },
                        onDragCancel = { dragFraction = -1f },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
            // Track fill + thumb container
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .height(3.dp)
                    .align(Alignment.CenterStart)
            ) {
                // Filled track
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Red)
                )
                // Thumb circle at the end of the fill
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatTime(duration.toLong()),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
