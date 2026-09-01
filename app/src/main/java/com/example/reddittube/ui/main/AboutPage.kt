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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lean.reddittube.ui.main.components.AppUpdateBottomSheet
import com.lean.reddittube.utils.GitHubRelease
import com.lean.reddittube.utils.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun AboutPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }

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
                    Text("Clear", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceRaised,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (availableRelease != null) {
        AppUpdateBottomSheet(
            release = availableRelease!!,
            onDismiss = { availableRelease = null },
            downloadProgress = downloadProgress,
            onInstallClick = { apkUrl ->
                coroutineScope.launch {
                    downloadProgress = 0f
                    UpdateChecker.downloadAndInstallApk(
                        context = context,
                        apkUrl = apkUrl,
                        onProgress = { progress -> downloadProgress = progress },
                        onComplete = { success, error ->
                            downloadProgress = null
                            if (success) {
                                availableRelease = null
                            } else {
                                Toast.makeText(context, "Update failed: $error", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(start = HPad, top = TopBarHeight + 2.dp, end = HPad, bottom = 90.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Header Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("RdTube", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v${com.lean.reddittube.BuildConfig.VERSION_NAME} • Unrestricted Power-User Edition", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Check for Updates Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            val release = UpdateChecker.checkForUpdate(com.lean.reddittube.BuildConfig.VERSION_NAME)
                            isCheckingUpdate = false
                            if (release != null) {
                                availableRelease = release
                            } else {
                                Toast.makeText(context, "RdTube is up to date! (v${com.lean.reddittube.BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isCheckingUpdate) "Checking for updates..." else "Check for Updates (OTA)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isCheckingUpdate) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact Gestures Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Gestures", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                CompactGestureItem("Two-Finger Swipe ↓", "Refresh Video Feed Instantly")
                CompactGestureItem("Left Edge ↕", "Adjust Screen Brightness")
                CompactGestureItem("Right Edge ↕", "Adjust System Volume")
                CompactGestureItem("Swipe Left ↔", "Dismiss & Mark Watched")
                CompactGestureItem("Swipe Right ↔", "Like Video & Advance")
                CompactGestureItem("Double-Tap ⏩", "Seek 10s Rewind/Forward")
                CompactGestureItem("Single Tap 👆", "Play / Pause / Replay")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Compact Toolbar Functions Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Toolbar Functions", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                CompactButtonItem(Icons.Default.Repeat, "Loop Video", "Single-video continuous replay toggle")
                CompactButtonItem(Icons.Default.SkipNext, "Auto-Next", "Auto-play next clip on end")
                CompactButtonItem(Icons.Default.Download, "Save Video", "Direct export to Downloads")
                CompactButtonItem(Icons.Default.Lock, "Rotation Lock", "Toggle sensor / portrait orientation")
                CompactButtonItem(Icons.Default.Settings, "Quality & Speed", "Adjust resolution & playback speed")
                CompactButtonItem(Icons.AutoMirrored.Filled.VolumeUp, "Mute", "Quick audio mute / unmute")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                shape = RoundedCornerShape(12.dp),
                color = SurfaceRaised,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sponsor", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showClearDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = SurfaceRaised,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
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
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
        ) {
            Text(
                text = tag,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "• $desc", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}
