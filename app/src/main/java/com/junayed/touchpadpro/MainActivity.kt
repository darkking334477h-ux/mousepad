package com.junayed.touchpadpro

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 36, 28, 36)
            setBackgroundColor(0xFF101820.toInt())
        }
        val title = TextView(this).apply {
            text = "TOUCHPAD PRO"
            textSize = 28f
            setTextColor(0xFF55E6D1.toInt())
            gravity = Gravity.CENTER
        }
        val info = TextView(this).apply {
            text = "Enable the accessibility service to start the floating touchpad.\n\n• Move one finger: move cursor\n• Tap once: click\n• Tap twice: double-click\n• Drag: hold and move\n• Two fingers: scroll\n\nA floating STOP button closes the touchpad."
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 28)
        }
        val enable = Button(this).apply {
            text = "OPEN ACCESSIBILITY SETTINGS"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        layout.addView(title)
        layout.addView(info)
        layout.addView(enable)
        setContentView(layout)
    }
}
