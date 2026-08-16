package com.example

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.service.FloatingDictionaryService
import com.example.ui.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var pendingTextToProcess: String? = null
    private val viewModel: DictionaryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingTextToProcess = extractTextFromIntent(intent)
        handlePendingText()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                MainScreen(activity = this@MainActivity, viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTextToProcess = extractTextFromIntent(intent)
        handlePendingText()
    }

    private fun handlePendingText() {
        val text = pendingTextToProcess ?: return
        if (Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, FloatingDictionaryService::class.java).apply {
                putExtra("EXTRA_WORD", text)
            }
            startService(serviceIntent)
            finish()
        } else {
            Toast.makeText(
                this,
                "Permita a sobreposição para usar a janela flutuante",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            finish()
        }
    }

    private fun extractTextFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                    ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            Intent.ACTION_SEND -> {
                if ("text/plain" == intent.type) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else null
            }
            "colordict.intent.action.SEARCH" -> {
                intent.getStringExtra("EXTRA_QUERY")
            }
            Intent.ACTION_SEARCH -> {
                intent.getStringExtra(SearchManager.QUERY)
            }
            else -> null
        }
    }
}
