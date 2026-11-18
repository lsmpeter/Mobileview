package com.monterto.imageviewer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.view.ScaleGestureDetector
import android.view.GestureDetector
import android.view.ViewGroup
import android.graphics.Matrix
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var canvas: ZoomImageView
    private lateinit var label: TextView
    private lateinit var openButton: TextView
    private var controlsVisible = true

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
        openButton = TextView(this).apply {
            text = "이미지 열기"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(28, 16, 28, 16)
            setBackgroundColor(0xCCB00020.toInt())
            setOnClickListener { launchPicker() }
        }
        root.addView(openButton, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = 28 })
        setContentView(root)
        handleIntent(intent)
        applyImmersive()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_SEND -> if (android.os.Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java) else @Suppress("DEPRECATION") (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
            else -> intent?.data
        }
        if (uri == null) {
            label.text = "Monterto Image Viewer"
            canvas.setImageBitmap(null)
            return
        }
        try {
            contentResolver.openInputStream(uri).use { stream ->
                if (stream == null) throw IllegalStateException("이미지를 읽을 수 없습니다")
                val bitmap = BitmapFactory.decodeStream(stream) ?: throw IllegalArgumentException("지원하지 않는 이미지입니다")
                canvas.setImageBitmap(bitmap)
            }
            label.text = queryName(uri) ?: "Monterto Image Viewer"
        } catch (e: Exception) {
            Toast.makeText(this, "이미지를 열 수 없습니다: ${e.localizedMessage ?: "오류"}", Toast.LENGTH_LONG).show()
            label.text = "이미지를 열 수 없습니다"
        }
    }

    private fun launchPicker() {
        val pick = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivityForResult(pick, 4101) }
        catch (_: Exception) { Toast.makeText(this, "파일 선택기를 열 수 없습니다", Toast.LENGTH_SHORT).show() }
    }

    @Deprecated("Deprecated by Android, retained for broad API compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4101 && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data!!
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            handleIntent(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun queryName(uri: Uri): String? = try {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    } catch (_: Exception) { null }

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
    private val matrixValues = FloatArray(9)
    private val imageMatrixValue = Matrix()
    private var baseScale = 1f
    private var currentScale = 1f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val next = (currentScale * detector.scaleFactor).coerceIn(baseScale, baseScale * 8f)
            val factor = next / currentScale
            imageMatrixValue.postScale(factor, factor, detector.focusX, detector.focusY)
            currentScale = next
            imageMatrix = imageMatrixValue
            return true
        }
    })

    init {
        scaleType = ScaleType.MATRIX
        setBackgroundColor(Color.BLACK)
        isClickable = true
    }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        post { fitImage() }
    }

    private fun fitImage() {
        val d = drawable ?: return
        if (width <= 0 || height <= 0 || d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return
        val sx = width.toFloat() / d.intrinsicWidth
        val sy = height.toFloat() / d.intrinsicHeight
        baseScale = min(sx, sy)
        currentScale = baseScale
        val dx = (width - d.intrinsicWidth * baseScale) / 2f
        val dy = (height - d.intrinsicHeight * baseScale) / 2f
        imageMatrixValue.reset()
        imageMatrixValue.setScale(baseScale, baseScale)
        imageMatrixValue.postTranslate(dx, dy)
        imageMatrix = imageMatrixValue
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; dragging = true }
            MotionEvent.ACTION_MOVE -> if (dragging && !scaleDetector.isInProgress && currentScale > baseScale * 1.001f) {
                val dx = event.x - lastX
                val dy = event.y - lastY
                imageMatrixValue.postTranslate(dx, dy)
                imageMatrix = imageMatrixValue
                lastX = event.x; lastY = event.y
            } else { lastX = event.x; lastY = event.y }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return true
    }
}
