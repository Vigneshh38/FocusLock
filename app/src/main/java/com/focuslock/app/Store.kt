package com.focuslock.app

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

data class Rule(val pkg: String, val method: String, val amount: Int)

object Store {
    const val PUSHUPS = "pushups"
    const val PARAGRAPH = "paragraph"

    private fun prefs(c: Context) = c.getSharedPreferences("focuslock", Context.MODE_PRIVATE)

    // ---------- block rules: "pkg|method|amount" ----------

    fun rules(c: Context): List<Rule> =
        (prefs(c).getStringSet("rules", emptySet()) ?: emptySet()).mapNotNull { line ->
            val p = line.split("|")
            if (p.size == 3) p[2].toIntOrNull()?.let { Rule(p[0], p[1], it) } else null
        }

    fun blockedPkgs(c: Context): Set<String> = rules(c).map { it.pkg }.toSet()

    fun rule(c: Context, pkg: String): Rule? = rules(c).firstOrNull { it.pkg == pkg }

    fun addRules(c: Context, pkgs: Collection<String>, method: String, amount: Int) {
        val keep = rules(c).filter { it.pkg !in pkgs }
        val all = keep + pkgs.map { Rule(it, method, amount) }
        save(c, all)
    }

    fun removeRule(c: Context, pkg: String) = save(c, rules(c).filter { it.pkg != pkg })

    private fun save(c: Context, rules: List<Rule>) {
        prefs(c).edit()
            .putStringSet("rules", rules.map { "${it.pkg}|${it.method}|${it.amount}" }.toHashSet())
            .commit()
    }

    // ---------- password (for uninstalling / settings) ----------

    fun hasPassword(c: Context) = !prefs(c).getString("pw", null).isNullOrEmpty()

    fun setPassword(c: Context, pw: String) {
        val salt = UUID.randomUUID().toString()
        prefs(c).edit().putString("salt", salt).putString("pw", hash(salt, pw)).commit()
    }

    fun checkPassword(c: Context, pw: String): Boolean {
        val salt = prefs(c).getString("salt", "") ?: ""
        return prefs(c).getString("pw", null) == hash(salt, pw)
    }

    private fun hash(salt: String, s: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + s).toByteArray())
            .joinToString("") { "%02x".format(it) }

    // ---------- short protection pause after entering the password ----------

    fun pauseProtection(c: Context, ms: Long) {
        prefs(c).edit().putLong("pauseUntil", System.currentTimeMillis() + ms).commit()
    }

    fun protectionPaused(c: Context) = System.currentTimeMillis() < prefs(c).getLong("pauseUntil", 0)
}
