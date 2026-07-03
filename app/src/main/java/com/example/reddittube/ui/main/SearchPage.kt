package com.example.reddittube.ui.main

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    "Search subreddits...",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 15.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            val query = searchQuery.trim().replace(" ", "")
                            if (query.isNotEmpty()) {
                                onSubredditSelect(query)
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.08f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.Red,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val trimmedQuery = searchQuery.trim().lowercase()
        if (trimmedQuery.isNotEmpty()) {
            LaunchedEffect(trimmedQuery) { onSearchQuery(trimmedQuery) }
            if (searchResults.isNotEmpty()) {
                Column(modifier = Modifier.wrapContentHeight()) {
                    searchResults.forEach { sub ->
                        val isSubbed = currentSubscribed.contains(sub.lowercase())
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable { onSubredditSelect(sub) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Red.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) { Text(sub.take(1).uppercase(), color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("r/$sub", color = Color.White, fontSize = 14.sp)
                            }
                            TextButton(onClick = { onSubscribeToggle(sub) }) {
                                Text(if (isSubbed) "Unsub" else "Sub", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Subscriptions",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Text(
                "${currentSubscribed.size}",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 13.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (currentSubscribed.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No subscriptions yet",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    currentSubscribed.sorted().forEachIndexed { i, sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (i % 2 == 0) Color.White.copy(alpha = 0.03f) else Color.Transparent)
                                .clickable { onSubredditSelect(sub) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        sub.take(1).uppercase(),
                                        color = Color.Red,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("r/$sub", color = Color.White, fontSize = 15.sp)
                            }
                            IconButton(onClick = { onSubscribeToggle(sub) }, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color.White.copy(alpha = 0.3f),
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
