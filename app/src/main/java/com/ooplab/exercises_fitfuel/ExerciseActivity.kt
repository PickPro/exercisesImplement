package com.ooplab.exercises_fitfuel

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.ooplab.exercises_fitfuel.Utils.calculateAngle
import com.ooplab.exercises_fitfuel.Utils.calculateAngle360
import com.ooplab.exercises_fitfuel.Utils.viewBinding
import com.ooplab.exercises_fitfuel.Utils.yuvToRgb
import com.ooplab.exercises_fitfuel.databinding.ActivityExerciseBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2


class ExerciseActivity : AppCompatActivity() {
    val binding by viewBinding(ActivityExerciseBinding::inflate)
   
    lateinit var soundManager: SoundManager


    private var lastConditionExecutionTime: Long = 0
    var inKickbackPosition = false

    // Declares a variable for the camera executor service.
    // 'private' means it can only be accessed within this class.
    // 'lateinit var' tells the compiler that the variable will be initialized before use.
    private lateinit var cameraExecutor: ExecutorService


    // Declares a variable for the PoseLandmarker, which detects human poses in images.
    private lateinit var poseLandmarker: PoseLandmarker


    var count = 0
    var stage = "Down"
    //Legraise variables
    var isAt90Degrees = false
    // Plank variables
    var Pose = "Position"
    var timer: CountDownTimer? = null
    var secondsElapsed = 0

    //variable that contains the name of the exercise to play
    var exerciseName = "squats"
    // Create a coroutine scope for the activity
    private val scope = CoroutineScope(Dispatchers.Main)

    // Replace Handler with coroutine delay
    private suspend fun delayedNoActivitySound() {
        delay(10000)
        if (System.currentTimeMillis() - lastConditionExecutionTime >= 5000) {
            soundManager.playNoActivitySound()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        exerciseName = intent.getStringExtra("exerciseName") ?: ""
        soundManager = SoundManager(this)
        binding.exerciseName.text = exerciseName

        enableEdgeToEdge() // Calls a function to adjust the layout for devices with edge-to-edge displays.

        initCameraExecutor() // Initializes the camera executor and pose landmarker.



        requestCameraPermission() // Initiates the process to request camera permission from the user.
    }


    // Initializes the camera executor and pose landmarker.
    private fun initCameraExecutor() {
        // Creates a single-threaded executor for running camera-related tasks asynchronously.
        cameraExecutor = Executors.newSingleThreadExecutor()

        initializePoseLandmarker() // Calls a function to initialize the pose landmarker.

        // Calls a function to initialize the pose landmarker.
    }

    // Sets up the pose landmarker with the necessary options.
    private fun initializePoseLandmarker() {
        // BaseOptions is used to configure the model for the pose landmarker.
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task") // Specifies the path to the model asset file.
            .build()

        // Configures the PoseLandmarker with options.
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions) // Sets the base options with the model path.
            .setRunningMode(RunningMode.LIVE_STREAM) // Sets the running mode to live stream for real-time processing.
            .setResultListener { result, inputImage ->
                // This is a lambda function that gets called when pose detection results are available.
                // 'result' contains the detected pose landmarks.
                // 'inputImage' is the image that was processed.

                // TODO: Implement exercise counting logic here using the landmarks.

                // Ensure that the landmarks list is not empty before accessing the first element.
                val allLandmarks = result.landmarks()

                if (allLandmarks.isNotEmpty() && allLandmarks[0].isNotEmpty()) {

                    when(exerciseName){
                        "Leg Raise" -> exerciseLegRaise(allLandmarks[0])
                        "Kick Back" -> exerciseKickBack(allLandmarks[0])
                        "Jumping Jacks" -> exerciseJumpingJack(allLandmarks[0])
                        "Plank" -> exercisePlank(allLandmarks[0])
                        "Situps" -> exerciseSitups(allLandmarks[0])
                        "Squats" -> exerciseSquats(allLandmarks[0])
                    }



                }else {
                    Log.d("PoseLandmarks", "No landmarks detected.")
                    // Clear the overlay if no landmarks are detected

                }
            }
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(this, options)
    }

    private fun exerciseLegRaise(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks

        // Define points for left and right hips, knees, and shoulders
        val leftHip = landmarks[23]
        val leftKnee = landmarks[25]
        val rightHip = landmarks[24]
        val rightKnee = landmarks[26]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]

// Check if all necessary landmarks are detected
        if (leftHip != null && leftKnee != null &&
            rightHip != null && rightKnee != null &&
            leftShoulder != null && rightShoulder != null
        ) {
            // Calculate hip angles
            val angleLeftHip = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y()
            )

