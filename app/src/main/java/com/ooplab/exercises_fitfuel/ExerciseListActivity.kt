package com.ooplab.exercises_fitfuel

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ExerciseListActivity : AppCompatActivity() {
    private lateinit var recyclerViewExercises: RecyclerView
    private lateinit var adapter: ExerciseAdapter
    private lateinit var exercises: List<Exercise>
    private lateinit var parentId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)
        recyclerViewExercises = findViewById(R.id.recycler_view_exercises)
        recyclerViewExercises.layoutManager = LinearLayoutManager(this)
        parentId = intent.getStringExtra("parentId") ?: ""
        exercises = getExercises(parentId)
        adapter = ExerciseAdapter(this, exercises)
        recyclerViewExercises.adapter = adapter
    }

    private fun getExercises(parentId: String): List<Exercise> {
        return listOf(
            Exercise("1", "Exercise 1", parentId),
            Exercise("2", "Exercise 2", parentId),
            Exercise("3", "Exercise 3", parentId)
        )
    }
}