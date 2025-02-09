package com.example.noteapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ezxample.noteapp.R
import com.ezxample.noteapp.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import com.google.gson.Gson

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var posts: List<PostEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        fetchPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        updateMapWithPosts()
    }

    private fun fetchPosts() {
        lifecycleScope.launch {
            posts = AppDatabase.getDatabase(requireContext()).postDao().getAllPosts()
            updateMapWithPosts()
        }

    }

    private fun updateMapWithPosts() {
        if (::map.isInitialized) {
            map.clear()
            val boundsBuilder = LatLngBounds.Builder()
            for (post in posts) {
                val location = LatLng(post.latitude, post.longitude)
                val markerTitle = "${post.title} by ${post.authorId}"
                map.addMarker(MarkerOptions().position(location).title(markerTitle))
                boundsBuilder.include(location)
            }
            if (posts.isNotEmpty()) {
                val bounds = boundsBuilder.build()
                val padding = resources.getDimensionPixelSize(R.dimen.map_padding) // Adjust as needed
                val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
                map.moveCamera(cameraUpdate)
            }
            map.setOnMarkerClickListener { marker ->
                val selectedPost = posts.find { "${it.title} by ${it.authorId}" == marker.title }
                selectedPost?.let {
                    val postJson = Gson().toJson(it)
                    val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
                        putExtra("POST", postJson)
                    }
                    startActivity(intent)
                }
                true
            }
        }
    }
}
