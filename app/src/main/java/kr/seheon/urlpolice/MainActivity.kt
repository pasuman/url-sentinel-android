package kr.seheon.urlpolice

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kr.seheon.urlpolice.ui.theme.URLPoliceTheme

class MainActivity : ComponentActivity() {
    private var interceptedUrl by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            URLPoliceTheme {
                URLPoliceApp(
                    interceptedUrl = interceptedUrl,
                    onDismiss = ::clearInterceptedUrl,
                    onOpenInBrowser = { url, browser ->
                        if (browser != null) {
                            openUrlInBrowser(url, browser)
                            clearInterceptedUrl()
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val isViewAction = intent?.action == Intent.ACTION_VIEW
        if (!isViewAction) return

        interceptedUrl = intent?.data
    }

    private fun clearInterceptedUrl() {
        interceptedUrl = null
    }

    private fun openUrlInBrowser(url: Uri, browser: Browser) {
        val browserIntent = Intent(Intent.ACTION_VIEW, url).apply {
            setPackage(browser.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(browserIntent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, url)
            startActivity(fallbackIntent)
        }
    }
}
