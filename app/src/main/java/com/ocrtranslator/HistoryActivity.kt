package com.ocrtranslator

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ocrtranslator.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private val scope = MainScope()
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18.dp, 18.dp, 18.dp, 18.dp) }
        setContentView(ScrollView(this).apply { setBackgroundColor(0xFF101114.toInt()); addView(list) })
        load()
    }

    private fun load() {
        scope.launch {
            val dao = AppDatabase.get(this@HistoryActivity).historyDao()
            val items = withContext(Dispatchers.IO) { dao.latest() }
            list.removeAllViews()
            list.addView(Button(this@HistoryActivity).apply {
                text = "清空历史"
                setOnClickListener {
                    scope.launch { withContext(Dispatchers.IO) { dao.clear() }; load() }
                }
            })
            items.forEach { item ->
                list.addView(TextView(this@HistoryActivity).apply {
                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.createdAt))
                    text = "$time\n原文：${item.originalText}\n译文：${item.translatedText}"
                    textSize = 15f
                    setTextColor(0xFFF4F6FA.toInt())
                    setPadding(0, 16.dp, 0, 16.dp)
                })
            }
        }
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
