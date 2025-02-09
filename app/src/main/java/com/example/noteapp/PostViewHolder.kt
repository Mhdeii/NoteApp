package com.example.noteapp

import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.noteapp.models.Post
import com.ezxample.noteapp.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostViewHolder(itemView: View, private val lifecycleOwner: LifecycleOwner) : RecyclerView.ViewHolder(itemView) {
    private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
    private val descriptionEditText: EditText = itemView.findViewById(R.id.description)
    private val viewsTextView: TextView = itemView.findViewById(R.id.viewsTextView)
    private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
    private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
    private val commentsTextView: TextView = itemView.findViewById(R.id.commentsTextView)
    private val authorTextView: TextView = itemView.findViewById(R.id.authorTextView)
    private val editButton: ImageButton = itemView.findViewById(R.id.editButton)
    private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    private val photoViewPager: ViewPager2 = itemView.findViewById(R.id.photoViewPager)
    private var currentPost: Post? = null
    private val chatTextView: TextView = itemView.findViewById(R.id.chatTextView)

    fun bind(post: Post, currentUserId: Int, username: String) {
        currentPost = post
        titleTextView.text = post.title
        descriptionEditText.setText(post.description)
        descriptionEditText.isEnabled = false
        viewsTextView.text = post.views.toString()
        dateTextView.text = post.date
        timeTextView.text = post.time
        authorTextView.text = username
        commentsTextView.text = null



        if (post.img!!.isNotEmpty()) {
            val imageBase64List = post.img!!.split(",")
            val imagePagerAdapter = ImagePagerAdapter(itemView.context, imageBase64List)
            photoViewPager.adapter = imagePagerAdapter
            photoViewPager.visibility = View.VISIBLE
        } else {
            photoViewPager.visibility = View.GONE
        }

        if (post.authorId == currentUserId) {
            editButton.visibility = View.VISIBLE
            deleteButton.visibility = View.VISIBLE

            deleteButton.setOnClickListener {
                currentPost?.let {
                    onDeleteClickListener?.onDeleteClicked(it)
                }
            }

            var isEditing = false

            editButton.setOnClickListener {
                if (isEditing) {
                    val updatedDescription = descriptionEditText.text.toString()
                    val updatedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val updatedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

                    post.description = updatedDescription
                    post.date = updatedDate
                    post.time = updatedTime

                    val apiService = RetrofitClient.getClient().create(ApiService::class.java)
                    lifecycleOwner.lifecycleScope.launch {
                        try {
                            val response = apiService.updatePost(post.id.toString(), post)
                            if (response.isSuccessful) {
                                updatePostInDatabase(post)
                                dateTextView.text = updatedDate
                                timeTextView.text = updatedTime
                                Toast.makeText(itemView.context, "Post updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("PostViewHolder", "Failed to update post")
                            }
                        } catch (e: Exception) {
                            Log.e("PostViewHolder", "Error updating post", e)
                        }
                    }

                    descriptionEditText.isEnabled = false
                    editButton.setImageResource(R.drawable.edit_24px)
                    isEditing = false
                } else {
                    descriptionEditText.isEnabled = true
                    editButton.setImageResource(R.drawable.save_24px)
                    isEditing = true
                }
            }
        } else {
            editButton.visibility = View.GONE
            deleteButton.visibility = View.GONE
        }

        deleteButton.setOnClickListener {
            AlertDialog.Builder(itemView.context)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Yes") { dialog, _ ->
                    currentPost?.let {
                        onDeleteClickListener?.onDeleteClicked(it)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        chatTextView.setOnClickListener {
            AlertDialog.Builder(itemView.context)
                .setTitle("Confirm Chat")
                .setMessage("Do you want to chat with this user?")
                .setPositiveButton("Yes") { dialog, _ ->
                    val context = itemView.context
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("USER_ID", post.authorId.toLong())
                    intent.putExtra("USERNAME", username)
                    context.startActivity(intent)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    interface OnDeleteClickListener {
        fun onDeleteClicked(post: Post)
    }

    private var onDeleteClickListener: OnDeleteClickListener? = null

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        onDeleteClickListener = listener
    }

    private suspend fun updatePostInDatabase(post: Post) {
        withContext(Dispatchers.IO) {
            val postDao = AppDatabase.getDatabase(itemView.context).postDao()
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
