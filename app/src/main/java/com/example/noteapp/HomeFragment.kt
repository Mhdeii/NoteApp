package com.example.noteapp

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.noteapp.models.Post
import com.ezxample.noteapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment(), PostAdapter.OnDeleteClickListener {

    private lateinit var apiService: ApiService
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var postDao: PostDao

    companion object {
        const val ADD_POST_REQUEST = 1
        const val USER_ID_EXTRA = "USER_ID"
        const val USERNAME_EXTRA = "USERNAME"
        const val NAME_EXTRA = "NAME"
        const val TAG = "HomeFragment"
        const val LAST_POST_ID_EXTRA = "LAST_POST_ID"
    }

    private var userId: Int = -1
    private var username: String? = null
    private var name: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val sharedPreferences = requireActivity().getSharedPreferences("SessionPrefs", MODE_PRIVATE)
        userId = sharedPreferences.getInt("userId", -1)
        username = sharedPreferences.getString("username", null)
        name = sharedPreferences.getString("name", null)


        with(sharedPreferences.edit()) {
            putInt("currentUserId", userId)
            apply()
        }

        if (username != null && name != null) {
            Toast.makeText(requireContext(), "Welcome, $name", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Welcome, $name")
            Log.d(TAG, "Username: $username")
        }

        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.floatingActionButton)
        floatingActionButton.setOnClickListener {
            val intent = Intent(requireContext(), AddPostActivity::class.java)

            intent.putExtra(USER_ID_EXTRA, userId)
            intent.putExtra(USERNAME_EXTRA, username)
            intent.putExtra(NAME_EXTRA, name)
            intent.putExtra(LAST_POST_ID_EXTRA, lastPostId)

            startActivityForResult(intent, ADD_POST_REQUEST)
        }

        fetchDataAndUpdateUI()
        postDao = AppDatabase.getDatabase(requireContext()).postDao()
        initRetrofit()
        setupRecyclerView(view)
        setupSwipeRefreshLayout(view)


        return view
    }



    private fun setupSwipeRefreshLayout(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchDataAndUpdateUI()
        }
    }

    private var lastPostId: Int = 0

    private fun fetchDataAndUpdateUI() {
        lifecycleScope.launch {
            val postsFetched = fetchPostsFromServer()
            if (!postsFetched) {
                fetchPostsFromLocal()
            }
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private suspend fun fetchPostsFromServer(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPosts("date,time", "desc").execute()
                if (response.isSuccessful) {
                    val posts = response.body() ?: emptyList()
                    postDao.insertPosts(posts.map {
                        PostEntity(
                            it.id.toString(), it.title, it.description, it.authorId, it.views, it.date,
                            it.time, it.img ?: "", it.latitude, it.longitude
                        )
                    })
                    Log.d(TAG, "Posts fetched and saved to local DB")

                    val sortedPosts = postDao.getAllPostsSortedByDate()
                    withContext(Dispatchers.Main) {
                        updateUIWithPosts(sortedPosts.map {
                            Post(it.id, it.title, it.authorId, it.description, it.views, it.date, it.time, emptyList(), it.img, it.latitude, it.longitude)
                        })
                    }
                    true
                } else {
                    Log.e(TAG, "Failed to fetch posts. Response code: ${response.code()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching posts", e)
                false
            }
        }
    }

    private fun fetchPostsFromLocal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val localPosts = postDao.getAllPostsSortedByDate()
            withContext(Dispatchers.Main) {
                updateUIWithPosts(localPosts.map {
                    Post(it.id, it.title, it.authorId, it.description, it.views, it.date, it.time, emptyList(), it.img, it.latitude, it.longitude)
                })
            }
        }
    }


    private fun updateUIWithPosts(posts: List<Post>) {
        postAdapter.updatePosts(posts)
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val userIdStr = userId.toString()
        Log.d(TAG, "Current user ID: $userIdStr")
        postAdapter = PostAdapter(emptyList(), userIdStr, this, apiService)
        recyclerView.adapter = postAdapter

        lifecycleScope.launch {
            postDao.deleteAllPosts()
        }

    }

    private fun initRetrofit() {
        val retrofit = RetrofitClient.getClient()
        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onDeleteClicked(post: Post) {
        lifecycleScope.launch {
            try {
                val deleteResponse = apiService.deletePost(post.id).execute()
                if (deleteResponse.isSuccessful) {
                    withContext(Dispatchers.IO) {
                        Log.d(TAG, "Attempting to delete post from local DB with ID: ${post.id.toInt()}")
                        postDao.deletePost(post.id)
                        Log.d(TAG, "Successfully deleted post from local DB with ID: ${post.id.toInt()}")
                    }
                    fetchDataAndUpdateUI()
                    showToast("Post deleted successfully.")
                } else {
                    showToast("Failed to delete post. Error code: ${deleteResponse.code()}")
                }
            } catch (e: Exception) {
                showToast("Failed to delete post. Please check your internet connection and try again.")
                Log.e(TAG, "Error deleting post", e)
            }
        }
    }



    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_POST_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            data?.getParcelableExtra<Post>("NEW_POST")?.let { newPost ->
                lifecycleScope.launch {
                    postDao.insertPosts(
                        listOf(
                            PostEntity(
                                newPost.id,
                                newPost.title,
                                newPost.description,
                                newPost.authorId,
                                newPost.views,
                                newPost.date,
                                newPost.time,
                                newPost.img ?: "",
                                newPost.latitude,
                                newPost.longitude
                            )
                        )
                    )
                    fetchPostsFromServer()
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        fetchDataAndUpdateUI()
    }

    override fun onResume() {
        super.onResume()
        fetchDataAndUpdateUI()
    }
}

