package com.ocrtranslator.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlKitOcrHelper {
    suspend fun recognize(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizers = listOf(
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
            TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        )

        val chunks = recognizers.mapNotNull { recognizer ->
            try {
                suspendCoroutine<String> { cont ->
                    recognizer.process(image)
                        .addOnSuccessListener { cont.resume(it.text) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                }
            } catch (_: Exception) {
                null
            } finally {
                recognizer.close()
            }
        }

        return chunks
            .flatMap { it.lines() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n")
    }
}
