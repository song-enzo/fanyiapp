package com.ocrtranslator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.ocrtranslator.service.ScreenCaptureService
import com.ocrtranslator.utils.PreferenceManager

class TranslationActivity : AppCompatActivity() {
    private lateinit var translated: String
    private lateinit var original: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()
        original = intent.getStringExtra(EXTRA_ORIGINAL).orEmpty()
        translated = intent.getStringExtra(EXTRA_TRANSLATED).orEmpty()
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val prefs = PreferenceManager(this)
        val result = TextView(this).apply {
            text = translated.ifBlank { "暂无翻译结果" }
            textSize = prefs.textSize
            setTextColor(0xFFF4F6FA.toInt())
            setPadding(20.dp, 20.dp, 20.dp, 20.dp)
        }
        val source = TextView(this).apply {
            text = original
            textSize = 14f
            setTextColor(0xFFA9B0BC.toInt())
            visibility = if (prefs.bilingual && original.isNotBlank()) View.VISIBLE else View.GONE
            setPadding(20.dp, 12.dp, 20.dp, 8.dp)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF101114.toInt())
            addView(ScrollView(this@TranslationActivity).apply {
                addView(LinearLayout(this@TranslationActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(source)
                    addView(result)
                })
            }, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(LinearLayout(this@TranslationActivity).apply {
                gravity = Gravity.CENTER
                addView(button("重译") {
                    ContextCompat.startForegroundService(
                        this@TranslationActivity,
                        Intent(this@TranslationActivity, ScreenCaptureService::class.java)
                    )
                })
                addView(button("复制") { copyText() })
                addView(button("清空") { result.text = ""; source.text = "" })
                addView(button("关闭") { finish() })
            })
        }
    }

    private fun copyText() {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("translation", translated))
        Toast.makeText(this, "已复制译文", Toast.LENGTH_SHORT).show()
    }

    private fun hideSystemUi() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() } }
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_ORIGINAL = "original"
        const val EXTRA_TRANSLATED = "translated"
    }
}
