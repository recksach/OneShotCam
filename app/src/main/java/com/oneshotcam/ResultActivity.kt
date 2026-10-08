package com.oneshotcam

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.oneshotcam.databinding.ActivityResultBinding
import java.io.File
import java.io.OutputStream

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.getStringExtra(" result_path\) ?: return
 val file = File(path)
 Glide.with(this).load(file).into(binding.resultImage)

 binding.btnSave.setOnClickListener {
 saveToGallery(file)
 }

 binding.btnRetake.setOnClickListener {
 val intent = Intent(this, CameraActivity::class.java)
 intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
 startActivity(intent)
 finish()
 }
 }

 private fun saveToGallery(file: File) {
 val bitmap = BitmapFactory.decodeFile(file.absolutePath)
 val contentValues = ContentValues().apply {
 put(MediaStore.Images.Media.DISPLAY_NAME, \oneshotcam_\.jpg\)
 put(MediaStore.Images.Media.MIME_TYPE, \image/jpeg\)
 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
 put(MediaStore.Images.Media.RELATIVE_PATH, \Pictures/OneShotCam\)
 put(MediaStore.Images.Media.IS_PENDING, 1)
 }
 }

 val resolver = contentResolver
 val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
 uri?.let {
 val stream: OutputStream? = resolver.openOutputStream(it)
 stream?.use { s -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, s) }
 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
 contentValues.clear()
 contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
 resolver.update(it, contentValues, null, null)
 }
 Toast.makeText(this, \Saved to gallery\, Toast.LENGTH_SHORT).show()
 } ?: run {
 Toast.makeText(this, \Failed to save\, Toast.LENGTH_SHORT).show()
 }
 }
}

