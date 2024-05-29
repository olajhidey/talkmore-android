package com.example.talkmoreapp.utils

import android.content.Context
import android.widget.Toast
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object Constants {
    const val USERNAME_MAP_KEY = "username_pref_key"
    const val PASSWORD_MAP_KEY = "password_pref_key"
    const val DOMAIN_MAP_KEY = "domain_pref_key"
    const val REMOTE_FROM = "remote_contract"
    const val REMOTE_TO = "remote_address"
    const val CALL_DIRECTION = "remote_direction"
}


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "talkmore_datastore")

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}