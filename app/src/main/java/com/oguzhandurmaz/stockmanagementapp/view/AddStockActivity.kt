package com.oguzhandurmaz.stockmanagementapp.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.oguzhandurmaz.stockmanagementapp.R
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityAddStockBinding
import com.oguzhandurmaz.stockmanagementapp.model.Product
import com.oguzhandurmaz.stockmanagementapp.model.Supplier
import com.squareup.picasso.Picasso

class AddStockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStockBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var activityBarcodeResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedBarcode: Bitmap? = null
    private lateinit var supplierArrayList: ArrayList<Supplier>
    private lateinit var supplierNameList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStockBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //initialize
        firestore = Firebase.firestore
        supplierArrayList = ArrayList()
        supplierNameList = ArrayList()

        registerLauncher()
        getSuppliers()

        totalPriceTextChanged()
        barcodeTextChanged()
    }

    private fun barcodeTextChanged() {
        binding.barcodeText2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (binding.barcodeText2.text.toString().length == 13) {
                    getProductByBarcode(binding.barcodeText2.text.toString())
                }
            }
        })
    }

    private fun totalPriceTextChanged() {
        binding.totalPriceText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (binding.addedStockText.text.toString()
                        .isNotEmpty() && binding.totalPriceText.text.toString().isNotEmpty()
                ) {
                    val totalPrice: Double = binding.totalPriceText.text.toString().toDouble()
                    val addedStock: Double = binding.addedStockText.text.toString().toDouble()
                    val unitPrice: Double = totalPrice / addedStock
                    binding.unitPriceText.setText(unitPrice.toString())
                }
            }
        })
    }

    fun barcodeImageClicked(view: View) {
        if (checkCameraPermission(view))
            intentToBarcode()
    }

    fun addToStockClicked(view: View) {
        val productName = binding.productNameText2.text.toString()
        if (productName.isEmpty())
            Toast.makeText(this, "Please add new product before add stock", Toast.LENGTH_SHORT)
                .show()
        else {
            val addedStock = binding.addedStockText.text.toString()
            val unitPrice = binding.unitPriceText.text.toString()
            val totalPrice = binding.totalPriceText.text.toString()
            if (addedStock.isEmpty() || unitPrice.isEmpty() || totalPrice.isEmpty())
                Toast.makeText(this, "Please enter informations.", Toast.LENGTH_SHORT).show()
            else{
                val addStockMap = hashMapOf<String, Any>()
                addStockMap.put("barcode", binding.barcodeText2.text.toString())
                addStockMap.put("addedStock", binding.addedStockText.text.toString().toInt())
                addStockMap.put("date", Timestamp.now().toDate())
                addStockMap.put("unitPrice", binding.unitPriceText.text.toString().toDouble())
                addStockMap.put("totalPrice", binding.totalPriceText.text.toString().toDouble())
                val selectedItemId = binding.suppliersSpinner.selectedItemId.toInt()
                addStockMap.put("supplierDocumentId", supplierArrayList[selectedItemId].documentId)

                firestore.collection("AddedStocks").add(addStockMap).addOnSuccessListener {
                    Toast.makeText(this, "added stocks successfully.", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { ex ->
                    Toast.makeText(this, ex.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getSuppliers() {
        firestore.collection("Suppliers").orderBy(
            "supplierName",
            Query.Direction.ASCENDING
        ).addSnapshotListener { value, error ->
            if (error != null)
                Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
            else {
                if (value != null) {
                    if (!value.isEmpty) {
                        val documents = value.documents
                        supplierArrayList.clear()
                        documents.forEach { document ->
                            val documentId = document.id
                            val supplierName = document.get("supplierName") as String
                            val supplierAddress = document.get("supplierAddress") as String
                            val supplier = Supplier(documentId, supplierName, supplierAddress)
                            supplierArrayList.add(supplier)
                            supplierNameList.add(supplierName)
                        }
                        val adapter = ArrayAdapter(
                            this,
                            R.layout.spinner_item,
                            supplierNameList
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.suppliersSpinner.adapter = adapter
                    }
                }
            }
        }
    }

    private fun checkCameraPermission(view: View): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                Snackbar.make(
                    view,
                    "Permission needed for access to camera",
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction("Give permission") {
                        //request permission
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }.show()
            } else {
                //request permission
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            return false
        } else {
            return true
        }
    }

    private fun intentToBarcode() {
        val intentToBarcode =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        activityBarcodeResultLauncher.launch(intentToBarcode)
    }

    private fun getProductByBarcode(barcode: String) {
        firestore.collection("Products").whereEqualTo("barcode", barcode)
            .addSnapshotListener { value, error ->
                if (error != null)
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                else {
                    if (value != null) {
                        if (!value.isEmpty) {
                            val documents = value.documents
                            documents.forEach { document ->
                                val documentId = document.id
                                val barcode = document.get("barcode") as String
                                val productBrand = document.get("productBrand") as String
                                val productName = document.get("productName") as String
                                val productImageUrl = document.get("productImageUrl") as String
                                val product = Product(
                                    documentId,
                                    barcode,
                                    productBrand,
                                    productName,
                                    productImageUrl
                                )
                                binding.productBrandText2.setText(product.productBrand)
                                binding.productNameText2.setText(product.productName)
                                Picasso.get().load(product.productImageUrl)
                                    .into(binding.productImage2)
                            }
                        }
                    }
                }
            }
    }

    private fun registerLauncher() {

        activityBarcodeResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    intentFromResult?.let { intentFromResult ->
                        selectedBarcode = intentFromResult.extras!!.get("data") as Bitmap
                        selectedBarcode?.let {
                            val options = BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(
                                    Barcode.FORMAT_EAN_13
                                )
                                .build()
                            val scanner = BarcodeScanning.getClient(options)
                            val result = scanner.process(it, 0)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.size == 0)
                                        Toast.makeText(
                                            this,
                                            "The barcode didn't read.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    else {
                                        binding.barcodeText2.setText(barcodes[0].displayValue)
                                        getProductByBarcode(barcodes[0].displayValue!!)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Toast.makeText(
                                        this,
                                        exception.localizedMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //granted
                    intentToBarcode()
                } else {
                    //denied
                    Toast.makeText(this, "Permission needed!", Toast.LENGTH_SHORT).show()
                }
            }
    }
}