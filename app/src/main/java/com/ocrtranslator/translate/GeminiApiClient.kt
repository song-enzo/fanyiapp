package com.ocrtranslator.translate

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiApiClient(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun translate(rawText: String): Result<String> {
        if (apiKey.isBlank()) return Result.failure(Exception("API Key 未配置，请前往设置填写"))
        if (rawText.isBlank()) return Result.failure(Exception("当前画面未检测到外文文本"))

        val prompt = """
            将以下内容中所有非中文文字翻译为标准简体中文，中文部分原样保留，不要添加任何解释：

            $rawText
        """.trimIndent()

        val body = gson.toJson(
            mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt)))))
        )

        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/v1beta/models/$model:generateContent?key=$apiKey")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 400 -> Result.failure(Exception("请求格式异常，请检查模型名称或接口地址"))
                    response.code == 401 || response.code == 403 -> Result.failure(Exception("API Key 无效，请检查密钥"))
                    response.code == 429 -> Result.failure(Exception("请求过于频繁，请稍后重试"))
                    !response.isSuccessful -> Result.failure(Exception("接口异常（${response.code}），请检查网络"))
                    else -> parseText(response.body?.string().orEmpty())
                }
            }
        } catch (_: Exception) {
            Result.failure(Exception("网络连接失败，请检查网络后重试"))
        }
    }

    private fun parseText(jsonText: String): Result<String> = try {
        val json = gson.fromJson(jsonText, Map::class.java)
        val candidate = (json["candidates"] as List<*>).first() as Map<*, *>
        val content = candidate["content"] as Map<*, *>
        val part = (content["parts"] as List<*>).first() as Map<*, *>
        Result.success((part["text"] as? String).orEmpty().trim())
    } catch (_: Exception) {
        Result.failure(Exception("接口返回解析失败，请稍后重试"))
    }
}
