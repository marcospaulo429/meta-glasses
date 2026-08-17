package com.prontuario.glasses.config

import android.content.Context

/** Identificação do médico exigida no atestado (nome/CRM); salva local, nunca sai do aparelho. */
object DoctorProfile {

    private const val PREFS = "doctor_profile"

    fun name(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("name", null)

    fun crm(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("crm", null)

    fun save(context: Context, name: String, crm: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("name", name).putString("crm", crm).apply()
    }
}
