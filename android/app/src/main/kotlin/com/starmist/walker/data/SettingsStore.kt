package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.starmist.core.metrics.Profile
import com.starmist.core.metrics.Sex
import com.starmist.core.sensor.FilterLevel
import com.starmist.core.steps.StepScaler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class UserSettings(
    val heightCm: Double = 170.0,
    val weightKg: Double = 65.0,
    val sex: Sex = Sex.OTHER,
    val dailyGoal: Int = 9_000,
    /** Correction factor applied to newly counted steps (1.0 = trust the sensor). */
    val multiplier: Double = 1.0,
    val strideOverrideM: Double? = null,
    val filterLevel: FilterLevel = FilterLevel.STANDARD,
    /**
     * Keep a foreground service running so the step sensor stays registered. On many phones the
     * hardware counter only counts while something is listening, so this is what makes counting
     * work with the screen off.
     */
    val stableMode: Boolean = true,
    /** Which traveller look is used in the world (index into the character list). */
    val characterId: Int = 0,
) {
    val profile: Profile
        get() = Profile(heightCm, weightKg, sex, strideOverrideM)
}

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    val flow: Flow<UserSettings> = context.settingsDataStore.data.map { it.toSettings() }

    suspend fun current(): UserSettings = flow.first()

    /**
     * The default goal moved from 8,000 to 9,000. Installs that never picked a goal have 8,000 stored
     * (every settings write saves all values), so move those once; anything else is left alone.
     */
    suspend fun migrateDefaults() {
        context.settingsDataStore.edit { prefs ->
            if (prefs[GOAL_DEFAULT_MOVED] == true) return@edit
            val stored = prefs[GOAL]
            if (stored != null && stored == OLD_DEFAULT_GOAL) prefs[GOAL] = 9_000
            prefs[GOAL_DEFAULT_MOVED] = true
        }
    }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsDataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[HEIGHT] = updated.heightCm
            prefs[WEIGHT] = updated.weightKg
            prefs[SEX] = updated.sex.name
            prefs[GOAL] = updated.dailyGoal
            prefs[MULTIPLIER] = StepScaler.clampMultiplier(updated.multiplier)
            val stride = updated.strideOverrideM
            if (stride == null) prefs.remove(STRIDE) else prefs[STRIDE] = stride
            prefs[FILTER] = updated.filterLevel.name
            prefs[STABLE] = updated.stableMode
            prefs[CHARACTER] = updated.characterId.coerceIn(0, CHARACTER_COUNT - 1)
        }
    }

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            heightCm = this[HEIGHT] ?: d.heightCm,
            weightKg = this[WEIGHT] ?: d.weightKg,
            sex = this[SEX]?.let { runCatching { Sex.valueOf(it) }.getOrNull() } ?: d.sex,
            dailyGoal = this[GOAL] ?: d.dailyGoal,
            multiplier = StepScaler.clampMultiplier(this[MULTIPLIER] ?: d.multiplier),
            strideOverrideM = this[STRIDE],
            filterLevel = this[FILTER]?.let { runCatching { FilterLevel.valueOf(it) }.getOrNull() } ?: d.filterLevel,
            stableMode = this[STABLE] ?: d.stableMode,
            characterId = (this[CHARACTER] ?: d.characterId).coerceIn(0, CHARACTER_COUNT - 1),
        )
    }

    private companion object {
        val HEIGHT = doublePreferencesKey("height_cm")
        val WEIGHT = doublePreferencesKey("weight_kg")
        val SEX = stringPreferencesKey("sex")
        val GOAL = intPreferencesKey("daily_goal")
        val MULTIPLIER = doublePreferencesKey("multiplier")
        val STRIDE = doublePreferencesKey("stride_override_m")
        val FILTER = stringPreferencesKey("filter_level")
        val STABLE = booleanPreferencesKey("stable_mode")
        val CHARACTER = intPreferencesKey("character_id")
        val GOAL_DEFAULT_MOVED = booleanPreferencesKey("goal_default_moved_to_9000")
        const val OLD_DEFAULT_GOAL = 8_000
        const val CHARACTER_COUNT = 4
    }
}