            val angleRightHip = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y()
            )

            // Update UI elements (need to run on main thread)
            runOnUiThread {
                val bothLegsDownCondition = (angleLeftHip > 174) &&
                        (angleRightHip > 174)
                val bothLegsUpCondition =
                    (angleLeftHip >= 85 && angleLeftHip <= 95) &&
                            (angleRightHip >= 85 && angleRightHip <= 95)

                //condition to complete the exercise
                if (count >= 10) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ExerciseActivity,
                            "Exercise completed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    endExercise()
                }

                // Check the stage and conditions for rep counting

                if (bothLegsUpCondition && stage == "Down") {
                    // When both legs reach approximately 90 degrees, set stage to "Up"
                    stage = "Up"
                    soundManager.playUpSound()
                    isAt90Degrees = true
                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }
                } else if (isAt90Degrees && bothLegsDownCondition) {
                    // When both legs go back to approximately 180 degrees from 90 degrees, increment rep count
                    count++
                    stage = "Down"       // Reset stage to "Down" after counting
                    soundManager.playDownSound()
                    isAt90Degrees = false // Reset the 90-degree tracker
                    lastConditionExecutionTime = System.currentTimeMillis()
                    // Reset timer
                    scope.launch {
                        delayedNoActivitySound()
                    }
                }

                // Update TextViews
                binding.countTextView.text = "Reps: $count"
                binding.stageTextView.text = "Stage: $stage"
            }
        }

    }

    private fun exerciseKickBack(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks

        // Define points for shoulders, hips, elbows, knees, and ankles
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        val leftElbow = landmarks[13]
        val rightElbow = landmarks[14]
        val leftKnee = landmarks[25]
        val rightKnee = landmarks[26]
        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]


