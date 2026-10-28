package com.lean.reddittube.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lean.reddittube.theme.*
import com.lean.reddittube.utils.RedditOAuthHelper

private val CURRENT_VERSION_CHANGELOG = """
• Pitch Black OLED Theme: Pure monochrome aesthetic with solid surfaces and adaptive icon
• Categorized Settings: Clean background-based sections without distracting wireframe borders
• Persistent Rotation Lock: Feed orientation state persists across swipe navigation
• Auto-Mute NSFW Option: Configurable preference to automatically silence mature videos
• Unified Search History: Immediate suggestion recall across both Subreddit and Video searches
• Video Search Filters: Granular Relevance, Hot, Top, and New filters directly in search results
• High-Resolution Thumbnails: Optimized source preview images with instant cache controls
• Performance: R8 Full Mode, automatic release log stripping, and token concurrency safety
""".trimIndent()

private val SettingsCardBg = Color(0xFF141414)
private val SettingsSubCardBg = Color(0xFF0C0C0C)
private val SettingsPillBg = Color(0xFF222222)
private val SettingsDividerColor = Color(0xFF1E1E1E)
private val SettingsButtonBg = Color(0xFF1E1E1E)

@Composable
fun AboutPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }

    var showClearDialog by remember { mutableStateOf(false) }

    // API Keys state
    var isOverridesEnabled by remember { mutableStateOf(RedditOAuthHelper.isOverridesEnabled(context)) }
    var customClientId by remember { mutableStateOf(RedditOAuthHelper.getCustomClientId(context)) }
    var customUserAgent by remember { mutableStateOf(RedditOAuthHelper.getCustomUserAgent(context)) }
    var customRedirectUri by remember { mutableStateOf(RedditOAuthHelper.getCustomRedirectUri(context)) }

    var showClientIdDialog by remember { mutableStateOf(false) }
    var showUserAgentDialog by remember { mutableStateOf(false) }
    var showRedirectUriDialog by remember { mutableStateOf(false) }

    // Account state
    var isLoggedIn by remember { mutableStateOf(RedditOAuthHelper.isLoggedIn(context)) }
    var username by remember { mutableStateOf(RedditOAuthHelper.getUsername(context)) }

    val accountBadge = when {
        isLoggedIn -> "u/${username ?: "User"}"
        isOverridesEnabled -> "Custom Keys"
        else -> null
    }

    // Unified Category Card States (Folded by default)
    var isPreferencesExpanded by remember { mutableStateOf(false) }
    var isRedditAccountExpanded by remember { mutableStateOf(false) }
    var isHelpExpanded by remember { mutableStateOf(false) }
    var isSocialsExpanded by remember { mutableStateOf(false) }
    var isReleaseNotesExpanded by remember { mutableStateOf(false) }

    // Preferences state
    var autoMuteNsfw by remember { mutableStateOf(sharedPreferences.getBoolean("pref_auto_mute_nsfw", true)) }
    var isNsfwUnrestricted by remember { mutableStateOf(sharedPreferences.getBoolean("pref_unrestricted_nsfw", true)) }
    var autoNextEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("auto_next", false)) }
    var isLoopEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("loop_video", true)) }
    var thumbnailQuality by remember { mutableStateOf(sharedPreferences.getString("pref_thumbnail_quality", "High") ?: "High") }
    var prefetchDepth by remember { mutableStateOf(sharedPreferences.getString("pref_prefetch_depth", "Balanced (10)") ?: "Balanced (10)") }
    var defaultAudioUnmuted by remember { mutableStateOf(sharedPreferences.getBoolean("pref_default_unmuted", true)) }
    var hapticFeedbackEnabled by remember { mutableStateOf(sharedPreferences.getBoolean("pref_haptic_feedback", true)) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Thumbnail Cache?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Frees up in-memory image thumbnail cache.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        com.lean.reddittube.ui.main.components.clearThumbnailCache()
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
            containerColor = Color(0xFF181818),
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showClientIdDialog) {
        ApiKeyInputDialog(
            title = "Reddit API Client ID",
            initialValue = customClientId,
            placeholder = "e.g. uLo0abP7KJQqP1G74o4TNg",
            hint = "Custom Reddit OAuth Client ID (Installed App type) for users with approved developer access or pre-existing apps.",
            onSave = { newId ->
                customClientId = newId
                RedditOAuthHelper.setCustomClientId(context, newId)
                Toast.makeText(context, if (newId.isNotEmpty()) "Custom Client ID saved" else "Reset to default Client ID", Toast.LENGTH_SHORT).show()
            },
            onReset = {
                customClientId = ""
                RedditOAuthHelper.setCustomClientId(context, "")
                Toast.makeText(context, "Reset to default RedReader Client ID", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClientIdDialog = false }
        )
    }

    if (showUserAgentDialog) {
        ApiKeyInputDialog(
            title = "User Agent",
            initialValue = customUserAgent,
            placeholder = "e.g. android:rdtube:v1.0.7 (by /u/yourname)",
            hint = "Custom HTTP User-Agent string sent with Reddit API requests.",
            onSave = { newUa ->
                customUserAgent = newUa
                RedditOAuthHelper.setCustomUserAgent(context, newUa)
                Toast.makeText(context, if (newUa.isNotEmpty()) "Custom User-Agent saved" else "Reset to default User-Agent", Toast.LENGTH_SHORT).show()
            },
            onReset = {
                customUserAgent = ""
                RedditOAuthHelper.setCustomUserAgent(context, "")
                Toast.makeText(context, "Reset to default User-Agent", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showUserAgentDialog = false }
        )
    }

    if (showRedirectUriDialog) {
        ApiKeyInputDialog(
            title = "Redirect URI",
            initialValue = customRedirectUri,
            placeholder = "e.g. redreader://rr_oauth_redir or rdtube://oauth",
            hint = "OAuth callback URI configured in your Reddit App settings.",
            onSave = { newUri ->
                customRedirectUri = newUri
                RedditOAuthHelper.setCustomRedirectUri(context, newUri)
                Toast.makeText(context, if (newUri.isNotEmpty()) "Custom Redirect URI saved" else "Reset to default Redirect URI", Toast.LENGTH_SHORT).show()
            },
            onReset = {
                customRedirectUri = ""
                RedditOAuthHelper.setCustomRedirectUri(context, "")
                Toast.makeText(context, "Reset to default Redirect URI", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showRedirectUriDialog = false }
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
        // 1. App Preferences
        FoldableSectionCard(
            title = "App Preferences",
            icon = Icons.Default.Tune,
            isExpanded = isPreferencesExpanded,
            onToggle = { isPreferencesExpanded = !isPreferencesExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section: Playback & Audio
                Text(
                    text = "Playback & Audio",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                PreferenceSwitchRow(
                    title = "Default Audio Unmuted",
                    subtitle = "Start video playback with sound unmuted",
                    checked = defaultAudioUnmuted,
                    onCheckedChange = { checked ->
                        defaultAudioUnmuted = checked
                        sharedPreferences.edit().putBoolean("pref_default_unmuted", checked).apply()
                    }
                )

                HorizontalDivider(color = SettingsDividerColor)

                PreferenceSwitchRow(
                    title = "Auto-Mute NSFW Videos",
                    subtitle = "Automatically start 18+ unrestricted videos muted",
                    checked = autoMuteNsfw,
                    onCheckedChange = { checked ->
                        autoMuteNsfw = checked
                        sharedPreferences.edit().putBoolean("pref_auto_mute_nsfw", checked).apply()
                        Toast.makeText(context, if (checked) "Auto-mute NSFW enabled" else "Auto-mute NSFW disabled", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = SettingsDividerColor)

                PreferenceSwitchRow(
                    title = "Default Video Looping",
                    subtitle = "Replay videos continuously in player",
                    checked = isLoopEnabled,
                    onCheckedChange = { checked ->
                        isLoopEnabled = checked
                        sharedPreferences.edit().putBoolean("loop_video", checked).apply()
                    }
                )

                HorizontalDivider(color = SettingsDividerColor)

                PreferenceSwitchRow(
                    title = "Auto-Next Video",
                    subtitle = "Automatically advance to next video upon completion",
                    checked = autoNextEnabled,
                    onCheckedChange = { checked ->
                        autoNextEnabled = checked
                        sharedPreferences.edit().putBoolean("auto_next", checked).apply()
                    }
                )

                HorizontalDivider(color = SettingsDividerColor)

                // Section: Media Quality & Cache
                Text(
                    text = "Media Quality & Cache",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Thumbnail Resolution", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Quality of video preview thumbnails in feeds and search", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("High", "Balanced", "Data Saver").forEach { q ->
                                val isSelected = thumbnailQuality == q
                                Surface(
                                    onClick = {
                                        thumbnailQuality = q
                                        sharedPreferences.edit().putString("pref_thumbnail_quality", q).apply()
                                        com.lean.reddittube.ui.main.components.clearThumbnailCache()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = q,
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier
                                            .padding(vertical = 6.dp)
                                            .fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SettingsDividerColor)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Feed Prefetch Depth", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Number of upcoming video streams buffered in the background", color = TextMuted, fontSize = 11.sp)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Lite (5)", "Balanced (10)", "Max (20)").forEach { depth ->
                                val isSelected = prefetchDepth == depth
                                Surface(
                                    onClick = {
                                        prefetchDepth = depth
                                        sharedPreferences.edit().putString("pref_prefetch_depth", depth).apply()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = depth,
                                        color = if (isSelected) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier
                                            .padding(vertical = 6.dp)
                                            .fillMaxWidth(),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SettingsDividerColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showClearDialog = true }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Image Cache", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Free up local thumbnail memory cache", color = TextMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }

                HorizontalDivider(color = SettingsDividerColor)

                // Section: Content & Restrictions
                Text(
                    text = "Content & Restrictions",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                PreferenceSwitchRow(
                    title = "Unrestricted Content",
                    subtitle = "Allow unrestricted media in feeds with subtle glass badges",
                    checked = isNsfwUnrestricted,
                    onCheckedChange = { checked ->
                        isNsfwUnrestricted = checked
                        sharedPreferences.edit().putBoolean("pref_unrestricted_nsfw", checked).apply()
                        Toast.makeText(context, if (checked) "Unrestricted content enabled" else "Unrestricted content filtered", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = SettingsDividerColor)

                // Section: Interface & Haptics
                Text(
                    text = "Interface & Haptics",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )

                PreferenceSwitchRow(
                    title = "Haptic Vibration Feedback",
                    subtitle = "Tactile response on edge sliders, likes, and feed pull-to-refresh",
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { checked ->
                        hapticFeedbackEnabled = checked
                        sharedPreferences.edit().putBoolean("pref_haptic_feedback", checked).apply()
                    }
                )
            }
        }

        // 2. Reddit Account & API Keys
        FoldableSectionCard(
            title = "Reddit Account & API Keys",
            icon = Icons.Default.AccountCircle,
            badge = accountBadge,
            isExpanded = isRedditAccountExpanded,
            onToggle = { isRedditAccountExpanded = !isRedditAccountExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Reddit Account Session
                if (isLoggedIn) {
                    Text(
                        text = "Connected as u/${username ?: "Reddit User"}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "✓ Unrestricted subreddits and media streams are unlocked via your Reddit authorization.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            RedditOAuthHelper.logout(context)
                            isLoggedIn = false
                            username = null
                            Toast.makeText(context, "Logged out from Reddit", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SettingsButtonBg,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Log Out from Reddit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Reddit API requires account authorization for unrestricted subreddits and media access. Tap below to connect securely via Reddit OAuth.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            RedditOAuthHelper.launchLogin(context)
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
                        Icon(Icons.Default.Login, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Connect Reddit Account (1-Tap)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = SettingsDividerColor)

                // 2. Advisory & Recommendation Callout
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SettingsSubCardBg
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFFFF4500),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Reddit API Policy & Custom Keys",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Reddit's Developer Terms restrict new self-service app creation on reddit.com/prefs/apps (defaulting accounts to a 0-app limit requiring manual approval). RdTube works out of the box using built-in defaults. If you already have a pre-existing Client ID or approved developer status, enable Overrides below to use your personal credentials.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/prefs/apps"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SettingsButtonBg,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reddit App Preferences (reddit.com/prefs/apps)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = SettingsDividerColor)

                // 3. API Key & Client ID Overrides Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Enable Overrides",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        val text = buildAnnotatedString {
                            append("Use your custom Client ID, User Agent, and Redirect URI instead of the built-in defaults, which are ")
                            withStyle(SpanStyle(color = Color(0xFFFF4500), fontWeight = FontWeight.Bold)) {
                                append("RedReader")
                            }
                        }
                        Text(
                            text = text,
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = isOverridesEnabled,
                        onCheckedChange = { checked ->
                            isOverridesEnabled = checked
                            RedditOAuthHelper.setOverridesEnabled(context, checked)
                            Toast.makeText(
                                context,
                                if (checked) "Custom API key overrides enabled" else "Using default RedReader API keys",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Color.White,
                            uncheckedThumbColor = Color(0xFFC0C0C0),
                            uncheckedTrackColor = Color(0xFF262626),
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }

                // Custom Credentials rows only show when Enable Overrides is switched ON
                AnimatedVisibility(
                    visible = isOverridesEnabled,
                    enter = fadeIn(tween(140)) + expandVertically(tween(140)),
                    exit = fadeOut(tween(110)) + shrinkVertically(tween(110))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(bottom = 8.dp))

                        // Reddit API Client ID Row
                        ApiKeySettingRow(
                            title = "Reddit API Client ID",
                            value = customClientId.ifEmpty { "Tap to set custom Client ID" },
                            isCustom = customClientId.isNotEmpty(),
                            onClick = { showClientIdDialog = true }
                        )

                        HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(vertical = 4.dp))

                        // User Agent Row
                        ApiKeySettingRow(
                            title = "User Agent",
                            value = customUserAgent.ifEmpty { "Tap to set custom User Agent" },
                            isCustom = customUserAgent.isNotEmpty(),
                            onClick = { showUserAgentDialog = true }
                        )

                        HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(vertical = 4.dp))

                        // Redirect URI Row
                        ApiKeySettingRow(
                            title = "Redirect URI",
                            value = customRedirectUri.ifEmpty { "Tap to set custom Redirect URI" },
                            isCustom = customRedirectUri.isNotEmpty(),
                            onClick = { showRedirectUriDialog = true }
                        )
                    }
                }
            }
        }

        // 3. Help & Reference
        FoldableSectionCard(
            title = "Help & Reference",
            icon = Icons.Default.HelpOutline,
            isExpanded = isHelpExpanded,
            onToggle = { isHelpExpanded = !isHelpExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Section: Gesture Controls Guide
                Text(
                    text = "Gesture Controls Guide",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    CompactGestureItem("Two-Finger Swipe ↓", "Refresh Video Feed Instantly")
                    CompactGestureItem("Left Edge ↕", "Adjust Screen Brightness")
                    CompactGestureItem("Right Edge ↕", "Adjust System Volume")
                    CompactGestureItem("Swipe Left ↔", "Dismiss & Mark Watched")
                    CompactGestureItem("Swipe Right ↔", "Like Video & Advance")
                    CompactGestureItem("Double-Tap ⏩", "Seek 10s Rewind/Forward")
                    CompactGestureItem("Single Tap 👆", "Play / Pause / Replay")
                }

                HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(vertical = 4.dp))

                // Section: Player Toolbar Functions
                Text(
                    text = "Player Toolbar Functions",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    CompactButtonItem(Icons.Default.Repeat, "Loop Video", "Single-video continuous replay toggle")
                    CompactButtonItem(Icons.Default.SkipNext, "Auto-Next", "Auto-play next clip on end")
                    CompactButtonItem(Icons.Default.Download, "Save Video", "Direct export to Downloads")
                    CompactButtonItem(Icons.Default.Lock, "Rotation Lock", "Toggle sensor / portrait orientation")
                    CompactButtonItem(Icons.Default.Settings, "Quality & Speed", "Adjust resolution & playback speed")
                    CompactButtonItem(Icons.AutoMirrored.Filled.VolumeUp, "Mute", "Quick audio mute / unmute")
                }

                HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(vertical = 4.dp))

                // Section: Security & FAQ
                Text(
                    text = "Account Safety & FAQ",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Text(
                    text = "Why is login required for unrestricted subreddits?",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reddit's API requires user authorization for unmoderated/unrestricted media. Logging in authenticates your preferences directly with Reddit's servers.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        // 4. Official Channels & Socials
        FoldableSectionCard(
            title = "Official Channels & Socials",
            icon = Icons.Default.Forum,
            isExpanded = isSocialsExpanded,
            onToggle = { isSocialsExpanded = !isSocialsExpanded }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Entirely Community Funded",
                    color = TextPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "RdTube is 100% free, open-source, and entirely community funded with zero ads, investors, or telemetry. Your sponsorship keeps development completely independent!",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Sponsor Development",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Sponsorship & Donation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/sponsors/LeanBitLab"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GitHub Sponsors", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://opencollective.com/leanbitlab-org"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color(0xFF1F6FEB), modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Collective", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Official Channels & Socials",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Social Grid Row 1: Telegram & Reddit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/LeanBitLab"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF29B6F6), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Telegram", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/r/LeanBitLab_/"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Forum, contentDescription = null, tint = Color(0xFFFF4500), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("r/LeanBitLab_", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Social Grid Row 2: X (Twitter) & YouTube
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/LeanBitLab"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("X (Twitter)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@LeanBitLab"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color(0xFFFF0000), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("YouTube", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(vertical = 4.dp))

                // Project & Code Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LeanBitLab/RdTube"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GitHub Repo", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LeanBitLab/RdTube/issues"))
                                context.startActivity(intent)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = SettingsSubCardBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Report Issue", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // 5. Version & Release Notes
        FoldableSectionCard(
            title = "Version & Release Notes",
            icon = Icons.Default.Info,
            badge = "v${com.lean.reddittube.BuildConfig.VERSION_NAME}",
            isExpanded = isReleaseNotesExpanded,
            onToggle = { isReleaseNotesExpanded = !isReleaseNotesExpanded }
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
                    color = SettingsSubCardBg
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

                // View Releases on GitHub Button
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LeanBitLab/RdTube/releases"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsButtonBg,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View Releases on GitHub", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(18.dp),
        color = SettingsCardBg
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SettingsPillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(title, color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SettingsPillBg
                        ) {
                            Text(
                                text = badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                    HorizontalDivider(color = SettingsDividerColor, modifier = Modifier.padding(bottom = 12.dp))
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
                uncheckedThumbColor = Color(0xFFC0C0C0),
                uncheckedTrackColor = Color(0xFF262626),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun CompactGestureItem(tag: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = SettingsPillBg
        ) {
            Text(
                text = tag,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CompactButtonItem(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SettingsPillBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "• $desc", color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun ApiKeySettingRow(
    title: String,
    value: String,
    isCustom: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isCustom) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SettingsPillBg
                    ) {
                        Text(
                            text = "CUSTOM",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = if (isCustom) Color(0xFFE0E0E0) else TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ApiKeyInputDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    hint: String? = null,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hint != null) {
                    Text(hint, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(placeholder, color = TextMuted, fontSize = 12.sp) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SettingsSubCardBg,
                        unfocusedContainerColor = SettingsSubCardBg,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(text.trim())
                    onDismiss()
                }
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (onReset != null && initialValue.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            onReset()
                            onDismiss()
                        }
                    ) {
                        Text("Reset", color = Color(0xFFFF5252))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = Color(0xFF181818),
        shape = RoundedCornerShape(18.dp)
    )
}
