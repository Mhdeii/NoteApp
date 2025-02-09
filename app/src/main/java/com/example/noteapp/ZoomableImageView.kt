package com.example.noteapp

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs), View.OnTouchListener {

    private val matrix = Matrix()
    private val scaleDetector: ScaleGestureDetector
    private var scale = 1f
    private var last = PointF()
    private var start = PointF()
    private val matrixValues = FloatArray(9)

    // Initial screen size
    private var initWidth = 0f
    private var initHeight = 0f

    init {
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        this.setOnTouchListener(this)
        imageMatrix = matrix
        scaleType = ScaleType.MATRIX
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initWidth = w.toFloat()
        initHeight = h.toFloat()
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val curr = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                last.set(curr)
                start.set(last)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    val dx = curr.x - last.x
                    val dy = curr.y - last.y
                    matrix.postTranslate(dx, dy)
                    fixTranslation()
                    last.set(curr.x, curr.y)
                }
            }
        }

        imageMatrix = matrix
        return true
    }

    private fun fixTranslation() {
        matrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]
        val fixTransX = getFixTranslation(transX, width.toFloat(), drawable.intrinsicWidth.toFloat() * scale)
        val fixTransY = getFixTranslation(transY, height.toFloat(), drawable.intrinsicHeight.toFloat() * scale)
        if (fixTransX != 0f || fixTransY != 0f) {
            matrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        return when {
            trans < minTrans -> -trans + minTrans
            trans > maxTrans -> -trans + maxTrans
            else -> 0f
        }
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val focusX = detector.focusX
            val focusY = detector.focusY

            // Restrict scaling out beyond initial size
            if (scale * scaleFactor < 1f) {
                scale = 1f
            } else {
                scale *= scaleFactor
                scale = Math.max(1f, Math.min(scale, MAX_SCALE))
            }

            matrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
            fixScaleTranslation()
            return true
        }
    }

    private fun fixScaleTranslation() {
        matrix.getValues(matrixValues)
        val scaleX = matrixValues[Matrix.MSCALE_X]
        val scaleY = matrixValues[Matrix.MSCALE_Y]

        val width = width.toFloat()
        val height = height.toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val scaledWidth = drawableWidth * scaleX
        val scaledHeight = drawableHeight * scaleY

        val dx = width - scaledWidth
        val dy = height - scaledHeight

        if (scaledWidth < width) {
            matrixValues[Matrix.MTRANS_X] = dx / 2f
        } else if (matrixValues[Matrix.MTRANS_X] > 0) {
            matrixValues[Matrix.MTRANS_X] = 0f
        } else if (matrixValues[Matrix.MTRANS_X] < dx) {
            matrixValues[Matrix.MTRANS_X] = dx
        }

        if (scaledHeight < height) {
            matrixValues[Matrix.MTRANS_Y] = dy / 2f
        } else if (matrixValues[Matrix.MTRANS_Y] > 0) {
            matrixValues[Matrix.MTRANS_Y] = 0f
        } else if (matrixValues[Matrix.MTRANS_Y] < dy) {
            matrixValues[Matrix.MTRANS_Y] = dy
        }

        matrix.setValues(matrixValues)
    }

    companion object {
        private const val MAX_SCALE = 5f
    }
}
