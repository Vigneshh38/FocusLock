package com.focuslock.app

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

/** Blocked app list + PIN hashes (PINs are never stored in plain text). */
object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("focuslock", Context.MODE_PRIVATE)

    fun blocked(c: Context): Set<String> =
        prefs(c).getStringSet("blocked", emptySet())?.toSet() ?: emptySet()

    fun setBlocked(c: Context, pkgs: Set<String>) {
        prefs(c).edit().putStringSet("blocked", HashSet(pkgs)).apply()
    }

    private fun pinHashes(c: Context): List<String> =
        (prefs(c).getString("pins", "") ?: "").split(",").filter { it.isNotEmpty() }

    fun hasPins(c: Context) = pinHashes(c).isNotEmpty()
    fun pinCount(c: Context) = pinHashes(c).size

    fun savePins(c: Context, pins: List<String>) {
        val salt = UUID.randomUUID().toString()
        prefs(c).edit()
            .putString("salt", salt)
            .putString("pins", pins.joinToString(",") { hash(salt, it) })
            .commit()
    }

    /** PINs must be entered in order: PIN 1, then PIN 2, ... */
    fun checkPin(c: Context, index: Int, pin: String): Boolean {
        val salt = prefs(c).getString("salt", "") ?: ""
        return pinHashes(c).getOrNull(index) == hash(salt, pin)
    }

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }
}
