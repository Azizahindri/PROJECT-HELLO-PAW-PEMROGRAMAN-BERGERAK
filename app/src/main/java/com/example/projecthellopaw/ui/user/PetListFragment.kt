package com.example.projecthellopaw.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projecthellopaw.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PetListFragment : Fragment() {

    private lateinit var rvPetList: RecyclerView
    private lateinit var fabAddPet: FloatingActionButton
    private lateinit var petAdapter: PetAdapter
    private var listHewan: ArrayList<PetModel> = ArrayList()

    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pet_list, container, false)

        rvPetList = view.findViewById(R.id.rv_pet_list)
        fabAddPet = view.findViewById(R.id.fab_add_pet)

        rvPetList.layoutManager = LinearLayoutManager(context)
        rvPetList.setHasFixedSize(true)

        petAdapter = PetAdapter(listHewan)
        rvPetList.adapter = petAdapter

        ambilDataDariFirebase(view)

        fabAddPet.setOnClickListener {
            val intent = Intent(context, AddPetActivity::class.java)
            startActivity(intent)
        }

        return view
    }

    private fun ambilDataDariFirebase(view: View) {
        val layoutEmpty = view.findViewById<LinearLayout>(R.id.layout_empty_state)

        db.collection("pets")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(context, "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (value != null) {
                    listHewan.clear()

                    for (doc in value.documents) {
                        val name = doc.getString("name") ?: ""
                        val category = doc.getString("category") ?: ""
                        val type = doc.getString("type") ?: ""
                        val age = doc.getString("age") ?: ""
                        val gender = doc.getString("gender") ?: ""

                        val hewan = PetModel(name, category, type, age, gender)
                        listHewan.add(hewan)
                    }

                    if (listHewan.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                        rvPetList.visibility = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        rvPetList.visibility = View.VISIBLE
                    }

                    petAdapter.notifyDataSetChanged()
                }
            }
    }
}