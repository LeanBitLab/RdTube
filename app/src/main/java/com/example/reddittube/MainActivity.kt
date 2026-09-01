package com.lean.reddittube

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.lean.reddittube.theme.RdTubeTheme
import com.lean.reddittube.utils.RedditOAuthHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        // ponytail: app chrome is always dark, so force light (white) status/nav icons
        // regardless of system night mode — fixes invisible icons in system light mode
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        handleIntent(intent)

        setContent {
            RdTubeTheme(darkTheme = true, dynamicColor = false) {
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
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "redreader" && uri.host == "rr_oauth_redir" || uri.scheme == "rdtube" && uri.host == "oauth") {
            lifecycleScope.launch {
                val success = RedditOAuthHelper.handleOAuthCallback(this@MainActivity, uri)
                if (success) {
                    val user = RedditOAuthHelper.getUsername(this@MainActivity) ?: "Reddit User"
                    Toast.makeText(this@MainActivity, "Connected as $user! 18+ content unlocked.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "Reddit authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        com.lean.reddittube.util.PerfTelemetryController.getInstance(this).isPaused = false
    }

    override fun onStop() {
        super.onStop()
        com.lean.reddittube.util.PerfTelemetryController.getInstance(this).isPaused = true
    }
}
