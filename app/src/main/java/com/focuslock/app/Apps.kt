package com.focuslock.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(val label: String, val pkg: String, val icon: Drawable?)

object Apps {
    /** All apps that appear in the phone's app drawer (except FocusLock itself). */
    fun launchable(c: Context): List<AppInfo> {
        val pm = c.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return pm.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != c.packageName }
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
            .sortedBy { it.label.lowercase() }
    }

    fun info(c: Context, pkg: String): AppInfo {
        val pm = c.packageManager
        return try {
            @Suppress("DEPRECATION")
            val ai = pm.getApplicationInfo(pkg, 0)
            AppInfo(pm.getApplicationLabel(ai).toString(), pkg, pm.getApplicationIcon(ai))
        } catch (e: PackageManager.NameNotFoundException) {
            AppInfo(pkg, pkg, null) // app was uninstalled
        }
    }
}
