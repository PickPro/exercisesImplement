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
            // Fitness Exercises – Cardio
            ExerciseModel("Leg Raise", "Cardio"),
            ExerciseModel("Plank", "Cardio"),
            ExerciseModel("Jumping Jacks", "Cardio"),
            ExerciseModel("Kick Back", "Cardio"),
            ExerciseModel("Situps", "Cardio"),
            ExerciseModel("Squats", "Cardio"),

            // Strength Exercises
            ExerciseModel("Squats", "Strength"),
            ExerciseModel("Push Ups", "Strength"),
            ExerciseModel("Pull Ups", "Strength"),
            ExerciseModel("Plank", "Strength"),
            ExerciseModel("Glute Bridge", "Strength"),
            ExerciseModel("Situps", "Strength"),

            // Yoga Exercises – Categorized Based on Weight Type
            ExerciseModel("Cobra Pose", "Under Weight"),
            ExerciseModel("Relieving Pose", "Under Weight"),
            ExerciseModel("Tree Pose", "Under Weight"),
            ExerciseModel("Sun Salutation", "Under Weight"),
            ExerciseModel("Chair Pose", "Under Weight"),

            ExerciseModel("Downward Facing Dog", "Over Weight"),
            ExerciseModel("Triangle Pose", "Over Weight"),
            ExerciseModel("Glute Bridge", "Over Weight"), // Moved from Strength
            ExerciseModel("Warrior 2", "Over Weight"),
            ExerciseModel("Child Pose", "Over Weight"),
            ExerciseModel("Bridge Pose", "Over Weight"),

            ExerciseModel("Mountain Pose", "Normal"),
            ExerciseModel("Easy Pose", "Normal"),
            ExerciseModel("Boat Pose", "Normal"),
            ExerciseModel("Cat Cow Pose", "Normal"),
            ExerciseModel("Bow Pose", "Normal"),

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