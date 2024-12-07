package com.ooplab.exercises_fitfuel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ooplab.exercises_fitfuel.Utils.setData
import com.ooplab.exercises_fitfuel.Utils.viewBinding
import com.ooplab.exercises_fitfuel.databinding.ActivityCategoriesBinding
import com.ooplab.exercises_fitfuel.databinding.ActivityExerciseBinding
import com.ooplab.exercises_fitfuel.databinding.SampleRowCategoryBinding


data class CategoryModel(var name: String="" , var subtype: Boolean = false , )

class CategoriesActivity : AppCompatActivity() {
    val binding by viewBinding(ActivityCategoriesBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_categories)
            var allCategories = listOf(
            CategoryModel("Cardio" , false),
            CategoryModel("Yoga" , true),
            CategoryModel("Strength" , false),
        )

          binding.recyclerview.setData(allCategories, SampleRowCategoryBinding::inflate) { b, item, position ->
              b.name.text = item.name

              lateinit var i: Intent
              b.main.setOnClickListener{
                  i = if (item.subtype){
                      Intent(this , SubTypeActivity::class.java)
                  }else{
                      Intent(this , ExerciseListActivity::class.java)
                  }
                  i.putExtra("category",item.name)
                  startActivity(i)
              }
          }

    }
}