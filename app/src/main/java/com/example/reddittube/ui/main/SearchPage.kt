package com.lean.reddittube.ui.main
import com.lean.reddittube.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchPage(
    currentSubscribed: Set<String>,
    onSubscribeToggle: (String) -> Unit,
    onSubredditSelect: (String) -> Unit,
    searchResults: List<String>,
    onSearchQuery: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("rdtube_prefs", Context.MODE_PRIVATE) }
    var searchQuery by remember { mutableStateOf("") }

    var recentSearches by remember {
        mutableStateOf(
            sharedPreferences.getStringSet("search_history", emptySet())?.toList() ?: emptyList()
        )
    }

    val saveSearchHistory: (String) -> Unit = { query ->
        if (query.isNotBlank()) {
            val updated = (listOf(query.lowercase()) + recentSearches.filterNot { it.equals(query, ignoreCase = true) }).take(8)
            recentSearches = updated
            sharedPreferences.edit().putStringSet("search_history", updated.toSet()).apply()
        }
    }

    val handleSelectSubreddit: (String) -> Unit = { sub ->
        saveSearchHistory(sub)
        onSubredditSelect(sub)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichObsidian)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = HPad, top = TopBarHeight, end = HPad, bottom = 84.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search subreddits...",
                    color = TextMuted,
                    fontSize = 15.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = BrandRed,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val query = searchQuery.trim().replace(" ", "")
                            if (query.isNotEmpty()) {
                                handleSelectSubreddit(query)
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Submit search",
                            tint = BrandRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceRaised,
                unfocusedContainerColor = SurfaceBase,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = BrandRed,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val trimmedQuery = searchQuery.trim().lowercase()
        if (trimmedQuery.isNotEmpty()) {
            LaunchedEffect(trimmedQuery) {
                delay(300) // 300ms debounce
                onSearchQuery(trimmedQuery)
            }
            if (searchResults.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    searchResults.forEach { sub ->
                        val isSubbed = currentSubscribed.contains(sub.lowercase())
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { handleSelectSubreddit(sub) },
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceRaised,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(BrandRed.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            sub.take(1).uppercase(),
                                            color = BrandRed,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("r/$sub", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                TextButton(onClick = { onSubscribeToggle(sub) }) {
                                    Text(
                                        if (isSubbed) "Subscribed" else "+ Subscribe",
                                        color = if (isSubbed) TextSecondary else BrandRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Searches",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        recentSearches = emptyList()
                        sharedPreferences.edit().remove("search_history").apply()
                    }
                ) {
                    Text("Clear", color = BrandRed, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentSearches.forEach { sub ->
                    Surface(
                        modifier = Modifier.clickable { handleSelectSubreddit(sub) },
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceRaised,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Text(
                            text = "r/$sub",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Subscriptions",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
            Text(
                "${currentSubscribed.size}",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (currentSubscribed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No subscriptions yet",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                currentSubscribed.sorted().forEach { sub ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSubredditSelect(sub) },
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceBase,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(BrandRed.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        sub.take(1).uppercase(),
                                        color = BrandRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("r/$sub", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            IconButton(onClick = { onSubscribeToggle(sub) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
