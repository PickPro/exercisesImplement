package com.ooplab.exercises_fitfuel



import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ooplab.exercises_fitfuel.Utils.setData
import com.ooplab.exercises_fitfuel.Utils.viewBinding
import com.ooplab.exercises_fitfuel.databinding.ActivityExerciseListBinding
import com.ooplab.exercises_fitfuel.databinding.ActivitySubTypeBinding
import com.ooplab.exercises_fitfuel.databinding.SampleRowCategoryBinding

data class ExerciseModel(var name: String = "" , var type: String = "")
class ExerciseListActivity : AppCompatActivity() {
    val binding by viewBinding(ActivityExerciseListBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var category = intent.getStringExtra("category")!!
        Toast.makeText(this, category, Toast.LENGTH_SHORT).show()

        var allCategories = listOf(
            ExerciseModel("Leg Raise" , "Cardio"),
            ExerciseModel("Squats" , "Cardio"),
            ExerciseModel("PushUp" , "Strength"),
            ExerciseModel("Stand" , "Normal"),
        )

        binding.recyclerview.setData(allCategories.filter { it.type== category}, SampleRowCategoryBinding::inflate) { b, item, position ->
            b.name.text = item.name
            lateinit var i: Intent
            b.main.setOnClickListener{
               i =  Intent(this , ExerciseActivity::class.java)
                i.putExtra("exerciseName",item.name)
                startActivity(i)
            }
        }
    }
}