package com.cuttingedge.virtualmic

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

    suspend fun setTheme(themeType: ThemeType) {
        context.dataStore.edit { appSettings ->
            appSettings[stringPreferencesKey("THEME_TYPE")] = themeType.name
        }
    }

    val themeTypeFlow: Flow<String> = context.dataStore.data.map {
        it[stringPreferencesKey("THEME_TYPE")] ?: "SYSTEM_DEFAULT"
    }
}

@Keep
enum class ThemeType {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}