package com.ooplab.exercises_fitfuel

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ExerciseActivity : AppCompatActivity() {
    private lateinit var exerciseName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)

        exerciseName = intent.getStringExtra("exerciseName") ?: ""
        val textViewExerciseName: TextView = findViewById(R.id.text_view_exercise_name)
        textViewExerciseName.text = exerciseName
    }
}