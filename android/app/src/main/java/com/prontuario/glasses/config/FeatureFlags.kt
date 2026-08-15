package com.prontuario.glasses.config

import android.content.Context
import com.prontuario.glasses.BuildConfig

object FeatureFlags {

    private const val PREFS = "feature_flags"
    private const val KEY_SECURITY_VIDEO = "security_video_enabled"

    /** EM DISPUTA (MEMORY.md §4.1): padrão OFF até decisão do time; parecer LGPD é ⛔. */
    fun securityVideoEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SECURITY_VIDEO, BuildConfig.SECURITY_VIDEO_DEFAULT)

    fun setSecurityVideoEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SECURITY_VIDEO, enabled).apply()
    }
}
