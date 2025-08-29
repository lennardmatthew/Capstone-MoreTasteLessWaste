package prototype.one.mtlw.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun setRememberMe(rememberMe: Boolean) {
        sharedPreferences.edit { putBoolean(REMEMBER_ME_KEY, rememberMe) }
    }

    fun getRememberMe(): Boolean {
        return sharedPreferences.getBoolean(REMEMBER_ME_KEY, false)
    }

    // Keys for failed login attempts
    private fun getFailedAttemptsKey(email: String): String = "failed_attempts_${email}"
    private fun getLockoutTimestampKey(email: String): String = "lockout_timestamp_${email}"

    // Store failed login attempts for a specific email
    fun setFailedAttempts(email: String, count: Int) {
        sharedPreferences.edit { putInt(getFailedAttemptsKey(email), count) }
    }

    // Retrieve failed login attempts for a specific email
    fun getFailedAttempts(email: String): Int {
        return sharedPreferences.getInt(getFailedAttemptsKey(email), 0)
    }

    // Store lockout timestamp for a specific email
    fun setLockoutTimestamp(email: String, timestamp: Long) {
        sharedPreferences.edit { putLong(getLockoutTimestampKey(email), timestamp) }
    }

    // Retrieve lockout timestamp for a specific email
    fun getLockoutTimestamp(email: String): Long {
        return sharedPreferences.getLong(getLockoutTimestampKey(email), 0L)
    }

    // Clear failed attempts and lockout timestamp for a specific email
    fun clearLoginAttempts(email: String) {
        sharedPreferences.edit()
            .remove(getFailedAttemptsKey(email))
            .remove(getLockoutTimestampKey(email))
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "mtlw_preferences"
        private const val REMEMBER_ME_KEY = "remember_me"
    }
} 