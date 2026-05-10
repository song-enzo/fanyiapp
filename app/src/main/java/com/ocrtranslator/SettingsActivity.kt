package com.ocrtranslator

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ocrtranslator.translate.GeminiApiClient
import com.ocrtranslator.utils.PermissionHelper
import com.ocrtranslator.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModelPreset(val label: String, val model: String, val baseUrl: String)

class SettingsActivity : AppCompatActivity() {
    private val scope = MainScope()

    private val presets = listOf(
        ModelPreset("Gemini 2.5 Flash", "gemini-2.5-flash", "https://generativelanguage.googleapis.com"),
        ModelPreset("Gemini 2.5 Pro", "gemini-2.5-pro-exp-03-25", "https://generativelanguage.googleapis.com"),
        ModelPreset("Kimi (Moonshot)", "moonshot-v1-8k", "https://api.moonshot.cn/v1"),
        ModelPreset("DeepSeek", "deepseek-chat", "https://api.deepseek.com"),
        ModelPreset("通义千问 (Qwen)", "qwen-turbo-latest", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
        ModelPreset("GLM-4 (智谱)", "glm-4-flash", "https://open.bigmodel.cn/api/paas/v4"),
        ModelPreset("自定义", "", ""),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferenceManager(this)

        val apiKey = input("API Key", prefs.apiKey)
        val baseUrl = input("接口地址", prefs.baseUrl)
        val model = input("模型名称", prefs.model)

        // 模型预设选择器
        val presetGroup = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        var selectedIndex = presets.indexOfFirst { it.model == prefs.model && it.baseUrl == prefs.baseUrl }
        if (selectedIndex < 0) selectedIndex = presets.size - 1 // 自定义

        presets.forEachIndexed { i, p ->
            val rb = RadioButton(this).apply {
                text = p.label
                setTextColor(0xFFF4F6FA.toInt())
                isChecked = i == selectedIndex
                setOnClickListener {
                    if (p.label != "自定义") {
                        model.setText(p.model)
                        baseUrl.setText(p.baseUrl)
                    }
                }
            }
            presetGroup.addView(rb)
        }

        val left = CheckBox(this).apply { text = "悬浮条显示在左侧"; isChecked = prefs.isLeft; setTextColor(0xFFF4F6FA.toInt()) }
        val bilingual = CheckBox(this).apply { text = "双语对照显示"; isChecked = prefs.bilingual; setTextColor(0xFFF4F6FA.toInt()) }
        val opacity = SeekBar(this).apply { max = 80; progress = ((prefs.opacity - 0.2f) * 100).toInt() }
        val textSize = SeekBar(this).apply { max = 20; progress = (prefs.textSize - 16f).toInt() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 24.dp, 24.dp, 24.dp)
            setBackgroundColor(0xFF101114.toInt())
            addView(title("设置"))
            addView(label("选择模型"))
            addView(presetGroup)
            addView(label("API Key"))
            addView(apiKey)
            addView(label("接口地址"))
            addView(baseUrl)
            addView(label("模型名称"))
            addView(model)
            addView(label("悬浮条透明度"))
            addView(opacity)
            addView(label("译文字体大小"))
            addView(textSize)
            addView(left)
            addView(bilingual)
            addView(button("保存") {
                prefs.apiKey = apiKey.text.toString()
                prefs.baseUrl = baseUrl.text.toString()
                prefs.model = model.text.toString()
                prefs.isLeft = left.isChecked
                prefs.bilingual = bilingual.isChecked
                prefs.opacity = 0.2f + opacity.progress / 100f
                prefs.textSize = 16f + textSize.progress
                Toast.makeText(this@SettingsActivity, "已保存", Toast.LENGTH_SHORT).show()
            })
            addView(button("测试配置") { testConfig(prefs, apiKey.text.toString(), baseUrl.text.toString(), model.text.toString()) })
            addView(button("电池优化设置") { PermissionHelper.openBatterySettings(this@SettingsActivity) })
        }
        setContentView(root)
    }

    private fun testConfig(prefs: PreferenceManager, key: String, url: String, model: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                GeminiApiClient(key, url.ifBlank { prefs.baseUrl }, model.ifBlank { prefs.model }).translate("Hello")
            }
            Toast.makeText(
                this@SettingsActivity,
                result.getOrElse { it.message ?: "测试失败" },
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun title(text: String) = label(text).apply { textSize = 26f; gravity = Gravity.CENTER }
    private fun label(text: String) = TextView(this).apply { this.text = text; textSize = 14f; setTextColor(0xFFF4F6FA.toInt()); setPadding(0, 14.dp, 0, 6.dp) }
    private fun input(hint: String, value: String) = EditText(this).apply { this.hint = hint; setText(value); setTextColor(0xFFF4F6FA.toInt()); setHintTextColor(0xFFA9B0BC.toInt()) }
    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() } }
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
