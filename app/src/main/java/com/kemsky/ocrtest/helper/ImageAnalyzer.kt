package com.kemsky.ocrtest.helper

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File

class ImageAnalyzer : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    var text = ""

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        mediaImage?.let {
            val images = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            recognizer.process(images)
                .addOnSuccessListener { visionText ->
                    val resultText = visionText.text
                    for(block in visionText.textBlocks) {
                        val blockText = block.text
//                        val blockCornerPoints = block.cornerPoints
//                        val blockFrame = block.boundingBox

                        Log.d("ImageAnalyzer", "Block text: $blockText")

//                        for(line in block.lines) {
//                            val lineText = line.text
//                            val lineCornerPoints = line.cornerPoints
//                            val lineFrame = line.boundingBox

//                            for(element in line.elements) {
//                                val elementText = element.text
//                                val elementCornerPoints = element.cornerPoints
//                                val elementFrame = element.boundingBox
//                            }
//                        }
                    }
                    text = resultText
                    Log.d("ImageAnalyzer", "Text recognition result: $resultText")
                    image.close()
                }
                .addOnFailureListener { e ->
                    Log.e("ImageAnalyzer", "Text recognition error", e)
                }
        }
    }

    fun analyzeFromImage(context: Context, file: Uri) {
        val images = InputImage.fromFilePath(context, file)
        recognizer.process(images)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                for(block in visionText.textBlocks) {
                    val blockText = block.text
//                        val blockCornerPoints = block.cornerPoints
//                        val blockFrame = block.boundingBox

                    Log.d("ImageAnalyzer", "Block text: $blockText")

//                        for(line in block.lines) {
//                            val lineText = line.text
//                            val lineCornerPoints = line.cornerPoints
//                            val lineFrame = line.boundingBox

//                            for(element in line.elements) {
//                                val elementText = element.text
//                                val elementCornerPoints = element.cornerPoints
//                                val elementFrame = element.boundingBox
//                            }
//                        }
                }
                text = resultText
                Log.d("ImageAnalyzer", "Text recognition result: $resultText")
            }
            .addOnFailureListener { e ->
                Log.e("ImageAnalyzer", "Text recognition error", e)
            }
    }

}