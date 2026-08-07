package com.lean.reddittube.ui.main
import com.lean.reddittube.theme.*

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Thumbnail Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Frees up in-memory image thumbnail cache.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = BrandRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceRaised,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RichObsidian)
            .statusBarsPadding()
            .padding(start = HPad, top = TopBarHeight + 2.dp, end = HPad, bottom = 84.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Header Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("RdTube", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v${com.lean.reddittube.BuildConfig.VERSION_NAME} • Gestures & Quick Guide", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compact Gestures Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Gestures", color = BrandRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                CompactGestureItem("Left Edge ↕", "Adjust Screen Brightness")
                CompactGestureItem("Right Edge ↕", "Adjust System Volume")
                CompactGestureItem("Swipe Left ↔", "Dismiss & Mark Watched")
                CompactGestureItem("Swipe Right ↔", "Like Video & Advance")
                CompactGestureItem("Double-Tap ⏩", "Seek 10s Rewind/Forward")
                CompactGestureItem("Single Tap 👆", "Play / Pause / Replay")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Compact Toolbar Functions Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Toolbar Functions", color = BrandRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                CompactButtonItem(Icons.Default.SkipNext, "Auto-Next", "Auto-play next clip on end")
                CompactButtonItem(Icons.Default.Download, "Save Video", "0-download export to Downloads")
                CompactButtonItem(Icons.Default.Lock, "Rotation Lock", "Toggle sensor / portrait orientation")
                CompactButtonItem(Icons.Default.Settings, "Quality & Speed", "Adjust resolution & playback speed")
                CompactButtonItem(Icons.AutoMirrored.Filled.VolumeUp, "Mute", "Quick audio mute / unmute")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Actions Row (Sponsor & Clear Cache)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/LeanBitLab"))
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(10.dp),
                color = SurfaceRaised,
                border = BorderStroke(1.dp, BrandRed.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = BrandRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sponsor Project", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showClearDialog = true },
                shape = RoundedCornerShape(10.dp),
                color = SurfaceRaised,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Clear Cache", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun CompactGestureItem(tag: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = BrandRed.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, BrandRed.copy(alpha = 0.25f))
        ) {
            Text(
                text = tag,
                color = BrandRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompactButtonItem(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = BrandRed, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "• $desc", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}
