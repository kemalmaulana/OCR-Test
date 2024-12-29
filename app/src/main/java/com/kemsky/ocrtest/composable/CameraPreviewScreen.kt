package com.kemsky.ocrtest.composable

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.kemsky.ocrtest.helper.ImageAnalyzer
import org.threeten.bp.Instant
import java.util.Stack
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreviewScreen() {
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val preview = Preview.Builder().build()
    val textRecognizer =
        remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var text by rememberSaveable {
        mutableStateOf("")
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember { LifecycleCameraController(context) }
    val previewView = remember {
        PreviewView(context)
    }
    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
    val imageCapture = remember {
        ImageCapture.Builder().build()
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder().build().apply {
            setAnalyzer(executor, { imageProxy ->
                imageProxy.image?.let {
                    textRecognizer.process(
                        InputImage.fromMediaImage(
                            it,
                            imageProxy.imageInfo.rotationDegrees
                        )
                    )
                        .addOnSuccessListener { visionText ->
                            val resultText = visionText.text
                            text = resultText
                            cameraController.clearImageAnalysisAnalyzer()
                            imageProxy.close()
                        }
                        .addOnFailureListener { e ->
                            e.fillInStackTrace()
                            imageProxy.close()
                        }
                }
            })
        }
    }
    previewView.controller = cameraController
    cameraController.cameraSelector = cameraSelector

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
//        cameraController.bindToLifecycle(lifecycleOwner)
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis,
            imageCapture
        )
        preview.surfaceProvider = previewView.surfaceProvider
    }
    Box(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.padding(8.dp), Alignment.BottomCenter) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.padding(8.dp)) {
                Row {
                    Button(onClick = { captureImage(imageCapture, context) }) {
                        Text(text = "Capture Image")
                    }
//                    Box(Modifier.padding(8.dp))
//                    Box(modifier = Modifier.padding(8.dp), Alignment.TopCenter) {
//                        Text(text = text, color = Color.Green)
//                    }
                }

            }
        }
    }

}


private fun captureImage(imageCapture: ImageCapture, context: Context) {
    val name = "${Instant.now().epochSecond}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let {
                    ImageAnalyzer().analyzeFromImage(context, it)
                    Toast.makeText(context, "Image saved successfully", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}


private suspend fun Context.getCameraProvider(): ProcessCameraProvider {
    return suspendCoroutine { continuation ->
        ProcessCameraProvider.getInstance(this).also { cameraProvider ->
            cameraProvider.addListener({
                try {
                    val provider = cameraProvider.get()
                    continuation.resume(provider)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }
}