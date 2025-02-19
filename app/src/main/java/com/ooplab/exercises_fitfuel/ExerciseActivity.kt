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
import kotlin.math.sqrt
import kotlin.math.pow


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

    private var timer: CountDownTimer? = null
    private var secondsElapsed = 0
    private var isTimerRunning = false

    //variable that contains the name of the exercise to play
    var exerciseName = "squats"

    //cobra exercise
    private var lastValidTime: Long = 0L
    private var cobraSecondsElapsed = 0
    private var cobraSetCount = 0
    private var readyForCobra = false

    //relieving pose
    private var relievingPoseHoldTime: Long = 0L
    private var lastRelievingTime: Long = 0L
    private var isRelievingPoseActive: Boolean = false
    private var currentPose: String = "Not Relieving pose"

    //sunSalutationState
    // State variable and pose sequence definition.
    private var sunSalutationState: Int = 0
    private val sunSalutationPoses = arrayOf(
        "Mountain Pose",
        "Upward Salute",
        "Forward Bend",
        "Plank Pose",
        "Chaturanga",
        "Upward Facing Dog",
        "Downward Facing Dog",
        "Mountain Pose"
    )
    private var currentSunPose: String = sunSalutationPoses[0]


    // Create a coroutine scope for the activity
    private val scope = CoroutineScope(Dispatchers.Main)

    // Replace Handler with coroutine delay
    private suspend fun delayedNoActivitySound() {
        val job = scope.launch {
            delay(10000)
            if (System.currentTimeMillis() - lastConditionExecutionTime >= 5000) {
                soundManager.playNoActivitySound()
            }
        }
        soundManager.setNoActivityJob(job)
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

                    // stage is Hips down

                    if (exerciseName == "Glute Bridge")
                    {
                        stage = "Hips Down"
                    }
                    when(exerciseName){
                        "Leg Raise" -> exerciseLegRaise(allLandmarks[0])
                        "Kick Back" -> exerciseKickBack(allLandmarks[0])
                        "Jumping Jacks" -> exerciseJumpingJack(allLandmarks[0])
                        "Plank" -> exercisePlank(allLandmarks[0])
                        "Situps" -> exerciseSitups(allLandmarks[0])
                        "Squats" -> exerciseSquats(allLandmarks[0])
                        "Glute Bridge" -> exerciseGluteBridge(allLandmarks[0])
                        "Push Ups" -> exercisePushUp(allLandmarks[0])
                        "Pull Ups" -> exercisePullUp(allLandmarks[0])
                        "Cobra Pose"         -> exerciseCobraPose(allLandmarks[0])
                        "Relieving Pose"     -> exerciseRelievingPose(allLandmarks[0])
                        "Tree Pose"          -> exerciseTreePose(allLandmarks[0])
                        "Sun Salutation"     -> exerciseSunSalutation(allLandmarks[0])
                        "Chair Pose"         -> exerciseChairPose(allLandmarks[0])
                        "Mountain Pose"      -> exerciseMountainPose(allLandmarks[0])
                        "Easy Pose"          -> exerciseEasyPose(allLandmarks[0])
                        "Boat Pose"          -> exerciseBoatPose(allLandmarks[0])
                        "Cat Cow Pose"       -> exerciseCatCowPose(allLandmarks[0])
                        "Bow Pose"           -> exerciseBowPose(allLandmarks[0])
                        "Downward Facing Dog"-> exerciseDownwardFacingDogPose(allLandmarks[0])
                        "Triangle Pose"      -> exerciseTrianglePose(allLandmarks[0])
                        "Warrior 2"          -> exerciseWarrior2Pose(allLandmarks[0])
                        "Child Pose"         -> exerciseChildPose(allLandmarks[0])
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
                if (count >= 5) {
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

                if (bothLegsUpCondition && stage == "Legs Down") {
                    // When both legs reach approximately 90 degrees, set stage to "Up"
                    stage = "Legs Up"
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
                    stage = "Legs Down"       // Reset stage to "Down" after counting
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
                binding.stageTextView.text = "Position: $stage"
            }
        }

    }

    private fun exerciseKickBack(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract required landmarks for the kicking leg.
        val leftShoulder = firstPersonLandmarks[11]
        val leftHip = firstPersonLandmarks[23]
        val leftKnee = firstPersonLandmarks[25]
        val leftAnkle = firstPersonLandmarks[27]

        // Ensure all required landmarks are available.
        if (leftShoulder == null || leftHip == null || leftKnee == null || leftAnkle == null) {
            return
        }

        // Calculate angles for the left leg:
        // 1. Hip angle: formed by left shoulder, left hip, and left knee.
        val leftHipAngle = calculateAngle(
            leftShoulder.x(), leftShoulder.y(),
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y()
        )

        // 2. Knee angle: formed by left hip, left knee, and left ankle.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )

        // Define thresholds:
        // The left hip angle should be greater than 110° to indicate sufficient hip extension.
        val hipExtensionThreshold = 110.0

        // For a proper kick, the knee should remain bent:
        // For example, a knee angle between 60° and 130° is considered correct.
        val kneeAngleMin = 60.0
        val kneeAngleMax = 130.0

        val isHipExtended = leftHipAngle > hipExtensionThreshold
        val isKneeBentCorrectly = leftKneeAngle in kneeAngleMin..kneeAngleMax

        // Only count as a valid kickback if both conditions are met.
        val kickbackDetected = isHipExtended && isKneeBentCorrectly

        if (kickbackDetected) {
            if (currentPose != "Kickback Exercise") {
                currentPose = "Kickback Exercise"
                soundManager.playUpSound()  // Trigger auditory feedback.
            }
        } else {
            if (currentPose == "Kickback Exercise") {
                currentPose = "Not Kickback Exercise"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
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
            if (count >= 5) {
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
                    stage = "In"
                    soundManager. playJumpingJackInSound()

                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }

                } else if (stage == "In" && armsDownCondition && legsInCondition) {
                    // When both arms and legs go back to the "Down" position from "Up", increment rep count
                    count++
                    stage = "Out"       // Reset stage to "Down" after counting
                    soundManager. playJumpingJackOutSound()

                    lastConditionExecutionTime = System.currentTimeMillis()

                    scope.launch {
                        delayedNoActivitySound()
                    }
                }
                // Update TextViews
               binding. countTextView.text = "Reps: $count"
               binding. stageTextView.text = "Position: $stage"
            }
        }
    }

    private fun exercisePlank(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
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

            val isPlankPose = ((angleLeftHip >= 155) &&
                    (angleRightHip >= 155)) &&
                    ((angleElbowLeft >= 70 && angleElbowLeft <= 115) ||
                            (angleElbowRight >= 70 && angleElbowRight <= 115))

            if (isPlankPose) {

                Pose = "Plank position"
                soundManager.playPlankSound()

                if (!isTimerRunning) {
                    startTimer()
                }
            } else {
                if (Pose == "Plank position") {
                    Pose = "Not Plank Pose"
                    soundManager.playPlankSound()
                }
                pauseTimer()
            }

            runOnUiThread {
                binding.countTextView.text = "Time: $secondsElapsed"
                binding.stageTextView.text = "Position: $Pose"
            }
        }
    }

    private fun startTimer() {
        if (isTimerRunning) return // Prevent restarting if already running

        isTimerRunning = true
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsElapsed++
                if (secondsElapsed >= 60) {
                    stopTimer()
                    runOnUiThread {
                        Toast.makeText(this@ExerciseActivity, "Exercise completed", Toast.LENGTH_SHORT).show()
                    }
                    endExercise()
                }
            }

            override fun onFinish() {}
        }.start()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isTimerRunning = false
    }

    private fun stopTimer() {
        timer?.cancel()
        secondsElapsed = 0
        isTimerRunning = false
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
            if (count >= 5) {
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

    //Strength Exercises
    private fun exerciseGluteBridge(firstPersonLandmarks: MutableList<NormalizedLandmark>)
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
        val leftAnkle = landmarks[27]
        val rightAnkle = landmarks[28]

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
            // Calculate hip angles
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


            // Update UI elements (need to run on main thread)
            runOnUiThread {
                val bothLegsDownCondition = ((angleLeftHip > 115 &&angleLeftHip<125) &&
                        (angleRightHip > 115 &&angleRightHip<125)) && ((angleLeftKnee > 25 && angleLeftKnee<35) && (angleRightKnee > 25 && angleRightKnee<35))
                val bothLegsUpCondition =
                    ((angleLeftHip > 174) &&
                            (angleRightHip > 174)) && ((angleLeftKnee > 75 && angleLeftKnee<85) && (angleRightKnee > 75 && angleRightKnee<85))

                //condition to complete the exercise
                if (count >= 5) {
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

                if (bothLegsUpCondition && stage == "Hips Down") {
                    // When both legs reach approximately 90 degrees, set stage to "Up"
                    stage = "Hips Up"
                    soundManager.playUpSound()
                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }
                } else if (stage == "Hips Up" && bothLegsDownCondition) {
                    // When both legs go back to approximately 180 degrees from 90 degrees, increment rep count
                    count++
                    stage = "Hips Down"       // Reset stage to "Down" after counting
                    soundManager.playDownSound()
                    lastConditionExecutionTime = System.currentTimeMillis()
                    // Reset timer
                    scope.launch {
                        delayedNoActivitySound()
                    }
                }

                // Update TextViews
                binding.countTextView.text = "Reps: $count"
                binding.stageTextView.text = "Position: $stage"
            }
        }

    }

    //pushups
    private fun exercisePushUp(firstPersonLandmarks: MutableList<NormalizedLandmark>)
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
        val leftElbow= landmarks[13]
        val rightElbow = landmarks[14]
        val leftFist= landmarks[15]
        val rightFist = landmarks[16]

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
            val angleLeftElbow = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftElbow.x(), leftElbow.y(),
                leftFist.x(), leftFist.y()
            )

            val angleRightElbow = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightElbow.x(), rightElbow.y(),
                rightFist.x(), rightFist.y()
            )

            // Update UI elements (need to run on main thread)
            runOnUiThread {
                val bothLegsDownCondition =
                    ((angleLeftElbow >= 85 && angleLeftElbow <= 95) &&
                        (angleRightElbow >= 85 && angleRightElbow <= 95)) && ((angleLeftHip > 174) &&
                        (angleRightHip > 174))
                val bothLegsUpCondition =
                    ((angleLeftElbow > 174) &&
                            (angleRightElbow > 174)) && ((angleLeftHip > 174) &&
                        (angleRightHip > 174))

                //condition to complete the exercise
                if (count >= 5) {
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

                if (bothLegsUpCondition && stage == "Legs Down") {
                    // When both legs reach approximately 90 degrees, set stage to "Up"
                    stage = "Legs Up"
                    soundManager.playUpSound()
                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }
                } else if (isAt90Degrees && bothLegsDownCondition) {
                    // When both legs go back to approximately 180 degrees from 90 degrees, increment rep count
                    count++
                    stage = "Legs Down"       // Reset stage to "Down" after counting
                    soundManager.playDownSound()
                    lastConditionExecutionTime = System.currentTimeMillis()
                    // Reset timer
                    scope.launch {
                        delayedNoActivitySound()
                    }
                }

                // Update TextViews
                binding.countTextView.text = "Reps: $count"
                binding.stageTextView.text = "Position: $stage"
            }
        }

    }

    private fun exercisePullUp(firstPersonLandmarks: MutableList<NormalizedLandmark>)
    {

        // Access the first set of landmarks (for the first detected person)
        val landmarks = firstPersonLandmarks

        // Define points for left and right hips, knees, and shoulders
        val leftShoulder = landmarks[11]
        val rightShoulder = landmarks[12]
        val leftElbow= landmarks[13]
        val rightElbow = landmarks[14]
        val leftFist= landmarks[15]
        val rightFist = landmarks[16]

// Check if all necessary landmarks are detected
        if (leftElbow != null && leftFist != null &&
            rightElbow != null && rightFist != null &&
            leftShoulder != null && rightShoulder != null
        ) {
            // Calculate angles

            val angleLeftElbow = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftElbow.x(), leftElbow.y(),
                leftFist.x(), leftFist.y()
            )

            val angleRightElbow = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightElbow.x(), rightElbow.y(),
                rightFist.x(), rightFist.y()
            )

            // Update UI elements (need to run on main thread)
            runOnUiThread {
                val bothLegsDownCondition =
                    ((angleLeftElbow > 164) && (angleRightElbow > 164))

                val bothLegsUpCondition =
                    ((angleLeftElbow > 29 && angleLeftElbow < 36) && (angleRightElbow > 29 && angleRightElbow < 36))

                //condition to complete the exercise
                if (count >= 5) {
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

                if (bothLegsUpCondition && stage == "Legs Down") {
                    // When both legs reach approximately 90 degrees, set stage to "Up"
                    stage = "Legs Up"
                    soundManager.playUpSound()
                    // Reset the handler whenever activity is detected
                    lastConditionExecutionTime = System.currentTimeMillis()
                    scope.launch {
                        delayedNoActivitySound()
                    }
                } else if (isAt90Degrees && bothLegsDownCondition) {
                    // When both legs go back to approximately 180 degrees from 90 degrees, increment rep count
                    count++
                    stage = "Legs Down"       // Reset stage to "Down" after counting
                    soundManager.playDownSound()
                    lastConditionExecutionTime = System.currentTimeMillis()
                    // Reset timer
                    scope.launch {
                        delayedNoActivitySound()
                    }
                }

                // Update TextViews
                binding.countTextView.text = "Reps: $count"
                binding.stageTextView.text = "Position: $stage"
            }
        }

    }

