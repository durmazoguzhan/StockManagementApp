package com.oguzhandurmaz.stockmanagementapp.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityNewStoreBinding

class NewStoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewStoreBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewStoreBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firestore = Firebase.firestore
    }

    fun addNewStoreClicked(view: View) {
        val name = binding.storeNameText.text.toString()
        val address = binding.storeAddressText.text.toString()
        if (name.isEmpty() || address.isEmpty())
            Toast.makeText(this, "Please enter informations.", Toast.LENGTH_SHORT).show()
        else {
            val postMap = hashMapOf<String, String>()
            postMap.put("storeName", binding.storeNameText.text.toString())
            postMap.put("storeAddress", binding.storeAddressText.text.toString())

            firestore.collection("Stores").add(postMap).addOnSuccessListener {
                Toast.makeText(this, "new store added successfully.", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { exception ->
                Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}