package com.oguzhandurmaz.stockmanagementapp.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            if (isAdmin(auth.currentUser!!.email!!))
                intentToAdminActivity()
            else
                intentToStaffActivity()
        }
    }

    fun signInClicked(view: View) {
        val email = binding.emailText.text.toString()
        val password = binding.passwordText.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter e-mail and password!", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                if (isAdmin(auth.currentUser!!.email!!))
                    intentToAdminActivity()
                else
                    intentToStaffActivity()
            }.addOnFailureListener {
                    Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signUpClicked(view: View) {
        val email = binding.emailText.text.toString()
        val password = binding.passwordText.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter e-mail and password!", Toast.LENGTH_SHORT).show()
        } else {
            auth.createUserWithEmailAndPassword(email,password).addOnSuccessListener {
                Toast.makeText(this, "New user created successfully.", Toast.LENGTH_SHORT).show()
                if (isAdmin(auth.currentUser!!.email!!))
                    intentToAdminActivity()
                else
                    intentToStaffActivity()
            }.addOnFailureListener {
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAdmin(email: String): Boolean {
        return email == "admin@gmail.com"
    }

    private fun intentToAdminActivity(){
        val intent = Intent(this, AdminActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun intentToStaffActivity(){
        val intent = Intent(this, StaffActivity::class.java)
        startActivity(intent)
        finish()
    }
}