// Yoga
// Underweight exercises
    
    private fun exerciseCobraPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract relevant Mediapipe landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftElbow = firstPersonLandmarks[13]
        val rightElbow = firstPersonLandmarks[14]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]

        // Ensure all required landmarks are available.
        if (leftShoulder != null && rightShoulder != null &&
            leftElbow != null && rightElbow != null &&
            leftWrist != null && rightWrist != null &&
            leftHip != null && rightHip != null &&
            leftKnee != null && rightKnee != null) {

            // Compute elbow angles.
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

            // Compute hip angles.
            val angleHipLeft = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y()
            )
            val angleHipRight = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y()
            )

            // Define the Cobra pose (up) condition.
            // Arms should be moderately bent and at least one hip must show an arch.
            val isCobraPose = ((angleElbowLeft in 80.0..120.0) || (angleElbowRight in 80.0..120.0)) &&
                    ((angleHipLeft < 150) || (angleHipRight < 150))

            // Define the Down position condition.
            // Arms nearly extended and the body flat.
            val isDownPosition = (angleElbowLeft > 140.0 && angleElbowRight > 140.0) &&
                    (angleHipLeft > 160.0 && angleHipRight > 160.0)

            val currentTime = System.currentTimeMillis()

            if (isDownPosition) {
                // User is in the down position.
                if (Pose != "Down position") {
                    Pose = "Down position"
                }
                // Allow the subsequent Cobra pose to be counted.
                readyForCobra = true
                // Reset timer reference to ensure fresh timing when moving up.
                lastValidTime = 0L
            } else if (isCobraPose && readyForCobra) {
                // User is in Cobra (up) pose and had previously been down.
                if (Pose != "Cobra pose") {
                    Pose = "Cobra pose"
                }
                if (lastValidTime == 0L) {
                    lastValidTime = currentTime
                } else {
                    val elapsed = currentTime - lastValidTime
                    if (elapsed >= 1000) {
                        val secondsPassed = (elapsed / 1000).toInt()
                        cobraSecondsElapsed += secondsPassed
                        lastValidTime += secondsPassed * 1000

                        // If 60 seconds of valid Cobra pose are accumulated, complete a set.
                        if (cobraSecondsElapsed >= 60) {
                            cobraSetCount++
                            runOnUiThread {
                                Toast.makeText(this@ExerciseActivity,
                                    "Set $cobraSetCount complete", Toast.LENGTH_SHORT).show()
                            }
                            if (cobraSetCount >= 2) {
                                runOnUiThread {
                                    Toast.makeText(this@ExerciseActivity,
                                        "Exercise completed", Toast.LENGTH_SHORT).show()
                                }
                                endExercise()
                            } else {
                                // Prepare for next set: reset counter and require a new down position.
                                cobraSecondsElapsed = 0
                                lastValidTime = currentTime
                                readyForCobra = false
                            }
                        }
                    }
                }
            } else {
                // If neither valid down nor valid Cobra pose, update the pose state.
                if (Pose == "Cobra pose" || Pose == "Down position") {
                    Pose = "Not Cobra pose"
                    //soundManager.playUpSound()
                }
                // Reset timer reference.
                lastValidTime = 0L
            }

            // Update UI.
            runOnUiThread {
                binding.countTextView.text = "Time: $cobraSecondsElapsed s"
                binding.stageTextView.text = "Set: ${cobraSetCount + 1}/2 | Pose: $Pose"
            }
        }
    }

