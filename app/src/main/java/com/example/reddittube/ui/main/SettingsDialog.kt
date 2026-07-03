package com.example.reddittube.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reddittube.utils.RedditOAuthHelper

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    var clientIdInput by remember { mutableStateOf(RedditOAuthHelper.getClientId(context)) }
    var userAgentInput by remember { mutableStateOf(RedditOAuthHelper.getUserAgent(context)) }
    var redirectUriInput by remember { mutableStateOf(RedditOAuthHelper.getRedirectUri(context)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color.Black,
        title = {
            Text("API Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = Color.Red)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    RedditOAuthHelper.saveApiCredentials(context, clientIdInput, userAgentInput, redirectUriInput)
                    Toast.makeText(context, "Settings saved. Please refresh.", Toast.LENGTH_SHORT).show()
                    onDismissRequest()
                }
            ) {
                Text("Save", color = Color.Red, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Reddit API Client ID",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    placeholder = { Text("Paste client ID here...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Red
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "User Agent",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = userAgentInput,
                    onValueChange = { userAgentInput = it },
                    placeholder = { Text("Enter User-Agent...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Red
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Redirect URI",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                TextField(
                    value = redirectUriInput,
                    onValueChange = { redirectUriInput = it },
                    placeholder = { Text("Enter Redirect URI...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Red
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Note: If left empty, defaults to the official RedReader Client ID, User Agent, and Redirect URI so the app works out of the box.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    )
}
