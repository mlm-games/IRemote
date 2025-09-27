package app.iremote.ir.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object LastUsedStore {
    private const val PREF = "remote_tile"
    private const val KEY = "last_key_id"

    fun markLastKeyId(ctx: Context, id: Long) {
        prefs(ctx).edit { putLong(KEY, id) }
    }

    fun lastKeyId(ctx: Context): Long = prefs(ctx).getLong(KEY, -1L)

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
}