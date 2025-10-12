package com.hika.accessibility.recognition.means.ocr

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognition

class GoogleOCRer {
    // When using Chinese script library
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    fun initiateNV21Recognition(frameBuffer: ByteArray, width: Int, height: Int)
        = recognizer.process(
            InputImage.fromByteArray(frameBuffer,
            width,
            height,
            0,
            InputImage.IMAGE_FORMAT_NV21)
        )
}