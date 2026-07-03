package com.example.reddittube.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HistoryDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reddittube_prefs", Context.MODE_PRIVATE) }
    val watchedIds = remember {
        prefs.getStringSet("watched_ids", emptySet())?.toList()?.sorted() ?: emptyList()
    }
    val watchedTitles = remember {
        try {
            val json = prefs.getString("watched_titles", null)
            if (json != null) org.json.JSONObject(json) else org.json.JSONObject()
        } catch (_: Exception) { org.json.JSONObject() }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Watch History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${watchedIds.size} videos", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close", color = Color.Red)
            }
        },
        confirmButton = {
            if (watchedIds.isNotEmpty()) {
                TextButton(onClick = {
                    prefs.edit().remove("watched_ids").remove("watched_titles").apply()
                    onDismissRequest()
                }) {
                    Text("Clear All", color = Color.Red)
                }
            }
        },
        text = {
            if (watchedIds.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No watched videos yet", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    watchedIds.forEachIndexed { index, id ->
                        val title = watchedTitles.optString(id, id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (index % 2 == 0) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Red.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (title != id) {
                                    Text(
                                        text = id,
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (index < watchedIds.lastIndex) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    )
}
