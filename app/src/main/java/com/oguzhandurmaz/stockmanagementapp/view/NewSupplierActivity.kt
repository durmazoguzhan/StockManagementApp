package com.oguzhandurmaz.stockmanagementapp.view

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityNewSupplierBinding

class NewSupplierActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewSupplierBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewSupplierBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firestore=Firebase.firestore
    }

    fun addNewSupplierClicked(view:View){
        val name = binding.supplierNameText.text.toString()
        val address = binding.supplierAddressText.text.toString()
        if (name.isEmpty() || address.isEmpty())
            Toast.makeText(this, "Please enter informations.", Toast.LENGTH_SHORT).show()
        else {
            val postMap = hashMapOf<String, String>()
            postMap.put("supplierName", binding.supplierNameText.text.toString())
            postMap.put("supplierAddress", binding.supplierAddressText.text.toString())

            firestore.collection("Suppliers").add(postMap).addOnSuccessListener {
                Toast.makeText(this, "new supplier added successfully.", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {exception->
                Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
}