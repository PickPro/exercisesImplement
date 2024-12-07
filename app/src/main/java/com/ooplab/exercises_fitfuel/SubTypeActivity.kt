package com.ooplab.exercises_fitfuel

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SubTypeActivity : AppCompatActivity() {
    private lateinit var recyclerViewSubTypes: RecyclerView
    private lateinit var adapter: SubTypeAdapter
    private lateinit var subTypes: List<SubType>
    private lateinit var parentTypeId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subtype)
        recyclerViewSubTypes = findViewById(R.id.recycler_view_subtypes)
        recyclerViewSubTypes.layoutManager = LinearLayoutManager(this)
        parentTypeId = intent.getStringExtra("typeId") ?: ""
        subTypes = getSubTypes(parentTypeId)
        adapter = SubTypeAdapter(this, subTypes)
        recyclerViewSubTypes.adapter = adapter
    }

    private fun getSubTypes(typeId: String): List<SubType> {
        return listOf(
            SubType("1", "Underweight", typeId),
            SubType("2", "Normal", typeId),
            SubType("3", "Overweight", typeId)
        )
    }
}