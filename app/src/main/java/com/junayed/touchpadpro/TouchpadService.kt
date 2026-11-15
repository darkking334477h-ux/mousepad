package com.junayed.touchpadpro

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Path
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.hypot

class TouchpadService : AccessibilityService() {
    private lateinit var wm: WindowManager
    private var pad: PadView? = null
    private var cursor: CursorView? = null
    private var stop: TextView? = null
    private var cursorX = 0f
    private var cursorY = 0f
    private val handler = Handler(Looper.getMainLooper())
    private var pendingClick: Runnable? = null
    private var lastTapAt = 0L
    private var downX = 0f
    private var downY = 0f
    private var startCursorX = 0f
    private var startCursorY = 0f
    private var moved = false
    private var twoFinger = false
    private var lastTwoY = 0f
    private var screenW = 1
    private var screenH = 1

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = resources.displayMetrics
        screenW = metrics.widthPixels
        screenH = metrics.heightPixels
        cursorX = screenW / 2f
        cursorY = screenH / 2f
        showOverlay()
    }

    private fun overlayParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun showOverlay() {
        if (pad != null) return
        pad = PadView().also { wm.addView(it, overlayParams()) }
        cursor = CursorView().also {
            val p = WindowManager.LayoutParams(54, 54,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (cursorX - 27).toInt()
                y = (cursorY - 27).toInt()
            }
            wm.addView(it, p)
        }
        stop = TextView(this).apply {
            text = " STOP "
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xDDC62828.toInt())
            setPadding(18, 12, 18, 12)
            setOnClickListener { removeOverlay() }
        }.also {
            val p = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 12; y = 40
            }
            wm.addView(it, p)
        }
    }

    private fun removeOverlay() {
        pendingClick?.let { handler.removeCallbacks(it) }
        listOf(pad, cursor, stop).forEach { v ->
            if (v != null) try { wm.removeView(v) } catch (_: Exception) {}
        }
        pad = null; cursor = null; stop = null
    }

    private fun updateCursor(dx: Float, dy: Float) {
        cursorX = (cursorX + dx * 1.65f).coerceIn(12f, screenW - 12f)
        cursorY = (cursorY + dy * 1.65f).coerceIn(12f, screenH - 12f)
        val v = cursor ?: return
        val p = v.layoutParams as WindowManager.LayoutParams
        p.x = (cursorX - 27).toInt(); p.y = (cursorY - 27).toInt()
        wm.updateViewLayout(v, p)
    }

    private fun sendTap(x: Float, y: Float, count: Int = 1) {
        val b = GestureDescription.Builder()
        repeat(count) { i ->
            val path = Path().apply { moveTo(x, y); lineTo(x + 1, y + 1) }
            b.addStroke(GestureDescription.StrokeDescription(path, i * 110L, 60L))
        }
        dispatchGesture(b.build(), null, null)
    }

    private fun sendSwipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 280L) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration)).build()
        dispatchGesture(gesture, null, null)
    }

    private inner class PadView : View(this@TouchpadService) {
        private val bg = Paint().apply { color = 0x1800DCC8 }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(0x08000000)
            val p = Paint().apply { color = 0x5580EDE0; textSize = 34f; textAlign = Paint.Align.CENTER }
            canvas.drawText("TOUCHPAD PRO", width / 2f, height / 2f, p)
            p.textSize = 18f
            canvas.drawText("Move • Tap • Double tap • Two-finger scroll", width / 2f, height / 2f + 34f, p)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.x; downY = e.y
                    startCursorX = cursorX; startCursorY = cursorY
                    moved = false; twoFinger = false
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (e.pointerCount >= 2) {
                        twoFinger = true
                        lastTwoY = (e.getY(0) + e.getY(1)) / 2f
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (twoFinger && e.pointerCount >= 2) {
                        val cy = (e.getY(0) + e.getY(1)) / 2f
                        val delta = cy - lastTwoY
                        lastTwoY = cy
                        if (abs(delta) > 1f) sendSwipe(cursorX, cursorY, cursorX, (cursorY - delta * 2f).coerceIn(0f, screenH.toFloat()), 180)
                    } else if (e.pointerCount == 1) {
                        val dx = e.x - downX; val dy = e.y - downY
                        if (hypot(dx, dy) > 8f) moved = true
                        updateCursor(dx, dy)
                        downX = e.x; downY = e.y
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!twoFinger) {
                        if (moved) {
                            sendSwipe(startCursorX, startCursorY, cursorX, cursorY, 350)
                        } else {
                            val now = System.currentTimeMillis()
                            if (now - lastTapAt < 300L) {
                                pendingClick?.let { handler.removeCallbacks(it) }
                                pendingClick = null
                                sendTap(cursorX, cursorY, 2)
                                lastTapAt = 0L
                            } else {
                                lastTapAt = now
                                val x = cursorX; val y = cursorY
                                val task = Runnable { sendTap(x, y); pendingClick = null }
                                pendingClick = task
                                handler.postDelayed(task, 300L)
                            }
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> return true
            }
            return true
        }
    }

    private inner class CursorView : View(this@TouchpadService) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF00E5D0.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawCircle(27f, 27f, 17f, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(27f, 27f, 4f, paint)
            paint.style = Paint.Style.STROKE
        }
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() { removeOverlay() }
    override fun onDestroy() { removeOverlay(); super.onDestroy() }
}
