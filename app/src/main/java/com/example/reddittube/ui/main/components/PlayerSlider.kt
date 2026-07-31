package com.lean.reddittube.ui.main.components
import com.lean.reddittube.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun PlayerSlider(
    player: Player,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var dragFraction by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(player, player.isPlaying) {
        if (!player.isPlaying) {
            val dur = player.duration
            if (dur > 0 && dragFraction < 0f) {
                duration = dur.toFloat()
                currentPosition = player.currentPosition.toFloat()
            }
            return@LaunchedEffect
        }

        while (isActive) {
            val dur = player.duration
            if (dur > 0 && dragFraction < 0f) {
                duration = dur.toFloat()
                currentPosition = player.currentPosition.toFloat()
            }
            delay(250)
        }
    }

    val progress = if (duration > 0f) {
        if (dragFraction >= 0f) dragFraction else currentPosition / duration
    } else {
        0f
    }
    val clampedProgress = progress.coerceIn(0f, 1f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatTime((clampedProgress * duration).toLong()),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (duration > 0) {
                            player.seekTo(((offset.x / size.width).coerceIn(0f, 1f) * duration).toLong())
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            val frac = dragFraction.coerceIn(0f, 1f)
                            player.seekTo((frac * duration).toLong())
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
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
            // Filled track + glowing thumb
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrandRed, BrandRedLight)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = formatTime(duration.toLong()),
            color = Color.White.copy(alpha = 0.65f),
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
