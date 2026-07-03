package com.example.remotecontrol

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import org.json.JSONObject

/**
 * الخدمة دي هي اللي بتنفذ فعليًا أوامر التحكم اللي جاية من جهاز التحكم.
 * لازم المستخدم يفعّلها يدويًا من الإعدادات مرة واحدة (Settings > Accessibility)
 * - ده إجراء أمان من أندرويد نفسه، مينفعش يتعدى أوتوماتيك.
 *
 * الأوامر المدعومة:
 *  - tap:   {"action":"tap","x":0.5,"y":0.5}       (إحداثيات نسبية من 0 إلى 1)
 *  - swipe: {"action":"swipe","from":{...},"to":{...}}
 *  - text:  {"action":"text","value":"..."}
 */
class RemoteControlAccessibilityService : AccessibilityService() {

    companion object {
        var instance: RemoteControlAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun executeCommand(command: JSONObject) {
        val metrics: DisplayMetrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        when (command.getString("action")) {
            "tap" -> {
                val x = command.getDouble("x") * screenWidth
                val y = command.getDouble("y") * screenHeight
                dispatchTap(x.toFloat(), y.toFloat())
            }
            "swipe" -> {
                val from = command.getJSONObject("from")
                val to = command.getJSONObject("to")
                dispatchSwipe(
                    (from.getDouble("x") * screenWidth).toFloat(),
                    (from.getDouble("y") * screenHeight).toFloat(),
                    (to.getDouble("x") * screenWidth).toFloat(),
                    (to.getDouble("y") * screenHeight).toFloat()
                )
            }
            "text" -> {
                // لكتابة نص في حقل مركّز عليه، عادة أسهل حاجة إنك تستخدم
                // ACTION_SET_TEXT على الـ AccessibilityNodeInfo الحالي:
                rootInActiveWindow?.findFocus(AccessibilityNodeInfoCompatFocus())
                    ?.let { /* implement setText via node.performAction(...) */ }
            }
        }
    }

    private fun dispatchTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchSwipe(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}

// ملحوظة: دي مجرد إشارة placeholder - استبدلها بمنطق حقيقي للبحث عن
// العنصر اللي عليه focus وكتابة نص فيه، أو استخدم AccessibilityNodeInfo.ACTION_SET_TEXT
private fun AccessibilityService.rootInActiveWindowSafe() = this.rootInActiveWindow
private class AccessibilityNodeInfoCompatFocus
