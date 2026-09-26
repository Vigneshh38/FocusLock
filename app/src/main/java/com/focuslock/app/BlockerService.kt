package com.focuslock.app

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

/**
 * 1) Blocked app opens -> go home + turn screen off.
 * 2) Someone tries to uninstall / stop / switch off FocusLock from Settings,
 *    the installer or the launcher -> back out of that screen.
 *    Use "Uninstall" or "App settings" inside FocusLock (password) instead.
 */
class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastLockAt = 0L
    private var lastGuardCheck = 0L
    private var lastGuardAction = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val now = SystemClock.elapsedRealtime()

        // 1) blocked apps
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg in Store.blockedPkgs(this)
        ) {
            if (now - lastLockAt < 1000) return
            lastLockAt = now
            performGlobalAction(GLOBAL_ACTION_HOME)
            handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }, 150)
            return
        }

        // 2) self-protection
        if (!isGuardedPackage(pkg) || Store.protectionPaused(this)) return
        if (now - lastGuardCheck < 300) return
        lastGuardCheck = now
        if (threatensFocusLock(pkg) && now - lastGuardAction > 1500) {
            lastGuardAction = now
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 120)
            Toast.makeText(
                this,
                "FocusLock is protected. Use Uninstall / App settings inside FocusLock with your password.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isGuardedPackage(pkg: String) =
        pkg.contains("settings") || pkg.contains("packageinstaller") ||
            pkg.contains("launcher") || pkg.contains("safecenter") ||
            pkg.contains("securitycenter") || pkg.contains("permissioncontroller")

    private fun threatensFocusLock(pkg: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return try {
            val mentions = root.findAccessibilityNodeInfosByText("FocusLock")
            if (mentions.isEmpty()) return false

            if (pkg.contains("launcher")) {
                // only a launcher dialog like "Uninstall FocusLock?"
                return mentions.any { (it.text?.toString() ?: "").contains("uninstall", true) }
            }
            // accessibility page for our service
            if (mentions.any { (it.text?.toString() ?: "").contains("blocker", true) }) return true
            // app info page / uninstall dialog
            DANGER_WORDS.any { root.findAccessibilityNodeInfosByText(it).isNotEmpty() }
        } catch (e: Exception) {
            false
        }
    }

    override fun onInterrupt() {}

    companion object {
        private val DANGER_WORDS = listOf("Uninstall", "Force stop", "Deactivate", "Disable")
    }
}
