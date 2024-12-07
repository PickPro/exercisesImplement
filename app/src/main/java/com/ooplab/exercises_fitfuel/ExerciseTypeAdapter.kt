package com.ooplab.exercises_fitfuel

class ExerciseTypeAdapter(private val context: Context, private val exerciseTypes: List<ExerciseType>) :
    RecyclerView.Adapter<ExerciseTypeAdapter.TypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_exercise_type, parent, false)
        return TypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TypeViewHolder, position: Int) {
        val type = exerciseTypes[position]
        holder.typeName.text = type.name
        holder.itemView.setOnClickListener {
            val intent = if (type.hasSubTypes) {
                Intent(context, SubTypeActivity::class.java).apply {
                    putExtra("typeId", type.id)
                }
            } else {
                Intent(context, ExerciseListActivity::class.java).apply {
                    putExtra("parentId", type.id)
                }
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = exerciseTypes.size

    class TypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeName: TextView = itemView.findViewById(R.id.text_view_type_name)
    }
}