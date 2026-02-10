package kr.seheon.urlpolice

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kr.seheon.urlpolice.ui.theme.URLSentinelTheme

class MainActivity : ComponentActivity() {
    private var interceptedUrl by mutableStateOf<Uri?>(null)
    private lateinit var browserPreferences: BrowserPreferences

    private val defaultBrowserRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check if user has set this app as default browser
        if (isDefaultBrowser()) {
            // Automatically advance to next tutorial step
            lifecycleScope.launch {
                browserPreferences.setHasSeenDefaultBrowserTutorial(true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        browserPreferences = BrowserPreferences(this)
        handleIntent(intent)

        setContent {
            URLSentinelTheme {
                URLPoliceApp(
                    interceptedUrl = interceptedUrl,
                    onDismiss = ::clearInterceptedUrl,
                    onOpenInBrowser = { url, browser ->
                        if (browser != null) {
                            openUrlInBrowser(url, browser)
                            clearInterceptedUrl()
                        }
                    },
                    onOpenDefaultBrowserSettings = ::openDefaultBrowserSettings,
                    isDefaultBrowser = ::isDefaultBrowser
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

    private fun openDefaultBrowserSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)

            if (roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            ) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                defaultBrowserRoleLauncher.launch(intent)
            }
        } else {
            // Fallback for Android 9 and below: open app details settings
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = "package:$packageName".toUri()
                startActivity(intent)
            } catch (e: Exception) {
                // Last resort: open general settings
                val intent = Intent(Settings.ACTION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun isDefaultBrowser(): Boolean {
        val testIntent = Intent(Intent.ACTION_VIEW, "http://www.example.com".toUri())
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                testIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(testIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo?.activityInfo?.packageName == packageName
    }
}
