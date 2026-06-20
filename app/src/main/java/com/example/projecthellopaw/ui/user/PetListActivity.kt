package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.projecthellopaw.R

class PetListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_list)

        val ivBack = findViewById<ImageView>(R.id.ivBackPetList)
        ivBack?.setOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerPetList, PetListFragment())
                .commit()
        }
    }
}