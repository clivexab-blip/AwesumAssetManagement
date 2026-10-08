package com.awesum.assetmanagement.ui.add

import android.content.ContentValues.TAG
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.awesum.assetmanagement.Constants
import com.awesum.assetmanagement.R
import com.awesum.assetmanagement.api.ApiClient
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UploadPhotoActivity : AppCompatActivity() {

    private val jobId: Int
        get() = intent.getIntExtra(Constants.EXTRA_JOB_ID, Constants.CURRENT_JOB_ID)

    private var selectedUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            showPreview(uri)
        }
    }

    private lateinit var ivPreview: ImageView
    private lateinit var placeholder: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_photo)

        val dropZone = findViewById<FrameLayout>(R.id.dropZone)
        ivPreview = findViewById(R.id.ivPreview)
        placeholder = findViewById(R.id.dropZonePlaceholder)
        val etCaption = findViewById<EditText>(R.id.etCaption)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnSave = findViewById<TextView>(R.id.btnSavePhoto)
        val btnBack = findViewById<TextView>(R.id.btnBack)

        dropZone.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "Please choose a photo first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            uploadPhoto(uri, etCaption.text.toString().trim())
        }
    }

    private fun showPreview(uri: Uri) {
        placeholder.visibility = View.GONE
        ivPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(ivPreview)
    }

    private fun uploadPhoto(uri: Uri, caption: String) {
        lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { copyUriToTempFile(uri) }
                val mediaType = (contentResolver.getType(uri) ?: "image/jpeg").toMediaTypeOrNull()
                val requestFile = file.asRequestBody(mediaType)
                val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                val jobIdBody = jobId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val captionBody = caption.toRequestBody("text/plain".toMediaTypeOrNull())

                val res = ApiClient.instance.uploadPhoto(jobIdBody, captionBody, photoPart)
                if (res.isSuccessful && res.body()?.success == true) {
                    Toast.makeText(this@UploadPhotoActivity, "Photo uploaded", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@UploadPhotoActivity, res.body()?.message ?: "Upload failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload error", e)
                Toast.makeText(this@UploadPhotoActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyUriToTempFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open selected image")
        val tempFile = File.createTempFile("upload_", ".jpg", cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        return tempFile
    }
}
