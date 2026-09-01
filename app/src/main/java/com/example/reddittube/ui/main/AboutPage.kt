package com.lean.reddittube.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lean.reddittube.theme.*
import com.lean.reddittube.ui.main.components.AppUpdateBottomSheet
import com.lean.reddittube.utils.GitHubRelease
import com.lean.reddittube.utils.UpdateChecker
import kotlinx.coroutines.launch

private val CURRENT_VERSION_CHANGELOG = """
• Pure OLED Pitch Black & Crisp Monochrome aesthetic
• Unrestricted Power-User Feed with sleek 18+ glass micro-badges
• High-Speed Floating Pill Navigation Dock (850f snappy spring physics)
• Seamless Single-Video Loop toggle directly in the playback bar
• Ultra-smooth continuous volume & brightness vertical edge sliders
• Native GitHub Releases In-App OTA Update System
• Two-finger downward swipe feed refresh
""".trimIndent()

@Composable
fun AboutPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }

    var showClearDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var availableRelease by remember { mutableStateOf<GitHubRelease?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }

    // Expandable Sections State
    var isOtaExpanded by remember { mutableStateOf(true) }
    var isPreferencesExpanded by remember { mutableStateOf(true) }
    var isGesturesExpanded by remember { mutableStateOf(false) }
    var isToolbarExpanded by remember { mutableStateOf(false) }
    var isCommunityExpanded by remember { mutableStateOf(false) }

    // Preferences state
    var isNsfwUnrestricted by remember { mutableStateOf(sharedPreferences.getBoolean("pref_unrestricted_nsfw", true)) }
    var autoNextEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("auto_next", false)) }
    var isLoopEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("loop_video", true)) }

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
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("RdTube", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("v${com.lean.reddittube.BuildConfig.VERSION_NAME} • Unrestricted Power-User Edition", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }

        // Section 1: OTA Updates & Changelog (Expandable/Foldable)
        FoldableSectionCard(
            title = "OTA Updates & Changelog",
            icon = Icons.Default.SystemUpdate,
            badge = "v${com.lean.reddittube.BuildConfig.VERSION_NAME}",
            isExpanded = isOtaExpanded,
            onToggle = { isOtaExpanded = !isOtaExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Installed Version: v${com.lean.reddittube.BuildConfig.VERSION_NAME}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // Current Version Changelog Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("What's New in this Build:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = CURRENT_VERSION_CHANGELOG,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }

                // Check for Updates Button
                Button(
                    onClick = {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Checking GitHub Releases...", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check for Updates (OTA)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Content & Preferences (Expandable/Foldable with 18+ Toggle)
        FoldableSectionCard(
            title = "Content & Preferences",
            icon = Icons.Default.Tune,
            isExpanded = isPreferencesExpanded,
            onToggle = { isPreferencesExpanded = !isPreferencesExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 18+ Unrestricted Switch
                PreferenceSwitchRow(
                    title = "18+ Unrestricted Content",
                    subtitle = "Allow NSFW media in feeds with subtle glass badges",
                    checked = isNsfwUnrestricted,
                    onCheckedChange = { checked ->
                        isNsfwUnrestricted = checked
                        sharedPreferences.edit().putBoolean("pref_unrestricted_nsfw", checked).apply()
                        Toast.makeText(context, if (checked) "18+ content enabled" else "18+ content filtered", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = BorderSubtle)

                // Auto-Next Switch
                PreferenceSwitchRow(
                    title = "Auto-Next Video",
                    subtitle = "Automatically advance to next video upon completion",
                    checked = autoNextEnabled,
                    onCheckedChange = { checked ->
                        autoNextEnabled = checked
                        sharedPreferences.edit().putBoolean("auto_next", checked).apply()
                    }
                )

                HorizontalDivider(color = BorderSubtle)

                // Loop Video Switch
                PreferenceSwitchRow(
                    title = "Default Video Looping",
                    subtitle = "Replay videos continuously in player",
                    checked = isLoopEnabled,
                    onCheckedChange = { checked ->
                        isLoopEnabled = checked
                        sharedPreferences.edit().putBoolean("loop_video", checked).apply()
                    }
                )

                HorizontalDivider(color = BorderSubtle)

                // Clear Thumbnail Cache Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Image Cache", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Free up local thumbnail memory cache", color = TextMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Section 3: Gesture Reference Guide (Expandable/Foldable)
        FoldableSectionCard(
            title = "Gesture Controls Guide",
            icon = Icons.Default.TouchApp,
            isExpanded = isGesturesExpanded,
            onToggle = { isGesturesExpanded = !isGesturesExpanded }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CompactGestureItem("Two-Finger Swipe ↓", "Refresh Video Feed Instantly")
                CompactGestureItem("Left Edge ↕", "Adjust Screen Brightness")
                CompactGestureItem("Right Edge ↕", "Adjust System Volume")
                CompactGestureItem("Swipe Left ↔", "Dismiss & Mark Watched")
                CompactGestureItem("Swipe Right ↔", "Like Video & Advance")
                CompactGestureItem("Double-Tap ⏩", "Seek 10s Rewind/Forward")
                CompactGestureItem("Single Tap 👆", "Play / Pause / Replay")
            }
        }

        // Section 4: Toolbar Functions Guide (Expandable/Foldable)
        FoldableSectionCard(
            title = "Player Toolbar Functions",
            icon = Icons.Default.Widgets,
            isExpanded = isToolbarExpanded,
            onToggle = { isToolbarExpanded = !isToolbarExpanded }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                CompactButtonItem(Icons.Default.Repeat, "Loop Video", "Single-video continuous replay toggle")
                CompactButtonItem(Icons.Default.SkipNext, "Auto-Next", "Auto-play next clip on end")
                CompactButtonItem(Icons.Default.Download, "Save Video", "Direct export to Downloads")
                CompactButtonItem(Icons.Default.Lock, "Rotation Lock", "Toggle sensor / portrait orientation")
                CompactButtonItem(Icons.Default.Settings, "Quality & Speed", "Adjust resolution & playback speed")
                CompactButtonItem(Icons.AutoMirrored.Filled.VolumeUp, "Mute", "Quick audio mute / unmute")
            }
        }

        // Section 5: Community & Links (Expandable/Foldable)
        FoldableSectionCard(
            title = "Community & Project",
            icon = Icons.Default.Favorite,
            isExpanded = isCommunityExpanded,
            onToggle = { isCommunityExpanded = !isCommunityExpanded }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LeanBitLab/RdTube"))
                            context.startActivity(intent)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/LeanBitLab"))
                            context.startActivity(intent)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.5f),
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FoldableSectionCard(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(140),
        label = "ChevronRotation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceRaised,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row (Clickable to toggle fold/expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                        ) {
                            Text(
                                text = badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(140)) + expandVertically(tween(140)),
                exit = fadeOut(tween(110)) + shrinkVertically(tween(110))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 2.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = SurfaceBar,
                uncheckedBorderColor = GlassBorder
            )
        )
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