// Check if all necessary landmarks are detected
        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null &&
            leftElbow != null && rightElbow != null && leftKnee != null && rightKnee != null &&
            leftAnkle != null && rightAnkle != null
        ) {

            // Calculate angles for kickback position
            val angleKneeLeft = calculateAngle360(
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),
                leftAnkle.x(), leftAnkle.y()
            )
            val angleKneeRight = calculateAngle360(
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y(),
                rightAnkle.x(), rightAnkle.y()
            )


            val angleHipRight = calculateAngle360(
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y()
            )
            val angleHipLeft = calculateAngle360(
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),
            )


            // Define conditions for kickback position in the range 130 to 150 degrees
            val kickbackConditionLeft =
                (angleKneeLeft > 130.0 && angleKneeLeft < 191.0)

            val doggyPosition = angleKneeLeft > 50 && angleKneeLeft < 110
            val kickbackConditionRight =
                (angleKneeRight > 210.0 && angleKneeRight < 270.0)
            val hipCondition = (angleHipLeft > 120.0 && angleHipLeft < 186.0)
            //condition to complete the exercise
            if (count >= 10) {
                runOnUiThread {
                    Toast.makeText(
                        this@ExerciseActivity,
                        "Exercise completed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                endExercise()
            }

// Check if both kickback conditions are met and update the state
            if (kickbackConditionLeft && hipCondition) {
                // Set flag indicating that both legs are in the kickback position
                inKickbackPosition = true
                soundManager.playUpSound()
                // Reset the handler whenever activity is detected
                lastConditionExecutionTime = System.currentTimeMillis()
                scope.launch {
                    delayedNoActivitySound()
                }
            } else if (inKickbackPosition && doggyPosition) {
                // Only increment count if transitioning from kickback position to normal position
                count++
                inKickbackPosition = false // Reset for the next rep cycle
                soundManager.playDownSound()
                // Reset the handler whenever activity is detected
                lastConditionExecutionTime = System.currentTimeMillis()
                scope.launch {
                    delayedNoActivitySound()
                }
            }

// Update TextViews on the main thread
            runOnUiThread() {
               binding. countTextView.text = "Reps: $count"
               binding. stageTextView.text =
                    if (inKickbackPosition) "In Kickback Position" else "Down"

            }
        }


    }

    private fun exerciseJumpingJack(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks


        // Define points for left and right hips, knees, shoulders, and ankles
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]
        val leftElbow = landmarks[13]
        val rightElbow = landmarks[14]
        val leftKnee = landmarks[25]
        val rightKnee = landmarks[26]

        // Check if all necessary landmarks are detected
        if (leftHip != null && rightHip != null &&
            leftShoulder != null && rightShoulder != null &&
            leftAnkle != null && rightAnkle != null &&
            leftElbow != null && rightElbow != null
        ) {
            // Calculate shoulder and hip angles
            val angleLeftShoulder = calculateAngle(
                leftElbow.x(), leftElbow.y(),
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y()
            )

            val angleRightShoulder = calculateAngle(
                rightElbow.x(), rightElbow.y(),
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y()
            )
            val angleLeftLeg = calculateAngle(
                rightHip.x(), rightHip.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),

                )

            val angleRightLeg = calculateAngle(
                leftHip.x(), leftHip.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y(),
            )


            // Define angle conditions
            val armsUpCondition =
                (angleLeftShoulder > 145 && angleLeftShoulder < 170) ||
                        (angleRightShoulder > 145 && angleRightShoulder < 170)
            val legsOutCondition = (angleLeftLeg > 95) ||
                    (angleRightLeg > 95)

            val armsDownCondition =
                (angleLeftShoulder > 10 && angleLeftShoulder < 40) ||
                        (angleRightShoulder > 10 && angleRightShoulder < 40)
            val legsInCondition = (angleLeftLeg > 80 && angleLeftLeg < 95) ||
                    (angleRightLeg > 80 && angleRightLeg < 95)

            //condition to complete the exercise
            if (count >= 3) {
                runOnUiThread {
                    Toast.makeText(
                        this@ExerciseActivity,
                        "Exercise completed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                endExercise()
            }

            runOnUiThread() {

                // Check the stage and conditions for rep counting
                if (armsUpCondition && legsOutCondition) {
                    // When both arms and legs reach the "Up" position, set stage to "Up"
                    //
                    stage = "Up"
                    soundManager. playUpSound()

                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }

                } else if (stage == "Up" && armsDownCondition && legsInCondition) {
                    // When both arms and legs go back to the "Down" position from "Up", increment rep count
                    count++
                    stage = "Down"       // Reset stage to "Down" after counting
                    soundManager. playDownSound()

                    lastConditionExecutionTime = System.currentTimeMillis()

                    scope.launch {
                        delayedNoActivitySound()
                    }
                }
                // Update TextViews
               binding. countTextView.text = "Reps: $count"
               binding. stageTextView.text = "Stage: $stage"
            }
        }
    }


    private fun exercisePlank(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        val leftElbow = landmarks[13]
        val rightElbow = landmarks[14]
        val leftKnee = landmarks[25]
        val rightKnee = landmarks[26]
        val leftWrist = landmarks[15]
        val rightWrist = landmarks[16]

        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null &&
            leftElbow != null && rightElbow != null && leftKnee != null && rightKnee != null) {

            val angleElbowLeft = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftElbow.x(), leftElbow.y(),
                leftWrist.x(), leftWrist.y()
            )

            val angleElbowRight = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightElbow.x(), rightElbow.y(),

                rightWrist.x(), rightWrist.y()
            )

            val angleLeftHip = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y()
            )

            val angleRightHip = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y()
            )

            val isPlankPose = (angleLeftHip >= 160 ) &&
                    (angleRightHip >= 160 ) &&
                    (angleElbowLeft >= 70 && angleElbowLeft <= 115) &&
                    (angleElbowRight >= 70 && angleElbowRight <= 115)

            if (isPlankPose) {
                if (Pose != "Plank position") {
                    soundManager.playPlankSound()
                    Pose = "Plank position"
                    runOnUiThread {
                        timer?.cancel()
                        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
                            override fun onTick(millisUntilFinished: Long) {
                                secondsElapsed++
                                //condition to complete the exercise
                                if (secondsElapsed >= 60) {
                                    timer?.cancel()
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@ExerciseActivity,
                                            "Exercise completed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    endExercise()
                                }
                            }



                            override fun onFinish() {}
                        }.start()

                    }
                }
                else {
                    if (Pose == "Plank position") {
                        Pose = "Not Plank Pose"
                        timer?.cancel()

                    }
                }

                runOnUiThread() {

                   binding. countTextView.text  = "Time: $secondsElapsed"
                   binding. stageTextView.text = "Position: $Pose"

                }
            } else {

                soundManager.playNotPlankSound()
            }
        }
    }

    private fun exerciseSitups(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks

        // Define points for shoulders, hips, and knees
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftHip = landmarks[23]
        val rightHip = landmarks[24]
        val leftKnee = landmarks[25]
        val rightKnee = landmarks[26]
        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]

