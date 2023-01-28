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
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityRemoveStockBinding
import com.oguzhandurmaz.stockmanagementapp.model.Product
import com.oguzhandurmaz.stockmanagementapp.model.Supplier
import com.squareup.picasso.Picasso

class RemoveStockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemoveStockBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var activityBarcodeResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedBarcode: Bitmap? = null
    private lateinit var storeArrayList: ArrayList<Supplier>
    private lateinit var storeNameList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoveStockBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firestore = Firebase.firestore
        storeArrayList = ArrayList()
        storeNameList = ArrayList()

        registerLauncher()
        getStores()

        barcodeTextChanged()
    }

    fun removeFromStockClicked(view: View) {
        val productName = binding.productNameText3.text.toString()
        if (productName.isEmpty())
            Toast.makeText(this, "Please add new product before remove stock", Toast.LENGTH_SHORT)
                .show()
        else {
            val barcode = binding.barcodeText3.text.toString()

            var totalProductStock = 0
            var addedProductStock = 0
            var removedProductStock = 0

            firestore.collection("AddedStocks").whereEqualTo("barcode", barcode)
                .addSnapshotListener { value, error ->
                    if (error != null)
                        Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                    else {
                        if (value != null) {
                            if (!value.isEmpty) {
                                val documents = value.documents
                                documents.forEach { document ->
                                    addedProductStock += (document.get("addedStock") as Number).toInt()
                                }
                            }
                        }
                    }
                }

            firestore.collection("RemovedStocks").whereEqualTo("barcode", barcode)
                .get().addOnFailureListener { error ->
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()}
                .addOnSuccessListener { value->
                    if (value != null) {
                        if (!value.isEmpty) {
                            val documents = value.documents
                            documents.forEach { document ->
                                removedProductStock += (document.get("removedStock") as Number).toInt()
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    totalProductStock = addedProductStock - removedProductStock

                    if (totalProductStock >= binding.removedStockText.text.toString().toInt()) {
                        val removeStockMap = hashMapOf<String, Any>()
                        removeStockMap.put("barcode", binding.barcodeText3.text.toString())
                        removeStockMap.put("removedStock", binding.removedStockText.text.toString().toInt())
                        removeStockMap.put("date", Timestamp.now().toDate())
                        val selectedItemId = binding.storesSpinner.selectedItemId.toInt()
                        removeStockMap.put("storeDocumentId", storeArrayList[selectedItemId].documentId)

                        firestore.collection("RemovedStocks").add(removeStockMap).addOnSuccessListener {
                            Toast.makeText(this, "removed stocks successfully.", Toast.LENGTH_SHORT).show()
                            finish()
                        }.addOnFailureListener { ex ->
                            Toast.makeText(this, ex.localizedMessage, Toast.LENGTH_SHORT).show()
                        }
                    } else
                        Toast.makeText(
                            this,
                            "Not enough stocks. Total stock now: $totalProductStock",
                            Toast.LENGTH_SHORT
                        ).show()
                }
        }
    }

    private fun getStores() {
        firestore.collection("Stores").orderBy(
            "storeName",
            Query.Direction.ASCENDING
        ).addSnapshotListener { value, error ->
            if (error != null)
                Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
            else {
                if (value != null) {
                    if (!value.isEmpty) {
                        val documents = value.documents
                        storeArrayList.clear()
                        documents.forEach { document ->
                            val documentId = document.id
                            val supplierName = document.get("storeName") as String
                            val supplierAddress = document.get("storeAddress") as String
                            val supplier = Supplier(documentId, supplierName, supplierAddress)
                            storeArrayList.add(supplier)
                            storeNameList.add(supplierName)
                        }
                        val adapter = ArrayAdapter(
                            this,
                            R.layout.spinner_item,
                            storeNameList
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        binding.storesSpinner.adapter = adapter
                    }
                }
            }
        }
    }

    private fun barcodeTextChanged() {
        binding.barcodeText3.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (binding.barcodeText3.text.toString().length == 13) {
                    getProductByBarcode(binding.barcodeText3.text.toString())
                }
            }
        })
    }

    fun barcodeImageClicked(view: View) {
        if (checkCameraPermission(view))
            intentToBarcode()
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
                                binding.productBrandText3.setText(product.productBrand)
                                binding.productNameText3.setText(product.productName)
                                Picasso.get().load(product.productImageUrl)
                                    .into(binding.productImage3)
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
                                        binding.barcodeText3.setText(barcodes[0].displayValue)
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