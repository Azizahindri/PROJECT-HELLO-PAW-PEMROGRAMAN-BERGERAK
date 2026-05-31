package com.example.projecthellopaw.ui.user

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.projecthellopaw.R

class UserMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        val bottomNav =
            findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // Fragment awal
        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener {

            when(it.itemId){

                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }

                R.id.nav_doctor -> {
                    loadFragment(DoctorListFragment())
                    true
                }

                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }

                else -> false
            }

        }
    }

    private fun loadFragment(fragment: Fragment){

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

    }
}