package com.example.btfinder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bt_finder_prefs")

/**
 * Persistencia del último dispositivo seleccionado (incluida en el MVP,
 * sección 2 del documento de diseño). Solo se guarda la dirección MAC:
 * ningún token, credencial ni dato personal, conforme a los criterios de
 * aceptación del MVP (sección 20).
 */
class PreferencesRepository(context: Context) {

    private val appContext = context.applicationContext
    private val lastDeviceAddressKey = stringPreferencesKey("last_device_address")

    val lastDeviceAddress: Flow<String?> =
        appContext.dataStore.data.map { prefs -> prefs[lastDeviceAddressKey] }

    suspend fun saveLastDeviceAddress(address: String) {
        appContext.dataStore.edit { prefs ->
            prefs[lastDeviceAddressKey] = address
        }
    }
}
