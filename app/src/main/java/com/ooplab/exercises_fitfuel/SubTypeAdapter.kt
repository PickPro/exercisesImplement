package com.ooplab.exercises_fitfuel

class SubTypeAdapter(private val context: Context, private val subTypes: List<SubType>) :
    RecyclerView.Adapter<SubTypeAdapter.SubTypeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubTypeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_subtype, parent, false)
        return SubTypeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubTypeViewHolder, position: Int) {
        val subType = subTypes[position]
        holder.subTypeName.text = subType.name
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ExerciseListActivity::class.java).apply {
                putExtra("parentId", subType.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = subTypes.size

    class SubTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subTypeName: TextView = itemView.findViewById(R.id.text_view_subtype_name)
    }
}