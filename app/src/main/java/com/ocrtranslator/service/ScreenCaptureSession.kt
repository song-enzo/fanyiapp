package com.ocrtranslator.service

import android.content.Intent

object ScreenCaptureSession {
    var resultCode: Int = 0
    var resultData: Intent? = null

    val isReady: Boolean
        get() = resultData != null
}
