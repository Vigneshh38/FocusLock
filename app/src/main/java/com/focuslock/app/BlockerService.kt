package com.focuslock.app

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Watches which app comes to the foreground. If it's on the block list:
 * 1) sends you to the home screen (so the app isn't there after you unlock),
 * 2) turns off the display / locks the phone.
 */
class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastLockAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg !in Store.blocked(this)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastLockAt < 1000) return // ignore duplicate events
        lastLockAt = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }, 150)
    }

    override fun onInterrupt() {}
}
