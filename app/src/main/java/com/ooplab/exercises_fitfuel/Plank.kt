package com.ooplab.exercises_fitfuel

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
private var isAudioPlaying = false

var squatCount = 0

class Plank : AppCompatActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var poseLandmarker: PoseLandmarker
    private lateinit var countTextView: TextView
    private lateinit var stageTextView: TextView
    private lateinit var angleTextView: TextView

    private var count = 0
    private var stage: String? = null


    private val repCompleteAudioFiles = listOf(
        R.raw.motivational1, R.raw.motivational2, R.raw.motivational3,
        R.raw.motivational4, R.raw.motivational5, R.raw.motivational6,
        R.raw.motivational7,R.raw.motivational8,R.raw.motivational9,
        R.raw.motivational10,
    )
    private val noActivityAudioFiles = listOf(
        R.raw.no_activity_1, R.raw.no_activity_2, R.raw.no_activity_3,
        R.raw.no_activity_4, R.raw.no_activity_5, R.raw.no_activity_6,
        R.raw.no_activity_7, R.raw.no_activity_8, R.raw.no_activity_9,
        R.raw.no_activity_10
    )
    private val inactivityHandler = Handler(Looper.getMainLooper())

    private fun resetInactivityTimer() {
        inactivityHandler.removeCallbacks(inactivityRunnable)
        inactivityHandler.postDelayed(inactivityRunnable, 10000)
    }

    private val inactivityRunnable = object : Runnable {
        override fun run() {
            if (!isAudioPlaying) {
                playRandomAudio(noActivityAudioFiles)
            }
            inactivityHandler.postDelayed(this, 5000)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var overlayView: OverlayView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_squat)
        setupEdgeToEdge()
        initCameraExecutor()

        previewView = findViewById(R.id.previewCam)
        countTextView = findViewById(R.id.countTextView)
        stageTextView = findViewById(R.id.stageTextView)
        angleTextView = findViewById(R.id.angle1TextView)
        overlayView = findViewById(R.id.overlayView)

        requestCameraPermission()
        resetInactivityTimer()


    }
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initCameraExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        initializePoseLandmarker()
    }


    private fun initializePoseLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                val allLandmarks = result.landmarks()
                if (allLandmarks.isNotEmpty() && allLandmarks[0].isNotEmpty()) {
                    val landmarks = allLandmarks[0]
                    runOnUiThread {
                        overlayView.setLandmarks(landmarks)

                        val hip = landmarks[23]
                        val knee = landmarks[25]
                        val ankle = landmarks[27]

                        if (hip != null && knee != null && ankle != null) {
                            val angleKnee = calculateAngle(
                                hip.x(), hip.y(),
                                knee.x(), knee.y(),
                                ankle.x(), ankle.y()
                            )

                            runOnUiThread {
                                // Angle thresholds for squat
                                if (angleKnee > 160) { // Standing position
                                    if (stage == "Squat") {
                                        // Complete squat detected (down and up)
                                        stage = "Standing"
                                        count++
                                        playRandomAudio(repCompleteAudioFiles)
                                        resetInactivityTimer()


                                    } else {
                                        stage = "Standing"
                                    }
                                } else if (angleKnee < 90 && stage == "Standing") { // Squatting down
                                    stage = "Squat"
                                    resetInactivityTimer()
                                }

                                // Update UI
                                countTextView.text = "Squats: $count"
                                stageTextView.text = "Stage: $stage" // Update stage display
                                angleTextView.text = "Angle: $angleKnee"
                            }


                        }
                    }
                } else {
                    runOnUiThread { overlayView.setLandmarks(mutableListOf()) }
                }
            }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(this, options)
    }


    private fun playRandomAudio(audioFiles: List<Int>) {
        if (isAudioPlaying) return
        mediaPlayer?.release()
        val randomAudio = audioFiles.random()
        mediaPlayer = MediaPlayer.create(this, randomAudio)
        isAudioPlaying = true
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener {
            isAudioPlaying = false
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        var angle = Math.toDegrees(abs(radians).toDouble())
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setupCamera()
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    private fun requestCameraPermission() {
        if (hasCameraPermission()) {
            setupCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply { setAnalyzer(cameraExecutor, ::analyzeImage) }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {
                Log.e("CameraSetup", "Error binding camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && imageProxy.format == ImageFormat.YUV_420_888) {
            val bitmap = yuvToRgb(mediaImage, imageProxy)
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(rotatedBitmap).build()
            val timestamp = imageProxy.imageInfo.timestamp
            poseLandmarker.detectAsync(mpImage, timestamp)
        }
        imageProxy.close()
    }

    private fun yuvToRgb(image: Image, imageProxy: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        mediaPlayer?.release()
        inactivityHandler.removeCallbacks(inactivityRunnable)
    }
}
