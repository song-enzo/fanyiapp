package com.ocrtranslator.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.ocrtranslator.R
import com.ocrtranslator.TranslationActivity
import com.ocrtranslator.data.AppDatabase
import com.ocrtranslator.data.HistoryEntity
import com.ocrtranslator.ocr.MlKitOcrHelper
import com.ocrtranslator.translate.GeminiApiClient
import com.ocrtranslator.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenCaptureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(2, notification())
        scope.launch { runCaptureFlow() }
        return START_NOT_STICKY
    }

    private suspend fun runCaptureFlow() {
        val sessionData = ScreenCaptureSession.resultData
        if (sessionData == null) {
            openResult("", "请先打开 APP 并授权屏幕捕获")
            stopSelf()
            return
        }

        val bitmap = captureBitmap(ScreenCaptureSession.resultCode, sessionData)
        if (bitmap == null) {
            openResult("", "截屏失败，请重新授权屏幕捕获")
            stopSelf()
            return
        }

        val prefs = PreferenceManager(this)
        val result = withContext(Dispatchers.IO) {
            val original = MlKitOcrHelper().recognize(bitmap)
            bitmap.recycle()
            if (original.isBlank()) {
                original to "当前画面未检测到外文文本"
            } else {
                val translated = GeminiApiClient(prefs.apiKey, prefs.baseUrl, prefs.model)
                    .translate(original)
                    .getOrElse { it.message ?: "翻译失败，请稍后重试" }
                val dao = AppDatabase.get(this@ScreenCaptureService).historyDao()
                dao.insert(HistoryEntity(originalText = original, translatedText = translated))
                dao.trimToLimit()
                original to translated
            }
        }

        openResult(result.first, result.second)
        stopSelf()
    }

    private suspend fun captureBitmap(resultCode: Int, data: Intent): Bitmap? = withContext(Dispatchers.Main) {
        val projection = projection(resultCode, data) ?: return@withContext null
        projection.registerCallback(object : MediaProjection.Callback() {}, Handler(Looper.getMainLooper()))
        val metrics = displayMetrics()
        val reader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        val display = projection.createVirtualDisplay(
            "ocr-translator-capture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            null
        )

        delay(650)
        val image = reader.acquireLatestImage()
        val bitmap = image?.let {
            val plane = it.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * metrics.widthPixels
            val padded = Bitmap.createBitmap(
                metrics.widthPixels + rowPadding / pixelStride,
                metrics.heightPixels,
                Bitmap.Config.ARGB_8888
            )
            padded.copyPixelsFromBuffer(buffer)
            Bitmap.createBitmap(padded, 0, 0, metrics.widthPixels, metrics.heightPixels).also {
                padded.recycle()
            }
        }
        image?.close()
        display.release()
        reader.close()
        projection.stop()
        bitmap
    }

    private fun projection(resultCode: Int, data: Intent): MediaProjection? {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        return manager.getMediaProjection(resultCode, data)
    }

    private fun displayMetrics(): DisplayMetrics {
        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return metrics
    }

    private fun openResult(original: String, translated: String) {
        val intent = Intent(this, TranslationActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(TranslationActivity.EXTRA_ORIGINAL, original)
            .putExtra(TranslationActivity.EXTRA_TRANSLATED, translated)
        startActivity(intent)
    }

    private fun notification(): Notification {
        val channelId = "screen_capture"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "屏幕 OCR 翻译", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            return Notification.Builder(this, channelId)
                .setContentTitle("正在识别并翻译屏幕")
                .setSmallIcon(R.drawable.ic_translate)
                .build()
        }
        @Suppress("DEPRECATION")
        return Notification.Builder(this)
            .setContentTitle("正在识别并翻译屏幕")
            .setSmallIcon(R.drawable.ic_translate)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
