package com.example.reddittube

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.reddittube.theme.RedditTubeTheme
import com.example.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        enableEdgeToEdge()
        setContent {
            RedditTubeTheme(darkTheme = true, dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val code = data.getQueryParameter("code")
        val state = data.getQueryParameter("state")
        if (!code.isNullOrEmpty() && !state.isNullOrEmpty()) {
            intent.data = null
            lifecycleScope.launch(Dispatchers.IO) {
                val success = RedditOAuthHelper.exchangeCodeForToken(applicationContext, code, state)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@MainActivity, "Logged in to Reddit successfully!", Toast.LENGTH_SHORT).show()
                        recreate()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to log in to Reddit.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
