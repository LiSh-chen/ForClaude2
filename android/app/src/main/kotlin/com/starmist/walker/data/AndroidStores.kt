package com.starmist.walker.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** The files the small stores keep their values in on Android. */
internal val Context.settingsDataStore by preferencesDataStore(name = "settings")
internal val Context.regionDataStore by preferencesDataStore(name = "regions")
internal val Context.routeDataStore by preferencesDataStore(name = "routes")
internal val Context.exploreDataStore by preferencesDataStore(name = "explore")
