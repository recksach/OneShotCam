package com.oneshotcam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.oneshotcam.databinding.ActivityCameraBinding
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isProcessing = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) startCamera() else Toast.makeText(this, " Camera permission denied\, Toast.LENGTH_SHORT).show()
 }

 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)
 binding = ActivityCameraBinding.inflate(layoutInflater)
 setContentView(binding.root)
 cameraExecutor = Executors.newSingleThreadExecutor()
 if (allPermissionsGranted()) startCamera() else requestPermissionLauncher.launch(Manifest.permission.CAMERA)
 binding.captureButton.setOnClickListener { takePhoto() }
 }

 private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

 private fun startCamera() {
 val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
 cameraProviderFuture.addListener({
 val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
 val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }
 imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
 val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
 try {
 cameraProvider.unbindAll()
 cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
 } catch (e: Exception) {
 Log.e(TAG, \Use case binding failed\, e)
 }
 }, ContextCompat.getMainExecutor(this))
 }

 private fun takePhoto() {
 if (isProcessing) return
 val imageCapture = imageCapture ?: return
 isProcessing = true
 binding.progressBar.visibility = View.VISIBLE
 binding.captureButton.isEnabled = false
 val outputOptions = ImageCapture.OutputFileOptions.Builder(createTempFile()).build()
 imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
 override fun onImageSaved(output: ImageCapture.OutputFileResults) {
 val file = File(output.savedUri?.path ?: return)
 CoroutineScope(Dispatchers.Main).launch { processImage(file) }
 }
 override fun onError(exc: ImageCaptureException) {
 isProcessing = false
 binding.progressBar.visibility = View.GONE
 binding.captureButton.isEnabled = true
 Toast.makeText(this@CameraActivity, \Failed: \\, Toast.LENGTH_SHORT).show()
 }
 })
 }

 private suspend fun processImage(file: File) {
 withContext(Dispatchers.IO) {
 try {
 val bitmap = BitmapFactory.decodeFile(file.absolutePath)
 val rotated = rotateBitmapIfNeeded(bitmap, file)
 val base64 = bitmapToBase64(rotated)
 val apiKey = ApiKeyManager.getKey(this@CameraActivity)
 val request = GenerateContentRequest(
 contents = listOf(
 Content(
 parts = listOf(
 Part(text = GeminiConfig.PROMPT),
 Part(inlineData = InlineData(mimeType = \image/jpeg\, data = base64))
 )
 )
 ),
 generationConfig = GenerationConfig(responseModalities = listOf(\IMAGE\))
 )
 val response = GeminiApiClient.api.generateContent(GeminiConfig.MODEL_NAME, apiKey, request)
 val imageData = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }?.inlineData
 if (imageData != null && imageData.data != null) {
 val bytes = Base64.decode(imageData.data, Base64.DEFAULT)
 val resultFile = File(cacheDir, \result_\.jpg\)
 resultFile.writeBytes(bytes)
 withContext(Dispatchers.Main) {
 val intent = Intent(this@CameraActivity, ResultActivity::class.java)
 intent.putExtra(\result_path\, resultFile.absolutePath)
 startActivity(intent)
 finish()
 }
 } else {
 withContext(Dispatchers.Main) {
 isProcessing = false
 binding.progressBar.visibility = View.GONE
 binding.captureButton.isEnabled = true
 Toast.makeText(this@CameraActivity, \No image in response\, Toast.LENGTH_SHORT).show()
 }
 }
 } catch (e: Exception) {
 withContext(Dispatchers.Main) {
 isProcessing = false
 binding.progressBar.visibility = View.GONE
 binding.captureButton.isEnabled = true
 Toast.makeText(this@CameraActivity, \Error: \\, Toast.LENGTH_LONG).show()
 }
 }
 }
 }

 private fun rotateBitmapIfNeeded(bitmap: Bitmap, file: File): Bitmap {
 val exif = ExifInterface(file.absolutePath)
 val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
 val matrix = Matrix()
 when (orientation) {
 ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
 ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
 ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
 else -> return bitmap
 }
 return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
 }

 private fun bitmapToBase64(bitmap: Bitmap): String {
 val stream = ByteArrayOutputStream()
 bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
 return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
 }

 override fun onDestroy() {
 super.onDestroy()
 cameraExecutor.shutdown()
 }

 companion object {
 private const val TAG = \CameraActivity\
 }
}

