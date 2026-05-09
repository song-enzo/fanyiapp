package com.ocrtranslator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.ocrtranslator.R
import com.ocrtranslator.utils.PreferenceManager

class FloatingBarService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var lastClickAt = 0L

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notification())
        try {
            showFloatingBar()
        } catch (e: Exception) {
            Toast.makeText(this, "悬浮条启动失败，请检查悬浮窗权限", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatingView == null) showFloatingBar()
        return START_STICKY
    }

    private fun showFloatingBar() {
        val prefs = PreferenceManager(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = FrameLayout(this).apply {
            setBackgroundColor(Color.argb((255 * prefs.opacity).toInt(), 61, 220, 132))
            setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastClickAt < 420) triggerTranslate()
                else triggerTranslate()
                lastClickAt = now
            }
            setOnLongClickListener {
                triggerTranslate()
                true
            }
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            12.dp,
            96.dp,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (prefs.isLeft) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
        }

        windowManager?.addView(view, params)
        floatingView = view
    }

    private fun triggerTranslate() {
        if (!ScreenCaptureSession.isReady) {
            Toast.makeText(this, "请先打开 APP 重新授权屏幕捕获", Toast.LENGTH_LONG).show()
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, ScreenCaptureService::class.java))
    }

    private fun notification(): Notification {
        val channelId = "floating_bar"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "悬浮翻译服务", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            return Notification.Builder(this, channelId)
                .setContentTitle("OCR 翻译运行中")
                .setContentText("点击侧边悬浮条开始翻译")
                .setSmallIcon(R.drawable.ic_translate)
                .build()
        }
        @Suppress("DEPRECATION")
        return Notification.Builder(this)
            .setContentTitle("OCR 翻译运行中")
            .setContentText("点击侧边悬浮条开始翻译")
            .setSmallIcon(R.drawable.ic_translate)
            .build()
    }

    override fun onDestroy() {
        floatingView?.let { windowManager?.removeView(it) }
        floatingView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
