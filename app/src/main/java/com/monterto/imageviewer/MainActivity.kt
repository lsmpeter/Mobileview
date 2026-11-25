package com.monterto.imageviewer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var canvas: ZoomImageView
    private lateinit var label: TextView
    private lateinit var previousButton: TextView
    private lateinit var nextButton: TextView
    private lateinit var openButton: TextView
    private val images = mutableListOf<Uri>()
    private var currentIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.black)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        canvas = ZoomImageView(this)
        root.addView(canvas, FrameLayout.LayoutParams(-1, -1))

        label = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(18, 14, 18, 14)
            setBackgroundColor(0x99000000.toInt())
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }
        root.addView(label, FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(12, 10, 12, 10)
            setBackgroundColor(0x99000000.toInt())
        }
        previousButton = control("이전") { showImage(currentIndex - 1) }
        openButton = control("열기") { launchPicker() }
        nextButton = control("다음") { showImage(currentIndex + 1) }
        controls.addView(previousButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(openButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        controls.addView(nextButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(controls, FrameLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        setContentView(root)
        handleIntent(intent)
        updateNavigation()
        applyImmersive()
    }

    private fun control(title: String, action: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setPadding(18, 14, 18, 14)
        setOnClickListener { action() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(incoming: Intent?) {
        images.clear()
        currentIndex = -1
        if (incoming == null) {
            label.text = "Monterto Image Viewer"
            canvas.setImageBitmap(null)
            updateNavigation()
            return
        }

        try {
            when (incoming.action) {
                Intent.ACTION_SEND_MULTIPLE -> {
                    val streams = if (Build.VERSION.SDK_INT >= 33) {
                        incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        incoming.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                    streams?.let { images.addAll(it) }
                    addClipData(incoming)
                }
                Intent.ACTION_SEND -> {
                    val stream = if (Build.VERSION.SDK_INT >= 33) {
                        incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                    stream?.let { images.add(it) }
                    addClipData(incoming)
                }
                else -> {
                    incoming.data?.let { images.add(it) }
                    addClipData(incoming)
                }
            }
            val unique = images.distinct()
            images.clear()
            images.addAll(unique)
            if (images.isNotEmpty()) showImage(0) else {
                label.text = "이미지를 열어 주세요"
                canvas.setImageBitmap(null)
            }
        } catch (e: Exception) {
            showError(e)
        }
        updateNavigation()
    }

    private fun addClipData(intent: Intent) {
        val clip = intent.clipData ?: return
        for (i in 0 until clip.itemCount) {
            clip.getItemAt(i).uri?.let { images.add(it) }
        }
    }

    private fun showImage(index: Int) {
        if (index !in images.indices) return
        currentIndex = index
        val uri = images[index]
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) error("이미지를 읽을 수 없습니다")
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("지원하지 않는 이미지입니다")

            val maxSide = 4096
            var sample = 1
            while (max(bounds.outWidth / sample, bounds.outHeight / sample) > maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) error("이미지를 읽을 수 없습니다")
                BitmapFactory.decodeStream(stream, null, options)
            } ?: error("이미지를 표시할 수 없습니다")

            canvas.setImageBitmap(bitmap)
            label.text = "${queryName(uri) ?: "이미지"}  (${index + 1}/${images.size})"
        } catch (e: Exception) {
            showError(e)
            label.text = "이미지를 열 수 없습니다  (${index + 1}/${images.size})"
        }
        updateNavigation()
    }

    private fun showError(e: Exception) {
        Toast.makeText(this, "이미지를 열 수 없습니다: ${e.localizedMessage ?: "오류"}", Toast.LENGTH_LONG).show()
    }

    private fun launchPicker() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try {
            startActivityForResult(pick, 4101)
        } catch (_: Exception) {
            Toast.makeText(this, "파일 선택기를 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated by Android, retained for broad API compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 4101 || resultCode != Activity.RESULT_OK || data == null) return
        images.clear()
        currentIndex = -1
        data.data?.let { images.add(it) }
        addClipData(data)
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                try {
                    contentResolver.takePersistableUriPermission(clip.getItemAt(i).uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {}
            }
        }
        data.data?.let {
            try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
        }
        val unique = images.distinct()
        images.clear()
        images.addAll(unique)
        if (images.isNotEmpty()) showImage(0)
        updateNavigation()
    }

    private fun queryName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) { null }

    private fun updateNavigation() {
        previousButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex >= 0 && currentIndex < images.lastIndex
        previousButton.alpha = if (previousButton.isEnabled) 1f else 0.4f
        nextButton.alpha = if (nextButton.isEnabled) 1f else 0.4f
    }

    private fun applyImmersive() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }
}

private class ZoomImageView(context: android.content.Context) : ImageView(context) {
    private val matrixValue = android.graphics.Matrix()
    private var baseScale = 1f
    private var currentScale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private val scaleDetector = android.view.ScaleGestureDetector(context,
        object : android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: android.view.ScaleGestureDetector): Boolean {
                val next = (currentScale * detector.scaleFactor).coerceIn(baseScale, baseScale * 8f)
                val factor = next / currentScale
                matrixValue.postScale(factor, factor, detector.focusX, detector.focusY)
                currentScale = next
                imageMatrix = matrixValue
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
        setBackgroundColor(Color.BLACK)
        isClickable = true
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        super.setImageBitmap(bitmap)
        post { fitImage() }
    }

    private fun fitImage() {
        val drawable = drawable ?: return
        if (width <= 0 || height <= 0 || drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) return
        val sx = width.toFloat() / drawable.intrinsicWidth
        val sy = height.toFloat() / drawable.intrinsicHeight
        baseScale = minOf(sx, sy)
        currentScale = baseScale
        val dx = (width - drawable.intrinsicWidth * baseScale) / 2f
        val dy = (height - drawable.intrinsicHeight * baseScale) / 2f
        matrixValue.reset()
        matrixValue.setScale(baseScale, baseScale)
        matrixValue.postTranslate(dx, dy)
        imageMatrix = matrixValue
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            android.view.MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragging = true
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                if (dragging && !scaleDetector.isInProgress && currentScale > baseScale * 1.001f) {
                    matrixValue.postTranslate(event.x - lastX, event.y - lastY)
                    imageMatrix = matrixValue
                }
                lastX = event.x
                lastY = event.y
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }
}
