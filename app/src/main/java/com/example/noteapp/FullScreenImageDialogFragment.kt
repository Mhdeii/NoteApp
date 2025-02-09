package com.example.noteapp

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.ezxample.noteapp.R

class FullScreenImageDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGE_BASE64 = "image_base64"

        fun newInstance(imageBase64: String): FullScreenImageDialogFragment {
            val fragment = FullScreenImageDialogFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_BASE64, imageBase64)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_full_screen_image_dialog, container, false)
        val imageView = view.findViewById<ImageView>(R.id.fullScreenImageView)
        val imageBase64 = arguments?.getString(ARG_IMAGE_BASE64) ?: ""
        val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)

        // Decode bitmap with options to improve quality
        val options = BitmapFactory.Options()
        options.inPreferredConfig = Bitmap.Config.ARGB_8888 // High quality config
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        if (bitmap != null) {
            // Calculate image dimensions and adjust view size
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // Scale bitmap to fit screen width and maintain aspect ratio
            val scaledBitmap = scaleBitmapToFitWidth(bitmap, screenWidth)
            imageView.setImageBitmap(scaledBitmap)
        }

        return view
    }

    private fun scaleBitmapToFitWidth(bitmap: Bitmap, targetWidth: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val scaleFactor = targetWidth.toFloat() / originalWidth
        val targetHeight = (originalHeight * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        return dialog
    }
}
