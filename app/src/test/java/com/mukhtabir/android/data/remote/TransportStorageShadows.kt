package com.mukhtabir.android.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** JVM transport tests have no AndroidKeyStore service. Encryption requires device verification. */
@Implements(MasterKeys::class, isInAndroidSdk = false)
class TransportMasterKeysShadow {
    companion object {
        @JvmStatic
        @Implementation
        fun getOrCreate(spec: KeyGenParameterSpec): String = "transport-test-master-key"
    }
}

/** Retains real CookieJar/preferences behavior while substituting only encrypted storage creation. */
@Implements(EncryptedSharedPreferences::class, isInAndroidSdk = false)
class TransportPreferencesShadow {
    companion object {
        @JvmStatic
        @Implementation
        fun create(
            fileName: String,
            masterKeyAlias: String,
            context: Context,
            keyScheme: EncryptedSharedPreferences.PrefKeyEncryptionScheme,
            valueScheme: EncryptedSharedPreferences.PrefValueEncryptionScheme,
        ): SharedPreferences = context.getSharedPreferences("transport-test-$fileName", Context.MODE_PRIVATE)
    }
}
