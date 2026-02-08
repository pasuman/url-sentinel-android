package kr.seheon.urlpolice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

class BrowserManager(private val context: Context) {

    companion object {
        private val BROWSER_QUERY_URI: Uri = "https://example.com".toUri()
    }

    fun getInstalledBrowsers(): List<Browser> {
        val intent = Intent(Intent.ACTION_VIEW, BROWSER_QUERY_URI).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val packageManager = context.packageManager

        val resolveInfoList = packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL
        )

        return resolveInfoList
            .filter { it.activityInfo.packageName != context.packageName }
            .map { resolveInfo ->
                Browser(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
    }
}
