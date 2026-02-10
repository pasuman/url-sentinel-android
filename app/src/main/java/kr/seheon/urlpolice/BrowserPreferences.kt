package kr.seheon.urlpolice

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        private val KEY_ALWAYS_SHOW_RESULTS = booleanPreferencesKey("always_show_results")
        private val KEY_HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
        private val KEY_HAS_SEEN_DEFAULT_BROWSER_TUTORIAL = booleanPreferencesKey("has_seen_default_browser_tutorial")
    }

    val defaultBrowserPackage: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_BROWSER_PACKAGE]
    }

    val alwaysShowResults: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_ALWAYS_SHOW_RESULTS] ?: false
    }

    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAS_SEEN_WELCOME] ?: false
    }

    val hasSeenDefaultBrowserTutorial: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAS_SEEN_DEFAULT_BROWSER_TUTORIAL] ?: false
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

    suspend fun setAlwaysShowResults(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ALWAYS_SHOW_RESULTS] = enabled
        }
    }

    suspend fun setHasSeenWelcome(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_SEEN_WELCOME] = seen
        }
    }

    suspend fun setHasSeenDefaultBrowserTutorial(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_HAS_SEEN_DEFAULT_BROWSER_TUTORIAL] = seen
        }
    }
}
