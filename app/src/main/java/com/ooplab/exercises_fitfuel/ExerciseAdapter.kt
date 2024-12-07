//package com.ooplab.exercises_fitfuel
//import com.ooplab.exercises_fitfuel.R
//import android.content.Context
//import android.content.Intent
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.ooplab.exercises_fitfuel.model.Exercise
//
//class ExerciseAdapter(private val context: Context, private val exercises: List<Exercise>) :
//    RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
//        val view = LayoutInflater.from(context).inflate(R.layout.item_exercise, parent, false)
//        return ExerciseViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
//        val exercise = exercises[position]
//        holder.exerciseName.text = exercise.name
//        holder.itemView.setOnClickListener {
//            val intent = Intent(context, ExerciseActivity::class.java).apply {
//                putExtra("exerciseName", exercise.name)
//            }
//            context.startActivity(intent)
//        }
//    }
//
//    override fun getItemCount(): Int = exercises.size
//
//    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val exerciseName: TextView = itemView.findViewById(R.id.text_view_exercise_name)
//    }
//}