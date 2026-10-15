package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PowerShellAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PS_Accessibility"
        var instance: PowerShellAccessibilityService? = null
            private set

        val isRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "PowerShell Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events monitored
    }

    override fun onInterrupt() {
        Log.w(TAG, "PowerShell Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "PowerShell Accessibility Service destroyed")
    }

    /**
     * Real screen touch / tap at exact (x, y) coordinates
     */
    fun tap(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                onComplete?.invoke(false)
            }
        }, null)
    }

    /**
     * Real screen swipe from (startX, startY) to (endX, endY)
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300, onComplete: ((Boolean) -> Unit)? = null): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(50))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                onComplete?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                onComplete?.invoke(false)
            }
        }, null)
    }

    /**
     * Click on a visible UI element by exact or partial text or view ID
     */
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            for (node in nodes) {
                if (node.isClickable) {
                    val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node.recycle()
                    return clicked
                } else {
                    // Try clicking parent
                    var parent = node.parent
                    while (parent != null) {
                        if (parent.isClickable) {
                            val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            parent.recycle()
                            node.recycle()
                            return clicked
                        }
                        parent = parent.parent
                    }
                }
                node.recycle()
            }
        }
        return false
    }

    /**
     * Dumps the visible text & view hierarchy of the active screen
     */
    fun dumpScreen(): String {
        val root = rootInActiveWindow ?: return "[No active window accessible]"
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
            if (node == null) return
            val indent = "  ".repeat(depth)
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
            val id = node.viewIdResourceName ?: ""
            val cls = node.className?.toString()?.substringAfterLast('.') ?: ""
            val clickable = if (node.isClickable) "[CLICKABLE]" else ""
            if (text.isNotBlank() || clickable.isNotBlank() || id.isNotBlank()) {
                sb.appendLine("$indent- $cls ($id) bounds=[${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}] $clickable : \"$text\"")
            }
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i), depth + 1)
            }
        }
        traverse(root, 0)
        return sb.toString()
    }
}
