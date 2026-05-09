package com.ocrtranslator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ocrtranslator.service.FloatingBarService
import com.ocrtranslator.service.ScreenCaptureSession
import com.ocrtranslator.utils.PermissionHelper

class MainActivity : AppCompatActivity() {
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureSession.resultCode = result.resultCode
            ScreenCaptureSession.resultData = result.data
            Toast.makeText(this, "截屏授权已就绪", Toast.LENGTH_SHORT).show()
            startFloatingService()
        } else {
            Toast.makeText(this, "需要截屏授权才能 OCR 翻译", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        if (PermissionHelper.canDrawOverlays(this) && ScreenCaptureSession.isReady) {
            startFloatingService()
        }
    }

    private fun buildContent(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32.dp, 32.dp, 32.dp, 32.dp)
            setBackgroundColor(0xFF101114.toInt())
        }

        root.addView(TextView(this).apply {
            text = "悬浮 OCR 翻译"
            textSize = 28f
            setTextColor(0xFFF4F6FA.toInt())
            gravity = Gravity.CENTER
        })

        root.addView(TextView(this).apply {
            text = "授权悬浮窗和屏幕捕获后，侧边悬浮条会常驻显示。点击悬浮条即可截屏、OCR 并翻译为简体中文。"
            textSize = 15f
            setTextColor(0xFFA9B0BC.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18.dp, 0, 28.dp)
        })

        root.addView(button("1. 开启悬浮窗权限") { PermissionHelper.openOverlaySettings(this) })
        root.addView(button("2. 授权屏幕捕获") { requestCapture() })
        root.addView(button("启动悬浮翻译") { startFloatingService() })
        root.addView(button("设置") { startActivity(Intent(this, SettingsActivity::class.java)) })
        root.addView(button("历史记录") { startActivity(Intent(this, HistoryActivity::class.java)) })
        return root
    }

    private fun button(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }

    private fun requestCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun startFloatingService() {
        if (!PermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(this, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
            return
        }
        startService(Intent(this, FloatingBarService::class.java))
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
