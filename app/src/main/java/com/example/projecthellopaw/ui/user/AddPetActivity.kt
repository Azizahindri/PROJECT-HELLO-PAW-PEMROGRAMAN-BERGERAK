package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddPetActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_pet)

        val btnBack = findViewById<ImageView>(R.id.btn_add_pet_back)
        val etCategory = findViewById<EditText>(R.id.et_add_pet_category)
        val etType = findViewById<EditText>(R.id.et_add_pet_type)
        val etName = findViewById<EditText>(R.id.et_add_pet_name)
        val etAge = findViewById<EditText>(R.id.et_add_pet_age)
        val rgGender = findViewById<RadioGroup>(R.id.rg_add_pet_gender)
        val btnSave = findViewById<Button>(R.id.btn_add_pet_save)

        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val category = etCategory.text.toString().trim()
            val type = etType.text.toString().trim()
            val name = etName.text.toString().trim()
            val age = etAge.text.toString().trim()

            val selectedGenderId = rgGender.checkedRadioButtonId

            if (category.isEmpty() || type.isEmpty() || name.isEmpty() || age.isEmpty() || selectedGenderId == -1) {
                Toast.makeText(this, "Mohon lengkapi semua data hewan ya!", Toast.LENGTH_SHORT).show()
            } else {
                val radioButton = findViewById<RadioButton>(selectedGenderId)
                val gender = radioButton.text.toString()

                val petData = hashMapOf(
                    "category" to category,
                    "type" to type,
                    "name" to name,
                    "age" to age,
                    "gender" to gender,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )

                db.collection("pets")
                    .add(petData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Data $name berhasil disimpan!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}