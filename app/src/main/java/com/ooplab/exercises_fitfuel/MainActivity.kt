package com.ooplab.exercises_fitfuel

import com.ooplab.exercises_fitfuel.R
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ooplab.exercises_fitfuel.model.ExerciseType


class MainActivity : AppCompatActivity() {
    private lateinit var recyclerViewTypes: RecyclerView
    private lateinit var exerciseTypes: List<ExerciseType>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerViewTypes = findViewById(R.id.recycler_view_types)
        recyclerViewTypes.layoutManager = LinearLayoutManager(this)
        exerciseTypes = getExerciseTypes()
    }

    private fun getExerciseTypes(): List<ExerciseType> {
        return listOf(
            ExerciseType("1", "Cardio", false),
            ExerciseType("2", "Strength", false),
            ExerciseType("3", "Yoga", true)
        )
    }
}