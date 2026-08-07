package com.lean.reddittube.ui.main
import com.lean.reddittube.theme.*

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
            text = { Text("This will clear the in-memory thumbnail cache to free up memory.", color = TextSecondary) },
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
            .padding(start = HPad, top = TopBarHeight + 4.dp, end = HPad, bottom = 96.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // App Header Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BrandRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        "RdTube User Guide",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "v${com.lean.reddittube.BuildConfig.VERSION_NAME} • Gestures & Controls Reference",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 1: Edge Drag Controls
        GuideSectionHeader(title = "Edge Drag Controls", icon = Icons.Default.TouchApp)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                GuideRow(
                    badge = "LEFT EDGE ↕",
                    title = "Brightness Adjustment",
                    desc = "Drag vertically on the left 48dp edge to adjust screen brightness."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = GlassBorder)
                GuideRow(
                    badge = "RIGHT EDGE ↕",
                    title = "Volume Adjustment",
                    desc = "Drag vertically on the right 48dp edge to adjust media volume."
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 2: Feed & Screen Gestures
        GuideSectionHeader(title = "Feed & Touch Gestures", icon = Icons.Default.Swipe)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                GuideRow(
                    badge = "SWIPE LEFT ↔",
                    title = "Dismiss / Mark Watched",
                    desc = "Swipe left across the video to mark it watched and load the next clip."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = GlassBorder)
                GuideRow(
                    badge = "SWIPE RIGHT ↔",
                    title = "Like Video",
                    desc = "Swipe right to add the video to your Liked list and advance."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = GlassBorder)
                GuideRow(
                    badge = "DOUBLE-TAP ⏩",
                    title = "Seek 10 Seconds",
                    desc = "Double-tap left half to rewind 10s or right half to fast-forward 10s."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = GlassBorder)
                GuideRow(
                    badge = "SINGLE TAP 👆",
                    title = "Toggle Overlay / Replay",
                    desc = "Tap canvas to show/hide controls. Replays automatically if video ended."
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 3: Player Toolbar Functions
        GuideSectionHeader(title = "Player Toolbar Functions", icon = Icons.Default.Tune)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ButtonFunctionRow(
                    icon = Icons.Default.SkipNext,
                    title = "Auto-Next",
                    desc = "Automatically advance to the next video when current clip ends."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorder)
                ButtonFunctionRow(
                    icon = Icons.Default.Download,
                    title = "Smart Save / Download",
                    desc = "Saves video directly to Downloads/RedditTube from local cache (0 re-download)."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorder)
                ButtonFunctionRow(
                    icon = Icons.Default.Lock,
                    title = "Rotation Lock",
                    desc = "Toggle between sensor auto-rotate and fixed portrait orientation."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorder)
                ButtonFunctionRow(
                    icon = Icons.Default.Settings,
                    title = "Quality & Speed",
                    desc = "Change video stream resolution (Auto/720p/480p) or playback speed (0.5x–2x)."
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = GlassBorder)
                ButtonFunctionRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Mute Toggle",
                    desc = "Quickly mute or restore video audio stream."
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section 4: Sponsor & Utilities
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, BrandRed.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Become a Sponsor",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Support open-source development",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/LeanBitLab"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sponsor", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SurfaceRaised,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Clear Thumbnail Cache",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Free up image memory",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = { showClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceGlass),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = BrandRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear", color = BrandRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun GuideSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrandRed,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
private fun GuideRow(badge: String, title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = BrandRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, BrandRed.copy(alpha = 0.3f))
            ) {
                Text(
                    text = badge,
                    color = BrandRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ButtonFunctionRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceBase),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandRed,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