//    private fun startCobraTimer() {
//        if (isCobraTimerRunning) return
//
//        isCobraTimerRunning = true
//        cobraTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                cobraSecondsElapsed++
//                if (cobraSecondsElapsed >= 60) {
//                    // Complete the set.
//                    cobraSetCount++
//                    pauseCobraTimer()
//                    runOnUiThread {
//                        Toast.makeText(this@ExerciseActivity, "Set $cobraSetCount complete", Toast.LENGTH_SHORT).show()
//                    }
//                    // Reset seconds counter for the next set.
//                    cobraSecondsElapsed = 0
//                }
//            }
//            override fun onFinish() {}
//        }.start()
//    }
//
//    private fun pauseCobraTimer() {
//        cobraTimer?.cancel()
//        isCobraTimerRunning = false
//    }
//
//    private fun stopCobraTimer() {
//        cobraTimer?.cancel()
//        cobraSecondsElapsed = 0
//        isCobraTimerRunning = false
//    }



    private fun exerciseRelievingPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract head-related landmarks.
        val leftEye = firstPersonLandmarks[2]
        val rightEye = firstPersonLandmarks[5]

        // Extract shoulder, elbow, wrist, hip, and knee landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftElbow = firstPersonLandmarks[13]
        val rightElbow = firstPersonLandmarks[14]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]

        if (leftEye != null && rightEye != null &&
            leftShoulder != null && rightShoulder != null &&
            leftElbow != null && rightElbow != null &&
            leftWrist != null && rightWrist != null &&
            leftHip != null && rightHip != null &&
            leftKnee != null && rightKnee != null) {

            // Compute elbow angles.
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

            // Compute hip angles.
            val angleHipLeft = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y()
            )
            val angleHipRight = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y()
            )

            // Compute the average y-coordinate of the eyes.
            val averageEyeY = (leftEye.y() + rightEye.y()) / 2.0

            // Compute the midpoint of the shoulder line.
            val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0

            // Define a margin for head lowering.
            val headMargin = 0.05

            // Head is considered lowered if eyes are below shoulders by the margin.
            val isHeadDown = averageEyeY > (midShoulderY + headMargin)

            // Relieving pose criteria:
            // 1. Forward bend: at least one hip angle below 110°.
            // 2. Both arms extended: elbow angles above 150°.
            // 3. Head is lowered: as determined by eye position.
            val isRelievingPose = ((angleHipLeft < 110) || (angleHipRight < 110)) &&
                    (angleElbowLeft > 150 && angleElbowRight > 150) &&
                    isHeadDown

            val currentTime = System.currentTimeMillis()

            if (isRelievingPose) {
                if (!isRelievingPoseActive) {
                    isRelievingPoseActive = true
                    lastRelievingTime = currentTime
                    currentPose = "Relieving pose"
                    soundManager.playUpSound()
                } else {
                    val elapsed = currentTime - lastRelievingTime
                    relievingPoseHoldTime += elapsed
                    lastRelievingTime = currentTime
                }
            } else {
                isRelievingPoseActive = false
                relievingPoseHoldTime = 0L
                lastRelievingTime = 0L
                if (currentPose == "Relieving pose") {
                    currentPose = "Not Relieving pose"
                }
            }

            runOnUiThread {
                binding.countTextView.text = "Relief Hold: ${relievingPoseHoldTime / 1000} s"
                binding.stageTextView.text = "Pose: $currentPose"
            }
        }
    }

    private fun exerciseTreePose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]

        // Verify that all required landmarks are detected.
        if (leftShoulder != null && rightShoulder != null &&
            leftHip != null && rightHip != null &&
            leftKnee != null && rightKnee != null &&
            leftAnkle != null && rightAnkle != null &&
            leftWrist != null && rightWrist != null) {

            // Calculate knee angles for both legs.
            // The angle at the knee is computed using the hip, knee, and ankle landmarks.
            val leftKneeAngle = calculateAngle(
                leftHip.x(), leftHip.y(),
                leftKnee.x(), leftKnee.y(),
                leftAnkle.x(), leftAnkle.y()
            )
            val rightKneeAngle = calculateAngle(
                rightHip.x(), rightHip.y(),
                rightKnee.x(), rightKnee.y(),
                rightAnkle.x(), rightAnkle.y()
            )

            // Define thresholds:
            // A raised (bent) leg should have a knee angle below the raisedLegThreshold.
            // The supporting (straight) leg should have a knee angle above the supportingLegThreshold.
            val raisedLegThreshold = 130.0
            val supportingLegThreshold = 160.0

            // Evaluate arm elevation: both wrists must be above the corresponding shoulders.
            // In normalized coordinates, a lower y-value indicates a higher position.
            val armsRaised = (leftWrist.y() < leftShoulder.y() && rightWrist.y() < rightShoulder.y())

            // Determine if the left leg is raised:
            // The left knee is significantly bent while the right knee remains nearly straight.
            val isLeftTreePose = (leftKneeAngle < raisedLegThreshold) &&
                    (rightKneeAngle > supportingLegThreshold)

            // Determine if the right leg is raised:
            // The right knee is significantly bent while the left knee remains nearly straight.
            val isRightTreePose = (rightKneeAngle < raisedLegThreshold) &&
                    (leftKneeAngle > supportingLegThreshold)

            // Overall, tree pose is detected if either variant is present and the arms are raised.
            val isTreePose = (isLeftTreePose || isRightTreePose) && armsRaised

            // Update pose state and trigger sound feedback.
            if (isTreePose) {
                if (currentPose != "Tree Pose") {
                    currentPose = "Tree Pose"
                    soundManager.playUpSound()
                }
            } else {
                if (currentPose == "Tree Pose") {
                    currentPose = "Not Tree Pose"
                }
            }

            runOnUiThread {
                binding.stageTextView.text = "Pose: $currentPose"
            }
        }
    }


    /**
     * Processes the incoming Mediapipe landmarks to detect and transition through the Sun Salutation sequence.
     *
     * The detection relies solely on computed joint angles and relative landmark positions.
     * Transition only occurs when the currently expected pose is reliably detected.
     */
    private fun exerciseSunSalutation(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftElbow = firstPersonLandmarks[13]
        val rightElbow = firstPersonLandmarks[14]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val nose = firstPersonLandmarks[0]  // Optionally used for additional criteria

        // Verify that all required landmarks are present.
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null ||
            leftElbow == null || rightElbow == null ||
            leftWrist == null || rightWrist == null) {
            return
        }

        // Compute joint angles.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )
        val leftElbowAngle = calculateAngle(
            leftShoulder.x(), leftShoulder.y(),
            leftElbow.x(), leftElbow.y(),
            leftWrist.x(), leftWrist.y()
        )
        val rightElbowAngle = calculateAngle(
            rightShoulder.x(), rightShoulder.y(),
            rightElbow.x(), rightElbow.y(),
            rightWrist.x(), rightWrist.y()
        )
        val leftHipAngle = calculateAngle(
            leftShoulder.x(), leftShoulder.y(),
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y()
        )
        val rightHipAngle = calculateAngle(
            rightShoulder.x(), rightShoulder.y(),
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y()
        )

        // Average y-coordinate values used in some pose detections.
        val avgShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0
        val avgHipY = (leftHip.y() + rightHip.y()) / 2.0

        // Determine which pose is detected based on the current state.
        var detectedPose: String? = null

        when (sunSalutationState) {
            0 -> { // Mountain Pose: Upright, arms at sides, nearly extended knees.
                if (leftWrist.y() > leftShoulder.y() && rightWrist.y() > rightShoulder.y() &&
                    leftKneeAngle > 170 && rightKneeAngle > 170) {
                    detectedPose = "Mountain Pose"
                }
            }
            1 -> { // Upward Salute: Arms raised overhead; wrists above shoulders.
                if (leftWrist.y() < leftShoulder.y() && rightWrist.y() < rightShoulder.y() &&
                    leftKneeAngle > 170 && rightKneeAngle > 170) {
                    detectedPose = "Upward Salute"
                }
            }
            2 -> { // Forward Bend: Forward flexion indicated by a reduced hip angle (< 100°) on at least one side.
                if ((leftHipAngle < 100 || rightHipAngle < 100) &&
                    // Arms remain hanging (wrists below shoulder level).
                    leftWrist.y() > leftShoulder.y() && rightWrist.y() > rightShoulder.y()) {
                    detectedPose = "Forward Bend"
                }
            }
            3 -> { // Plank Pose: Horizontal alignment; hip angles ≥ 155°; moderate elbow flexion.
                if ((leftHipAngle >= 155 && rightHipAngle >= 155) &&
                    ((leftElbowAngle in 70.0..115.0) || (rightElbowAngle in 70.0..115.0))) {
                    detectedPose = "Plank Pose"
                }
            }
            4 -> { // Chaturanga: Lowered from plank with elbows significantly bent (< 90°).
                if (leftElbowAngle < 90 && rightElbowAngle < 90 &&
                    leftHipAngle >= 150 && rightHipAngle >= 150) {
                    detectedPose = "Chaturanga"
                }
            }
            5 -> { // Upward Facing Dog: Chest lifted; elbows nearly extended (> 160°) and hips remain low.
                if (leftElbowAngle > 160 && rightElbowAngle > 160 &&
                    avgHipY > avgShoulderY) {
                    detectedPose = "Upward Facing Dog"
                }
            }
            6 -> { // Downward Facing Dog: Inverted V shape; arms extended (elbow angles > 160°)
                // and hips elevated relative to shoulders (hips are higher, so avgHipY is lower than avgShoulderY by a margin).
                if (leftElbowAngle > 160 && rightElbowAngle > 160 &&
                    avgHipY < (avgShoulderY - 0.1)) {
                    detectedPose = "Downward Facing Dog"
                }
            }
            7 -> { // Final Mountain Pose to complete the cycle.
                if (leftWrist.y() > leftShoulder.y() && rightWrist.y() > rightShoulder.y() &&
                    leftKneeAngle > 170 && rightKneeAngle > 170) {
                    detectedPose = "Mountain Pose"
                }
            }
        }

        // If the detected pose matches the expected pose for the current state, advance the sequence.
        if (detectedPose != null && detectedPose == sunSalutationPoses[sunSalutationState]) {
            currentSunPose = detectedPose
            soundManager.playUpSound()  // Trigger a transition sound.
            // Advance state; wrap-around when the sequence completes.
            sunSalutationState = (sunSalutationState + 1) % sunSalutationPoses.size
        }

        // Update the UI with the current Sun Salutation pose and state.
        runOnUiThread {
            binding.stageTextView.text = "Sun Salutation: ${sunSalutationPoses[sunSalutationState]}"
            binding.countTextView.text = "Current Pose: $currentSunPose"
        }
    }

    private fun exerciseChairPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]

        // Proceed only if all required landmarks are available.
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null ||
            leftWrist == null || rightWrist == null) {
            return
        }

        // Compute knee angles for both legs using the hip, knee, and ankle landmarks.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )

        // Evaluate arm elevation:
        // In normalized coordinates, a lower y-value indicates a higher position.
        val armsRaised = (leftWrist.y() < leftShoulder.y() && rightWrist.y() < rightShoulder.y())

        // Chair Pose (Utkatasana) criteria:
        // 1. Both knees must be significantly bent.
        //    Here, a knee angle between 80° and 110° is considered valid.
        // 2. Both arms must be raised overhead.
        val validKneeAngles = (leftKneeAngle in 80.0..110.0) && (rightKneeAngle in 80.0..110.0)
        val isChairPose = validKneeAngles && armsRaised

        // Update pose state and provide auditory feedback.
        if (isChairPose) {
            if (currentPose != "Chair Pose") {
                currentPose = "Chair Pose"
                soundManager.playUpSound()  // Play Chair Pose sound cue.
            }
        } else {
            if (currentPose == "Chair Pose") {
                currentPose = "Not Chair Pose"
            }
        }

        // Update the UI.
        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    //Normal Pose

    private fun exerciseMountainPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]

        // Proceed only if all required landmarks are available.
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null ||
            leftWrist == null || rightWrist == null) {
            return
        }

        // Compute knee angles using hip, knee, and ankle landmarks.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )

        // Check that both knees are nearly straight.
        val kneesStraight = (leftKneeAngle > 170) && (rightKneeAngle > 170)

        // Check that the arms are relaxed by the sides.
        // In normalized coordinates, a lower wrist (higher y) indicates a lower position.
        val armsRelaxed = (leftWrist.y() > leftShoulder.y() && rightWrist.y() > rightShoulder.y())

        // Determine if the Mountain Pose is valid.
        val isMountainPose = kneesStraight && armsRelaxed

        if (isMountainPose) {
            if (currentPose != "Mountain Pose") {
                currentPose = "Mountain Pose"
                soundManager.playUpSound()
            }
        } else {
            if (currentPose == "Mountain Pose") {
                currentPose = "Not Mountain Pose"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    //Yoga Normal


    private fun exerciseEasyPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks: hips, knees, ankles, and shoulders.
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]

        // Ensure all required landmarks are available.
        if (leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null ||
            leftShoulder == null || rightShoulder == null) {
            return
        }

        // Compute knee angles using hip, knee, and ankle coordinates.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )

        // For Easy Pose, both knees should be significantly flexed.
        // A knee angle less than 120° is considered indicative of a seated (cross-legged) posture.
        val kneesFlexed = (leftKneeAngle < 120) && (rightKneeAngle < 120)

        // Compute the Euclidean distance between the ankles.
        val dx = leftAnkle.x() - rightAnkle.x()
        val dy = leftAnkle.y() - rightAnkle.y()
        val ankleDistance = sqrt(dx * dx + dy * dy)
        // In Easy Pose, the legs are crossed, so the ankles should be close together.
        val anklesClose = ankleDistance < 0.12  // This threshold may be calibrated as needed.

        // Verify that the upper body remains upright.
        // One simple check is to ensure that the shoulders are roughly aligned horizontally.
        val shouldersAligned = abs(leftShoulder.x() - rightShoulder.x()) < 0.2

        // Combine the conditions to decide if the practitioner is in Easy Pose.
        val isEasyPose = kneesFlexed && anklesClose && shouldersAligned

        if (isEasyPose) {
            if (currentPose != "Easy Pose") {
                currentPose = "Easy Pose"
                soundManager.playUpSound()  // Play the Easy Pose sound cue.
            }
        } else {
            if (currentPose == "Easy Pose") {
                currentPose = "Not Easy Pose"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    private fun exerciseBoatPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]

        // Proceed only if all required landmarks are available.
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null ||
            leftWrist == null || rightWrist == null) {
            return
        }

        // Compute midpoints for shoulders and hips.
        val midShoulderX = (leftShoulder.x() + rightShoulder.x()) / 2.0
        val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0
        val midHipX = (leftHip.x() + rightHip.x()) / 2.0
        val midHipY = (leftHip.y() + rightHip.y()) / 2.0

        // Compute trunk vector components.
        val dx = midShoulderX - midHipX
        val dy = midShoulderY - midHipY

        // Calculate trunk angle relative to the vertical.
        // In normalized coordinates, the vertical axis has zero horizontal deviation.
        // The absolute angle (in degrees) from vertical is given by arctan(|dx|/|dy|).
        val trunkAngleRadians = kotlin.math.atan2(kotlin.math.abs(dx), kotlin.math.abs(dy))
        val trunkAngleDegrees = Math.toDegrees(trunkAngleRadians)

        // For Boat Pose, the trunk is expected to lean backward.
        // We require a moderate inclination between, for example, 30° and 60°.
        val trunkInclined = trunkAngleDegrees in 30.0..60.0

        // Check if the legs are lifted.
        // In Boat Pose, the ankles are elevated relative to the hips.
        val avgAnkleY = (leftAnkle.y() + rightAnkle.y()) / 2.0
        val legsLifted = avgAnkleY < midHipY

        // Compute knee angles.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )
        // For a fully extended leg, the knee angle should be high (e.g., > 160°).
        val legsExtended = (leftKneeAngle > 160) && (rightKneeAngle > 160)

        // Evaluate arm extension.
        // In Boat Pose, the arms are usually extended forward.
        // We require the wrists to be above the shoulders (i.e. lower y-values).
        val armsExtended = (leftWrist.y() < leftShoulder.y()) && (rightWrist.y() < rightShoulder.y())

        // Combine the criteria to determine Boat Pose.
        val isBoatPose = trunkInclined && legsLifted && legsExtended && armsExtended

        // Update pose state and trigger auditory feedback.
        if (isBoatPose) {
            if (currentPose != "Boat Pose") {
                currentPose = "Boat Pose"
                soundManager.playUpSound()  // Trigger the Boat Pose sound cue.
            }
        } else {
            if (currentPose == "Boat Pose") {
                currentPose = "Not Boat Pose"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    private fun exerciseCatCowPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract required landmarks.
        val nose = firstPersonLandmarks[0]
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]

        // Ensure all required landmarks are available.
        if (nose == null || leftShoulder == null || rightShoulder == null ||
            leftWrist == null || rightWrist == null ||
            leftKnee == null || rightKnee == null) {
            return
        }

        // Confirm the practitioner is on all fours.
        // In normalized coordinates, a higher y-value indicates a lower position.
        val groundThreshold = 0.8  // Adjust as needed based on camera view.
        val onAllFours = (leftWrist.y() > groundThreshold && rightWrist.y() > groundThreshold &&
                leftKnee.y() > groundThreshold && rightKnee.y() > groundThreshold)

        if (!onAllFours) {
            // If not on all fours, do not update Cat-Cow status.
            if (currentPose == "Cat Pose" || currentPose == "Cow Pose") {
                currentPose = "Not Cat-Cow Pose"
            }
            runOnUiThread {
                binding.stageTextView.text = "Pose: $currentPose"
            }
            return
        }

        // Compute the midpoint of the shoulders.
        val midShoulderX = (leftShoulder.x() + rightShoulder.x()) / 2.0
        val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0

        // Define a margin to reduce sensitivity.
        val margin = 0.05

        // Determine pose phase based on head (nose) position relative to mid-shoulder.
        if (nose.y() < midShoulderY - margin) {
            // In Cow Pose, the head is lifted (nose is higher than the shoulder line).
            if (currentPose != "Cow Pose") {
                currentPose = "Cow Pose"
                //soundManager.playCowPoseSound()
            }
        } else if (nose.y() > midShoulderY + margin) {
            // In Cat Pose, the head is tucked (nose is lower than the shoulder line).
            if (currentPose != "Cat Pose") {
                currentPose = "Cat Pose"
                //soundManager.playCatPoseSound()
                soundManager.playUpSound()

            }
        } else {
            // When the difference is minimal, maintain the current state.
            if (currentPose != "Neutral Cat-Cow") {
                currentPose = "Neutral Cat-Cow"
            }
        }

        // Update the UI.
        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    private fun exerciseBowPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]

        // Ensure all required landmarks are available.
        if (leftWrist == null || rightWrist == null ||
            leftAnkle == null || rightAnkle == null ||
            leftKnee == null || rightKnee == null ||
            leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null) {
            return
        }

        // Compute Euclidean distances between wrists and corresponding ankles.
        val leftWristAnkleDistance = sqrt((leftWrist.x() - leftAnkle.x()).pow(2) +
                (leftWrist.y() - leftAnkle.y()).pow(2))
        val rightWristAnkleDistance = sqrt((rightWrist.x() - rightAnkle.x()).pow(2) +
                (rightWrist.y() - rightAnkle.y()).pow(2))

        // Define a proximity threshold for hands reaching the ankles.
        val proximityThreshold = 0.12
        val handsGraspingAnkles = (leftWristAnkleDistance < proximityThreshold) &&
                (rightWristAnkleDistance < proximityThreshold)

        // Compute knee angles using hip, knee, and ankle landmarks.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )

        // For Bow Pose, knees should be bent.
        val kneesBent = (leftKneeAngle in 70.0..120.0) && (rightKneeAngle in 70.0..120.0)

        // Compute midpoints for shoulders and hips.
        val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0
        val midHipY = (leftHip.y() + rightHip.y()) / 2.0
        // In normalized coordinates, a smaller y-value indicates a higher position.
        // The chest is considered lifted if the shoulders (chest) are above the hips.
        val chestLifted = midShoulderY < midHipY

        // Combine all conditions to detect Bow Pose.
        val isBowPose = handsGraspingAnkles && kneesBent && chestLifted

        if (isBowPose) {
            if (currentPose != "Bow Pose") {
                currentPose = "Bow Pose"
                //soundManager.playBowPoseSound() // Trigger auditory feedback.
                soundManager.playUpSound()

            }
        } else {
            if (currentPose == "Bow Pose") {
                currentPose = "Not Bow Pose"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    //Overweight exercises

    private fun exerciseDownwardFacingDogPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftElbow = firstPersonLandmarks[13]
        val rightElbow = firstPersonLandmarks[14]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]

        // Ensure all required landmarks are available.
        if (leftShoulder == null || rightShoulder == null ||
            leftElbow == null || rightElbow == null ||
            leftWrist == null || rightWrist == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null) {
            return
        }

        // Compute elbow angles using the shoulder, elbow, and wrist landmarks.
        val leftElbowAngle = calculateAngle(
            leftShoulder.x(), leftShoulder.y(),
            leftElbow.x(), leftElbow.y(),
            leftWrist.x(), leftWrist.y()
        )
        val rightElbowAngle = calculateAngle(
            rightShoulder.x(), rightShoulder.y(),
            rightElbow.x(), rightElbow.y(),
            rightWrist.x(), rightWrist.y()
        )

        // Compute knee angles using the hip, knee, and ankle landmarks.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )

        // Compute the midpoints for shoulders and hips (using the y-coordinates).
        val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0
        val midHipY = (leftHip.y() + rightHip.y()) / 2.0

        // Define a margin to detect hip lift (in normalized coordinates, a smaller y-value means higher up).
        val hipLiftMargin = 0.05

        // Pose conditions:
        // - Arms extended: both elbow angles > 160°.
        // - Legs extended: both knee angles > 160°.
        // - Hips lifted: the mid-hip y-coordinate is at least hipLiftMargin less than the mid-shoulder y-coordinate.
        val armsExtended = (leftElbowAngle > 160 && rightElbowAngle > 160)
        val legsExtended = (leftKneeAngle > 160 && rightKneeAngle > 160)
        val hipsLifted = midHipY < (midShoulderY - hipLiftMargin)

        val isDownwardFacingDog = armsExtended && legsExtended && hipsLifted

        if (isDownwardFacingDog) {
            if (currentPose != "Downward-Facing Dog") {
                currentPose = "Downward-Facing Dog"
//                soundManager.playDownwardDogSound()  // Play a sound cue.
                soundManager.playUpSound()
            }
        } else {
            if (currentPose == "Downward-Facing Dog") {
                currentPose = "Not Downward-Facing Dog"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    private fun exerciseTrianglePose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]

        // Proceed only if all required landmarks are available.
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftWrist == null || rightWrist == null ||
            leftAnkle == null || rightAnkle == null) {
            return
        }

        // Compute midpoints for shoulders and hips.
        val midShoulderX = (leftShoulder.x() + rightShoulder.x()) / 2.0
        val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0
        val midHipX = (leftHip.x() + rightHip.x()) / 2.0
        val midHipY = (leftHip.y() + rightHip.y()) / 2.0

        // Compute torso tilt angle relative to vertical.
        // A perfectly vertical torso gives an angle of 0°. We compute:
        val dx = Math.abs(midShoulderX - midHipX)
        val dy = Math.abs(midShoulderY - midHipY)
        val torsoTiltRadians = Math.atan2(dx, dy)
        val torsoTiltDegrees = Math.toDegrees(torsoTiltRadians)
        // Expected tilt range for Triangle Pose.
        val torsoTiltValid = torsoTiltDegrees in 20.0..60.0

        // Evaluate arm positions.
        // One arm should be raised (wrist above shoulder) while the other is lowered.
        val leftArmUp = leftWrist.y() < leftShoulder.y()
        val rightArmUp = rightWrist.y() < rightShoulder.y()
        val leftArmDown = leftWrist.y() > leftShoulder.y()
        val rightArmDown = rightWrist.y() > rightShoulder.y()
        val armCondition = (leftArmUp && rightArmDown) || (rightArmUp && leftArmDown)

        // Evaluate leg stance: a wide stance is typical.
        // Check the horizontal distance between the hips.
        val hipDistance = rightHip.x() - leftHip.x()
        val wideStance = hipDistance > 0.3  // Threshold may be calibrated as needed.

        // Combine conditions to decide if the practitioner is in Triangle Pose.
        val isTrianglePose = torsoTiltValid && armCondition && wideStance

        if (isTrianglePose) {
            if (currentPose != "Triangle Pose") {
                currentPose = "Triangle Pose"
//                soundManager.playTrianglePoseSound()  // Play a sound cue for Triangle Pose.
                soundManager.playUpSound()

            }
        } else {
            if (currentPose == "Triangle Pose") {
                currentPose = "Not Triangle Pose"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    private fun exerciseWarrior2Pose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract essential landmarks.
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]
        val leftWrist = firstPersonLandmarks[15]
        val rightWrist = firstPersonLandmarks[16]

        // Proceed only if all required landmarks are available.
        if (leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null ||
            leftWrist == null || rightWrist == null) {
            return
        }

        // Compute knee angles using hip, knee, and ankle coordinates.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )

        // Determine front leg condition:
        // Either left knee is bent (80°-110°) while right knee is nearly extended (>160°),
        // or vice versa.
        val isLeftLegFront = (leftKneeAngle in 80.0..110.0) && (rightKneeAngle > 160)
        val isRightLegFront = (rightKneeAngle in 80.0..110.0) && (leftKneeAngle > 160)
        val kneeCondition = isLeftLegFront || isRightLegFront

        // Evaluate arm positions.
        // Left arm should extend to the left: left wrist is left of left shoulder,
        // and right arm should extend to the right: right wrist is right of right shoulder.
        // Also, their y-values should be similar (difference < 0.05).
        val leftArmExtended = (leftWrist.x() < leftShoulder.x()) &&
                (Math.abs(leftWrist.y() - leftShoulder.y()) < 0.05)
        val rightArmExtended = (rightWrist.x() > rightShoulder.x()) &&
                (Math.abs(rightWrist.y() - rightShoulder.y()) < 0.05)
        val armCondition = leftArmExtended && rightArmExtended

        // Evaluate wide stance: horizontal distance between ankles should exceed a threshold.
        val ankleDistance = Math.abs(rightAnkle.x() - leftAnkle.x())
        val wideStance = ankleDistance > 0.3  // Threshold can be calibrated.

        // Ensure torso is upright: midpoints of shoulders and hips should be nearly aligned horizontally.
        val midShoulderX = (leftShoulder.x() + rightShoulder.x()) / 2.0
        val midHipX = (leftHip.x() + rightHip.x()) / 2.0
        val torsoUpright = Math.abs(midShoulderX - midHipX) < 0.1

        // Combine all conditions to determine if the pose is Warrior 2.
        val isWarrior2Pose = kneeCondition && armCondition && wideStance && torsoUpright

        if (isWarrior2Pose) {
            if (currentPose != "Warrior 2") {
                currentPose = "Warrior 2"
//                soundManager.playWarrior2PoseSound()  // Trigger the Warrior 2 sound cue.
                soundManager.playUpSound()

            }
        } else {
            if (currentPose == "Warrior 2") {
                currentPose = "Not Warrior 2"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }
    private fun exerciseChildPose(firstPersonLandmarks: MutableList<NormalizedLandmark>) {
        // Extract required landmarks.
        val nose = firstPersonLandmarks[0]
        val leftShoulder = firstPersonLandmarks[11]
        val rightShoulder = firstPersonLandmarks[12]
        val leftHip = firstPersonLandmarks[23]
        val rightHip = firstPersonLandmarks[24]
        val leftKnee = firstPersonLandmarks[25]
        val rightKnee = firstPersonLandmarks[26]
        val leftAnkle = firstPersonLandmarks[27]
        val rightAnkle = firstPersonLandmarks[28]

        // Ensure all required landmarks are available.
        if (nose == null || leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null ||
            leftKnee == null || rightKnee == null ||
            leftAnkle == null || rightAnkle == null) {
            return
        }

        // Compute midpoints for shoulders and hips.
        val midShoulderX = (leftShoulder.x() + rightShoulder.x()) / 2.0
        val midShoulderY = (leftShoulder.y() + rightShoulder.y()) / 2.0
        val midHipX = (leftHip.x() + rightHip.x()) / 2.0
        val midHipY = (leftHip.y() + rightHip.y()) / 2.0

        // Compute torso tilt angle relative to vertical.
        val dx = kotlin.math.abs(midShoulderX - midHipX)
        val dy = kotlin.math.abs(midShoulderY - midHipY)
        val torsoTiltRadians = kotlin.math.atan2(dx, dy)
        val torsoTiltDegrees = Math.toDegrees(torsoTiltRadians)
        // In Balasana, the torso is folded forward; we expect a significant tilt (e.g., > 45°).
        val torsoTiltValid = torsoTiltDegrees > 45.0

        // Check head position:
        // In Balasana, the head is lowered. The nose should be lower (i.e., have a higher y-value) than the mid-shoulder by a margin.
        val headLowered = nose.y() > (midShoulderY + 0.05)

        // Compute knee angles using hip, knee, and ankle landmarks.
        val leftKneeAngle = calculateAngle(
            leftHip.x(), leftHip.y(),
            leftKnee.x(), leftKnee.y(),
            leftAnkle.x(), leftAnkle.y()
        )
        val rightKneeAngle = calculateAngle(
            rightHip.x(), rightHip.y(),
            rightKnee.x(), rightKnee.y(),
            rightAnkle.x(), rightAnkle.y()
        )
        // In a kneeling position (Balasana), the knees are bent. A knee angle less than about 120° is typical.
        val kneesBent = (leftKneeAngle < 120) && (rightKneeAngle < 120)

        // Combine all conditions to decide if the practitioner is in Balasana.
        val isBalasana = torsoTiltValid && headLowered && kneesBent

        if (isBalasana) {
            if (currentPose != "Balasana") {
                currentPose = "Balasana"
//                soundManager.playBalasanaSound()  // Trigger auditory feedback.
                soundManager.playUpSound()

            }
        } else {
            if (currentPose == "Balasana") {
                currentPose = "Not Balasana"
            }
        }

        runOnUiThread {
            binding.stageTextView.text = "Pose: $currentPose"
        }
    }

    private fun endExercise() {
        soundManager.playCompleteSound()
        runOnUiThread {

            poseLandmarker.close() // Closes the pose landmarker to release resources.

            soundManager.stopAllSounds() // Stops all playing sounds

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

            // Shutdown camera executor
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



}

