package com.example.noteapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.noteapp.models.Post
import com.google.gson.Gson
import com.ezxample.noteapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostDetailActivity : AppCompatActivity() {

    private lateinit var viewsTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        val titleTextView: TextView = findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = findViewById(R.id.description)
        viewsTextView = findViewById(R.id.viewsTextView)
        val dateTextView: TextView = findViewById(R.id.dateTextView)
        val timeTextView: TextView = findViewById(R.id.timeTextView)
        val viewPager: ViewPager2 = findViewById(R.id.photoViewPager)

        val backBtn: ImageView = findViewById(R.id.backBtn)
        backBtn.setOnClickListener {
            finish()
        }

        val postJson = intent.getStringExtra("POST")
        if (postJson != null) {
            val post = Gson().fromJson(postJson, Post::class.java)

            titleTextView.text = post.title
            descriptionTextView.text = post.description
            viewsTextView.text = post.views.toString()
            dateTextView.text = post.date
            timeTextView.text = post.time

            if (post.img != null && post.img!!.isNotEmpty()) {
                try {
                    val imagesBase64 = post.img!!.split(",")
                    val adapter = ImagePagerAdapter(this, imagesBase64)
                    viewPager.adapter = adapter
                    viewPager.visibility = View.VISIBLE
                } catch (e: IllegalArgumentException) {
                    Log.e("PostDetailActivity", "Error decoding base64 string: ${e.message}")
                    viewPager.visibility = View.GONE
                }
            } else {
                viewPager.visibility = View.GONE
            }
            incrementViews(post)
        } else {
            Log.e("PostDetailActivity", "Post data is null")
            finish()
        }
    }

    private fun incrementViews(post: Post) {
        post.views += 1
        updatePostViews(post)
        viewsTextView.text = post.views.toString()
    }

    private fun updatePostViews(post: Post) {
        val apiService = RetrofitClient.getClient().create(ApiService::class.java)
        lifecycleScope.launch {
            try {
                val response = apiService.updatePost(post.id.toString(), post)
                if (response.isSuccessful) {
                    updatePostInDatabase(post)
                    Log.d("PostDetailActivity", "Views updated successfully")
                } else {
                    Log.e("PostDetailActivity", "Failed to update views")
                }
            } catch (e: Exception) {
                Log.e("PostDetailActivity", "Error updating views", e)
            }
        }
    }

    private suspend fun updatePostInDatabase(post: Post) {
        withContext(Dispatchers.IO) {
            val postDao = AppDatabase.getDatabase(applicationContext).postDao()
            val postEntity = PostEntity(
                id = post.id,
                title = post.title,
                description = post.description,
                authorId = post.authorId,
                views = post.views,
                date = post.date,
                time = post.time,
                img = post.img!!,
                latitude = post.latitude,
                longitude = post.longitude
            )
            postDao.updatePost(postEntity)
        }
    }
}
