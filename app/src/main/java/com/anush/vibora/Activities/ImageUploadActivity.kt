package com.anush.vibora.Activities

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.anush.vibora.R
import com.anush.vibora.Utils.Constants
import com.anush.vibora.databinding.ActivityImageUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.wang.avi.AVLoadingIndicatorView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ImageUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageUploadBinding
    private var selectedImageUri: Uri? = null

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var loadingDialog: AlertDialog? = null

    private val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.imageView.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()


    }

    private fun initListeners() {
        binding.imageView.setOnClickListener {
            launcher.launch("image/*")
        }

        binding.uploadBtn.setOnClickListener {
            if (selectedImageUri != null) {
                uploadImageToServer()
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
            }
        }

        binding.skipBtn.setOnClickListener {
            startActivity(Intent(this@ImageUploadActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun uploadImageToServer() {
        showLoading("Uploading Image... Please wait.")

        try {
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) {
                hideLoading()
                Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show()
                return
            }

            val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

            val requestBodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", "6d207e02198a847aa98d0a2a901485a5")
                .addFormDataPart("action", "upload")
                .addFormDataPart("source", base64Image)
                .addFormDataPart("format", "json")

            val apiUrl = "https://freeimage.host/api/1/upload"

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBodyBuilder.build())
                .build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideLoading()
                        Toast.makeText(this@ImageUploadActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        hideLoading()
                    }
                    if (response.isSuccessful) {
                        val jsonResponse = response.body?.string()
                        Log.d("ImageUpload", "Response: $jsonResponse")

                        try {
                            val jsonObject = JSONObject(jsonResponse)
                            val imageObject = jsonObject.getJSONObject("image")
                            val imageUrl = imageObject.getString("url")

                            runOnUiThread {
                                saveImageUrlToFirestore(imageUrl)
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                Toast.makeText(this@ImageUploadActivity, "Failed to parse upload response", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ImageUploadActivity, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

        } catch (e: Exception) {
            hideLoading()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }




    private fun saveImageUrlToFirestore(imageUrl: String) {
        val userId = auth.currentUser?.uid
        showLoading("Saving Image...")
        if (userId != null) {
            val userRef = db.collection(Constants.USERS_COLLECTION).document(userId)
            userRef.update(Constants.TAG_PROFILE_IMAGE, imageUrl)
                .addOnSuccessListener {
                    hideLoading()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    hideLoading()
                    Toast.makeText(this, "Failed to save image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            hideLoading()
        }
    }


    private fun showLoading(message: String) {
        if (loadingDialog == null) {
            loadingDialog = AlertDialog.Builder(this)
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .create()
        }

        // Access the TextView and set the message
        loadingDialog?.findViewById<TextView>(R.id.loadingMessage)?.text = message

        // Access the AVLoadingIndicatorView in the dialog and show it
        val loading = loadingDialog?.findViewById<AVLoadingIndicatorView>(R.id.loading)
        loading?.show()

        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

}
