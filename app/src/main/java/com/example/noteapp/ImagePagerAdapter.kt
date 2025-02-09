package com.example.noteapp

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.ezxample.noteapp.R

class ImagePagerAdapter(private val context: Context, private val imagesBase64: List<String>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {   
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_viewpager_item, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageBase64 = imagesBase64[position]
        try {
            if (imageBase64.isNotEmpty()) {
                val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.imageView.setImageBitmap(bitmap)
            } else {
                holder.imageView.setImageResource(R.drawable.default_image) // Set default image
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            holder.imageView.setImageResource(R.drawable.default_image) // Handle decoding error with default image
        }

        holder.imageView.setOnClickListener {
            val activity = context as FragmentActivity
            val dialogFragment = FullScreenImageDialogFragment.newInstance(imageBase64)
            dialogFragment.show(activity.supportFragmentManager, "FullScreenImageDialog")
        }
    }

    override fun getItemCount(): Int {
        return imagesBase64.size
    }
}
