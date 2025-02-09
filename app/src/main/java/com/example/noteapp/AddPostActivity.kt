package com.example.noteapp

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.noteapp.models.Post
import com.ezxample.noteapp.R
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class AddPostActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var switchLocation: Switch
    private lateinit var viewPager: ViewPager2
    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private var imagesBase64: MutableList<String> = mutableListOf()
    private var currentLocation: Pair<Double, Double>? = null
    private var progressDialog: ProgressDialog? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 2
        private const val REQUEST_IMAGE_GALLERY = 3
        private const val REQUEST_READ_EXTERNAL_STORAGE = 4
        private const val REQUEST_LOCATION_PERMISSION = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        val userId = intent.getIntExtra(HomeFragment.USER_ID_EXTRA, -1)
        val username = intent.getStringExtra(HomeFragment.USERNAME_EXTRA) ?: "Unknown"
        val name = intent.getStringExtra(HomeFragment.NAME_EXTRA) ?: ""

        if (userId == -1) {
            showToast("User ID not found")
            finish()
            return
        }

        Log.d("AddPostActivity", "User ID: $userId")
        Log.d("AddPostActivity", "Username: $username")
        Log.d("AddPostActivity", "Name: $name")

        titleEditText = findViewById(R.id.TitleEditText)
        descriptionEditText = findViewById(R.id.DescriptionEditText)
        switchLocation = findViewById(R.id.switch1)
        viewPager = findViewById(R.id.photoViewPager)

        val backBtn: ImageView = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }

        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 30) {
                    titleEditText.error = "Max 30 characters allowed"
                }
            }
        })

        descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > 120) {
                    descriptionEditText.error = "Max 120 characters allowed"
                }
            }
        })

        val addPostButton: Button = findViewById(R.id.addPostButton)
        val cancelButton: Button = findViewById(R.id.cancelButton)
        val takePhotoButton: ImageButton = findViewById(R.id.captureImageButton)
        val insertImageButton: ImageButton = findViewById(R.id.insertImageButton)

        initRetrofit()
        imagePagerAdapter = ImagePagerAdapter(this, imagesBase64)
        viewPager.adapter = imagePagerAdapter

        addPostButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val description = descriptionEditText.text.toString().trim()

            if (title.isEmpty() || description.isEmpty()) {
                showToast("Please fill in all fields.")
            } else {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val location = currentLocation

                val newPost = Post(
                    title = title,
                    authorId = userId,
                    description = description,
                    date = currentDate,
                    time = currentTime,
                    views = 0,
                    comments = emptyList(),
                    img = imagesBase64.joinToString(","), // Save all images as comma-separated Base64 strings
                    latitude = location?.first ?: 0.0,
                    longitude = location?.second ?: 0.0
                )
                addPostToServer(newPost)
            }
        }

        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        takePhotoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            } else {
                dispatchTakePictureIntent()
            }
        }

        insertImageButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
            } else {
                openGallery()
            }
        }

        switchLocation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION_PERMISSION)
                } else {
                    getLocation(this)
                }
            } else {
                currentLocation = null
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun openGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_READ_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    dispatchTakePictureIntent()
                } else {
                    showToast("Camera permission is required to take photos.")
                }
            }
            REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    showToast("Storage permission is required to select photos.")
                }
            }
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation(this)
                } else {
                    showToast("Location permission is required to get your location.")
                    switchLocation.isChecked = false
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    imagesBase64.add(encodeImageToBase64(imageBitmap))
                    updateViewPager()
                }
                REQUEST_IMAGE_GALLERY -> {
                    val selectedImage: Uri? = data?.data
                    try {
                        val imageStream: InputStream? = selectedImage?.let { contentResolver.openInputStream(it) }
                        val selectedBitmap = BitmapFactory.decodeStream(imageStream)
                        imagesBase64.add(encodeImageToBase64(selectedBitmap))
                        updateViewPager()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Failed to load image from gallery.")
                    }
                }
            }
        }
    }

    private fun updateViewPager() {
        if (imagesBase64.isNotEmpty()) {
            viewPager.visibility = View.VISIBLE
            imagePagerAdapter.notifyDataSetChanged()
            viewPager.setCurrentItem(imagesBase64.size - 1, true)
        } else {
            viewPager.visibility = View.GONE
        }
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun getLocation(context: Context) {
        progressDialog = ProgressDialog(context)
        progressDialog?.setMessage("Fetching location...")
        progressDialog?.setCancelable(false)
        progressDialog?.show()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    currentLocation = Pair(it.latitude, it.longitude)
                }
                fusedLocationClient.removeLocationUpdates(this)
                progressDialog?.dismiss()
            }
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun addPostToServer(post: Post) {
        apiService.addPost(post).enqueue(object : Callback<Post> {
            override fun onResponse(call: Call<Post>, response: Response<Post>) {
                if (response.isSuccessful) {
                    val returnedPost = response.body()
                    if (returnedPost != null) {
                        showToast("Post added successfully!")
                        navigateToHomeActivity()
                    }
                } else {
                    showToast("Failed to add post to server.")
                }
            }

            override fun onFailure(call: Call<Post>, t: Throwable) {
                showToast("Failed to add post to server: ${t.message}")
            }
        })
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(HomeFragment.USER_ID_EXTRA, intent.getIntExtra(HomeFragment.USER_ID_EXTRA, -1))
        startActivity(intent)
    }

    private fun initRetrofit() {
        val retrofit = RetrofitClient.getClient()
        apiService = retrofit.create(ApiService::class.java)
    }
}
