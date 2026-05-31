package com.example.projecthellopaw.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.projecthellopaw.R

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvEmail: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvEmail = view.findViewById(R.id.tvEmail)

        loadUserData()

        return view
    }

    private fun loadUserData(){

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid

        if(uid != null){

            FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .get()
                .addOnSuccessListener {

                    val name =
                        it.child("name").value.toString()

                    val email =
                        it.child("email").value.toString()

                    tvGreeting.text = "Hello $name!"
                    tvEmail.text = email
                }
        }
    }
}