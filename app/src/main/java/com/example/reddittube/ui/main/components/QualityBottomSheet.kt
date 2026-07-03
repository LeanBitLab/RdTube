package com.example.reddittube.ui.main.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ponytail: Bottom sheet for video quality + playback speed selection
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityBottomSheet(
    currentQuality: String,
    currentSpeed: Float,
    onQualitySelected: (String) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var selectedSpeed by remember { mutableStateOf(currentSpeed) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Video Quality",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val qualities = listOf("Auto", "1080p", "720p", "480p", "360p", "240p")
            qualities.forEach { qual ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onQualitySelected(qual)
                            Toast.makeText(context, "Quality updated to $qual", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(qual, color = Color.White, fontSize = 16.sp)
                    if (currentQuality == qual) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.width(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Playback Speed",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            speeds.forEach { speed ->
                val label = if (speed == 1.0f) "Normal" else "${speed}x"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedSpeed = speed
                            onSpeedSelected(speed)
                            Toast.makeText(context, "Speed set to $label", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = Color.White, fontSize = 16.sp)
                    if (selectedSpeed == speed) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.width(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
