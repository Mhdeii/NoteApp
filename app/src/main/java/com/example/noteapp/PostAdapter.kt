package com.example.noteapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.noteapp.models.Post
import com.ezxample.noteapp.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class PostAdapter(
    private var posts: List<Post>,
    private val currentUserId: String,
    private val lifecycleOwner: LifecycleOwner,
    private val apiService: ApiService
) : RecyclerView.Adapter<PostViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.post_viewholder, parent, false)
        return PostViewHolder(itemView, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.setOnDeleteClickListener(object : PostViewHolder.OnDeleteClickListener {
            override fun onDeleteClicked(post: Post) {
                removePost(post, holder.itemView.context)
            }
        })

        lifecycleOwner.lifecycleScope.launch {
            val username = fetchUsername(post.authorId)
            withContext(Dispatchers.Main) {
                holder.bind(post, currentUserId.toInt(), username)
            }
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra("POST", Gson().toJson(post))
            }
            context.startActivity(intent)
        }
    }

    private suspend fun fetchUsername(userId: Int): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getUsernameById(userId).execute()
                if (response.isSuccessful) {
                    response.body()?.name ?: "Unknown"
                } else {
                    "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }
    }

    override fun getItemCount() = posts.size

    fun removePost(post: Post, context: Context) {
        val index = posts.indexOfFirst { it.id == post.id }
        if (index != -1) {
            val mutableList = posts.toMutableList()
            mutableList.removeAt(index)
            posts = mutableList.toList()
            notifyItemRemoved(index)

            // Perform server and local delete
            lifecycleOwner.lifecycleScope.launch {
                val apiService = RetrofitClient.getClient().create(ApiService::class.java)
                val postDao = AppDatabase.getDatabase(context).postDao()

                val response = deletePostFromServer(apiService, post.id)
                if (response.isSuccessful) {
                    postDao.deletePost(post.id)
                } else {
                    // Handle error
                }
            }
        }
    }

    private suspend fun deletePostFromServer(apiService: ApiService, postId: String): Response<Void> {
        Log.d("DeletePost", "Attempting to delete post with ID: $postId")
        return withContext(Dispatchers.IO) {
            val response = apiService.deletePost(postId).execute()
            if (response.isSuccessful) {
                Log.d("DeletePost", "Successfully deleted post with ID: $postId")
            } else {
                Log.e("DeletePost", "Failed to delete post with ID: $postId. Response code: ${response.code()}, message: ${response.message()}")
            }
            response
        }
    }



    interface OnDeleteClickListener {
        fun onDeleteClicked(post: Post)
    }

    private var onDeleteClickListener: OnDeleteClickListener? = null

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        onDeleteClickListener = listener
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}