// Check if all necessary landmarks are detected
        if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null &&
            leftKnee != null && rightKnee != null) {

            // Calculate the angle between torso (shoulder to hip) and legs (hip to knee)
            val angleTorsoToLegsLeft = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y()
            )

            val angleTorsoToLegsRight = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y()
            )
            val angleKneeLeft = calculateAngle(
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),
                leftAnkle.x(), leftAnkle.y()
            )
            val angleKneeRight = calculateAngle(
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y(),
                rightAnkle.x(), rightAnkle.y()
            )

            // Determine the average angle for a more stable measurement


            // Define conditions for "up" and "down" positions
            val isDownPosition = angleTorsoToLegsLeft > 125.0 || angleTorsoToLegsRight >125.0
            val isUpPosition = (angleTorsoToLegsLeft < 55.0 && angleTorsoToLegsLeft >30) || angleTorsoToLegsRight < 55.0 && angleTorsoToLegsRight >30


            val isKneeDown = (angleKneeLeft > 29.0 && angleKneeLeft < 111.0) || (angleKneeRight > 29.0 && angleKneeRight<111.0)

            //condition to complete the exercise
            if (count >= 10) {
                runOnUiThread {
                    Toast.makeText(
                        this@ExerciseActivity,
                        "Exercise completed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                endExercise()
            }

            // Track sit-up movement
            if (isDownPosition && isKneeDown) {
                if (stage== "Up")
                {
                    count++ // Increment sit-up count
                    soundManager.playUpSound()
                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }
                }
                // Starting the sit-up from the "down" position
                stage = "Down"


            } else if (isUpPosition && stage == "Down") {
                // Transitioning to the "up" position, count a rep
                stage = "Up"
                soundManager.playDownSound()
                // Reset the handler whenever activity is detected
                lastConditionExecutionTime = System.currentTimeMillis()
                scope.launch {
                    delayedNoActivitySound()
                }

            }

            // Update TextViews on the main thread
            runOnUiThread {
              binding.  countTextView.text = "Reps: $count"
              binding.  stageTextView.text = "Stage: $stage"
            }
        }


    }

    private fun exerciseSquats(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks

        val leftHip = landmarks[23]
        val leftKnee = landmarks[25]
        val leftAnkle = landmarks[27]
        val rightHip = landmarks[24]
        val rightKnee = landmarks[26]
        val rightAnkle = landmarks[28]

        if (leftHip != null && leftKnee != null && leftAnkle != null &&
            rightHip != null && rightKnee != null && rightAnkle != null ) {

            // Calculate angles for left and right legs
            val angleLeftKnee = calculateAngle(
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),
                leftAnkle.x(), leftAnkle.y()
            )

            val angleRightKnee = calculateAngle(
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y(),
                rightAnkle.x(), rightAnkle.y()
            )

            //condition to complete the exercise
            if (count >= 4) {
                runOnUiThread {
                    Toast.makeText(
                        this@ExerciseActivity,
                        "Exercise completed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                endExercise()
            }



            // Update stage and rep count
            if (((angleLeftKnee > 160.0) && (angleRightKnee > 160.0))) {
                if (stage=="Down")
                {// Increment count after completing a full squat cycle
                    count++
                }

                stage = "Up"
                soundManager. playUpSound()

                lastConditionExecutionTime = System.currentTimeMillis()

                scope.launch {
                    delayedNoActivitySound()
                }


            } else if (((angleLeftKnee < 100.0) && (angleRightKnee < 100.0)) ) {
                // Transition from "Down" to "Up" when user comes back up
                stage = "Down"
                soundManager. playDownSound()

                lastConditionExecutionTime = System.currentTimeMillis()

                scope.launch {
                    delayedNoActivitySound()
                }

            }

            runOnUiThread() {
                // Display rep count and angles for debugging
               binding. stageTextView.text = "Stage: $stage"
               binding. countTextView.text = "Reps: $count"
            }

        }

    }
    private fun endExercise() {
        runOnUiThread {
            // Show the completion dialog
            val dialogView = layoutInflater.inflate(R.layout.exercise_completed_dialog, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialogView.findViewById<Button>(R.id.okButton).setOnClickListener {
                dialog.dismiss()
                finish() // Ends the activity
            }

            dialog.show()

            // Close pose landmarker and shutdown camera executor
            poseLandmarker.close()
            cameraExecutor.shutdown()
        }
    }

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        // Registers for activity result with a contract for requesting permissions.
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // This lambda function is called with 'granted' indicating whether permission was granted.
            if (granted) {
                setupCamera() // If permission is granted, proceed to set up the camera.
            } else {
                // If permission is denied, show a toast message to the user.
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    // Requests camera permission if it hasn't been granted yet.
    private fun requestCameraPermission() {
        if (hasCameraPermission()) {
            setupCamera() // If permission is already granted, set up the camera.
        } else {
            // Launches the permission request dialog.
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Checks if the app already has camera permission.
    private fun hasCameraPermission(): Boolean {
        // Uses 'ContextCompat.checkSelfPermission' to check permission status.
        // Returns true if permission is granted, false otherwise.
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Sets up the camera preview and image analysis pipeline.
    private fun setupCamera() {
        // Gets a future that resolves to a ProcessCameraProvider, which provides access to the camera.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Adds a listener that runs when the camera provider is available.
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get() // Retrieves the camera provider.

            // Configures the camera preview use case.
            val preview = Preview.Builder()
                .build()
                .apply {
                    // Sets the surface provider where the camera preview will be displayed.
                    setSurfaceProvider(binding.previewCam.surfaceProvider)
                }

            // Configures the image analysis use case.
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // Sets the backpressure strategy to discard old frames if the analyzer is busy.
                .build()
                .apply {
                    // Sets the analyzer that processes each frame.
                    // 'setAnalyzer' accepts an executor and an analyzer function.
                    setAnalyzer(cameraExecutor, ::analyzeImage)
                }

            try {
                cameraProvider.unbindAll() // Unbinds any previously bound use cases.

                // Binds the camera to the lifecycle with the specified use cases.
                cameraProvider.bindToLifecycle(
                    this, // LifecycleOwner, which is MainActivity in this case.
                    CameraSelector.DEFAULT_FRONT_CAMERA, // Selects the front-facing camera.
                    preview, // Adds the preview use case.
                    imageAnalyzer // Adds the image analysis use case.
                )
            } catch (e: Exception) {
                // Logs any exceptions that occur during camera setup.
                Log.e("CameraSetup", "Error binding camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(this)) // Ensures that the listener runs on the main thread.
    }


    // Called when the activity is destroyed to clean up resources.
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        cameraExecutor.shutdown() // Shuts down the executor service.
        poseLandmarker.close() // Closes the pose landmarker to release resources.
    }


    // Indicates that we are using an experimental API (getImage()).
    @OptIn(ExperimentalGetImage::class)
    // Analyzes each frame captured by the camera.
    fun analyzeImage( imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image // Gets the underlying media image.

        // Checks if the image is not null and is in the correct format.
        if (mediaImage != null && imageProxy.format == ImageFormat.YUV_420_888) {
            // Converts the YUV image to an RGB bitmap.
            val bitmap = yuvToRgb(mediaImage, imageProxy)

            // Creates a matrix to apply transformations to the bitmap.
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) // Rotates the image based on its rotation degrees.

                // Mirrors the image horizontally to correct the front camera preview.
                postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
            }

            // Creates a new bitmap with the applied transformations.
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, // Source bitmap.
                0, 0, // Starting coordinates.
                bitmap.width, bitmap.height, // Dimensions.
                matrix, // Transformation matrix.
                true // Indicates whether to filter the bitmap.
            )

            // Builds a MediaPipe Image from the rotated bitmap.
            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(rotatedBitmap).build()

            val timestamp = imageProxy.imageInfo.timestamp // Gets the timestamp of the frame.

            // Performs asynchronous pose detection on the image.
            poseLandmarker.detectAsync(mpImage, timestamp)

            imageProxy.close() // Closes the image proxy to allow the next frame to be processed.
        } else {
            // Logs an error if the image format is unsupported.
            Log.e("AnalyzeImage", "Unsupported image format")
            imageProxy.close() // Closes the image proxy.
        }
    }

    // Calculates the angle between three points
    private fun calculateAngleKickBack(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        val angle = Math.toDegrees(abs(radians).toDouble())
        return angle
    }


}

