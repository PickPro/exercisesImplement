package com.ooplab.exercises_fitfuel



import android.R.attr.category
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ooplab.exercises_fitfuel.Utils.setData
import com.ooplab.exercises_fitfuel.Utils.viewBinding
import com.ooplab.exercises_fitfuel.databinding.ActivitySubTypeBinding
import com.ooplab.exercises_fitfuel.databinding.SampleRowCategoryBinding

class SubTypeActivity : AppCompatActivity() {
    val binding by viewBinding(ActivitySubTypeBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Toast.makeText(this, "Yoga", Toast.LENGTH_SHORT).show()

        var allCategories = listOf(
            "Under Weight",
            "Normal",
            "Over Weight",
        )

        binding.recyclerview.setData(allCategories, SampleRowCategoryBinding::inflate) { b, item, position ->
            b.name.text = item

            lateinit var i: Intent
            b.main.setOnClickListener{
                i=Intent(this , ExerciseListActivity::class.java)
                i.putExtra("category",item)
                startActivity(i)
            }
        }

    }
}