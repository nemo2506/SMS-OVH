package com.miseservice.smsovh.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

object ApiTokenManager {
    private const val PREF_NAME = "api_token_prefs"
    private const val KEY_TOKEN = "api_token"

    fun getToken(context: Context): String {
        val prefs = getPrefs(context)
        var token = prefs.getString(KEY_TOKEN, null)
        if (token == null) {
            token = UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
        return token
    }

    private fun getPrefs(context: Context) =
        EncryptedSharedPreferences.create(
            PREF_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
}

