package com.ooplab.exercises_fitfuel

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.camera.core.ImageProxy
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.atan2

object Utils {

    fun <T, VB : ViewBinding> RecyclerView.setData(
        items: List<T>,
        bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
        bindHolder: (binding: VB, item: T, position: Int) -> Unit,
    ) {
        if (this.layoutManager == null) {
            layoutManager = LinearLayoutManager(context)
        }
        val existingAdapter = this.adapter as? GenericAdapter<T, VB>
        if (existingAdapter != null && existingAdapter.currentBindingInflater == bindingInflater) {
            existingAdapter.updateData(items)
        } else {
            val adapter = GenericAdapter(
                items,
                bindingInflater,
                bindHolder,
                animation
            )
            this.adapter = adapter
        }
    }
    class DataViewHolder<VB : ViewBinding>(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    class GenericAdapter<T, VB : ViewBinding>(
        private var items: List<T>,
        private val bindingInflater: (LayoutInflater, ViewGroup, Boolean) -> VB,
        private val bindHolder: (binding: VB, item: T, position: Int) -> Unit,
        private val animation: Animation?,
    ) : RecyclerView.Adapter<DataViewHolder<VB>>() {
        var currentBindingInflater = bindingInflater

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder<VB> {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = bindingInflater(layoutInflater, parent, false)
            return DataViewHolder(binding)
        }
        override fun onBindViewHolder(holder: DataViewHolder<VB>, position: Int) {


            bindHolder(holder.binding, items[position], position)
            animation?.let {
                holder.itemView.startAnimation(it)
            }
        }

        override fun getItemCount(): Int = items.size
        fun updateData(newItems: List<T>) {
            items = newItems
            notifyDataSetChanged()
        }
    }


    // Calculates the angle between three points
    fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        var angle = Math.toDegrees(abs(radians).toDouble())
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }

    // Calculates the angle between three points
    fun calculateAngle360(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        val angle = Math.toDegrees(abs(radians).toDouble())

        return angle
    }


    // Converts a YUV image to an RGB bitmap.
    fun yuvToRgb(image: Image, imageProxy: ImageProxy): Bitmap {
        // Accesses the Y, U, and V planes from the image.
        val yBuffer = image.planes[0].buffer // Luminance plane.
        val uBuffer = image.planes[1].buffer // Chrominance U plane.
        val vBuffer = image.planes[2].buffer // Chrominance V plane.

        // Calculates the size of each plane.
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // Creates a byte array to hold the NV21 formatted data.
        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copies the Y plane data into the byte array.
        yBuffer.get(nv21, 0, ySize)
        // Copies the V and U plane data into the byte array.
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Creates a YuvImage from the NV21 byte array.
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)

        // Creates an output stream to hold the JPEG data.
        val out = ByteArrayOutputStream()

        // Compresses the YUV image to JPEG format.
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)

        // Converts the output stream to a byte array.
        val imageBytes = out.toByteArray()

        // Decodes the byte array into a Bitmap.
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }


    inline fun <reified T : ViewBinding> Activity.viewBinding(
        crossinline bindingInflater: (LayoutInflater) -> T
    ): Lazy<T> {
        return lazy(LazyThreadSafetyMode.NONE) {
            bindingInflater.invoke(layoutInflater).also {
                setContentView(it.root)
            }
        }
    }

}