package kr.seheon.urlpolice

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "browser_preferences"
)

class BrowserPreferences(private val context: Context) {

    companion object {
        private val KEY_DEFAULT_BROWSER_PACKAGE = stringPreferencesKey("default_browser_package")
    }

    val defaultBrowserPackage: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_BROWSER_PACKAGE]
    }

    suspend fun setDefaultBrowser(packageName: String?) {
        context.dataStore.edit { preferences ->
            if (packageName == null) {
                preferences.remove(KEY_DEFAULT_BROWSER_PACKAGE)
            } else {
                preferences[KEY_DEFAULT_BROWSER_PACKAGE] = packageName
            }
        }
    }
}
