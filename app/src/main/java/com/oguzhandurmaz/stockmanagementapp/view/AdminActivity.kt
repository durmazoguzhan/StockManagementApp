package com.oguzhandurmaz.stockmanagementapp.view

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.oguzhandurmaz.stockmanagementapp.R
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        auth = Firebase.auth
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.signout_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_out) {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    fun newProductClicked(view: View) {
        val intent=Intent(this,NewProductActivity::class.java)
        startActivity(intent)
    }

    fun newSupplierClicked(view: View) {
        val intent = Intent(this, NewSupplierActivity::class.java)
        startActivity(intent)
    }

    fun newStoreClicked(view: View) {
        val intent = Intent(this, NewStoreActivity::class.java)
        startActivity(intent)
    }

    fun chartsClicked(view: View) {
        val intent = Intent(this, InputChartActivity::class.java)
        startActivity(intent)
    }
}