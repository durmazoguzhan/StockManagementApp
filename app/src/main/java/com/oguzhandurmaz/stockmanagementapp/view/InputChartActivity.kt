package com.oguzhandurmaz.stockmanagementapp.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityInputChartBinding
import com.oguzhandurmaz.stockmanagementapp.model.Product
import com.squareup.picasso.Picasso
import ir.mahozad.android.PieChart

class InputChartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputChartBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var activityBarcodeResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedBarcode: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputChartBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firestore = Firebase.firestore

        registerLauncher()
        barcodeTextChanged()

        binding.pieChart1.visibility = View.GONE
        binding.colorAdded.visibility = View.GONE
        binding.colorRemaining.visibility = View.GONE
        binding.colorRemoved.visibility = View.GONE
        binding.colorAddedText.visibility = View.GONE
        binding.colorRemovedText.visibility = View.GONE
        binding.colorRemainingText.visibility = View.GONE
    }

    fun checkStocksClicked(view: View) {
        val productName = binding.productNameText4.text.toString()
        if (productName.isEmpty())
            Toast.makeText(this, "Please add new product before check stock", Toast.LENGTH_SHORT)
                .show()
        else {
            val barcode = binding.barcodeText4.text.toString()

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
                    Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT).show()
                }
                .addOnSuccessListener { value ->
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

                    drawTotalProductChart(totalProductStock, addedProductStock, removedProductStock)
                }
        }

    }

    private fun drawTotalProductChart(totalStock: Int, addedStock: Int, removedStock: Int) {
        binding.pieChart1.visibility = View.VISIBLE
        binding.colorAdded.visibility = View.VISIBLE
        binding.colorRemaining.visibility = View.VISIBLE
        binding.colorRemoved.visibility = View.VISIBLE
        binding.colorAddedText.visibility = View.VISIBLE
        binding.colorRemovedText.visibility = View.VISIBLE
        binding.colorRemainingText.visibility = View.VISIBLE

        binding.colorAddedText.text = "Added Stock: $addedStock"
        binding.colorRemovedText.text = "Removed Stock: $removedStock"
        binding.colorRemainingText.text = "Remaining Stock: $totalStock"

        val pieChart = binding.pieChart1

        val totalChart = totalStock + addedStock + removedStock
        val totalStockRatio: Float = totalStock.toFloat() / totalChart.toFloat()
        val totalAddedRatio: Float = addedStock.toFloat() / totalChart.toFloat()
        val totalRemovedRatio: Float = removedStock.toFloat() / totalChart.toFloat()

        pieChart.apply {
            slices = listOf(
                PieChart.Slice(
                    totalAddedRatio,
                    Color.rgb(120, 181, 0),
                    Color.rgb(149, 224, 0)
                ),
                PieChart.Slice(
                    totalStockRatio,
                    Color.rgb(204, 168, 0),
                    Color.rgb(249, 228, 0)
                ),
                PieChart.Slice(
                    totalRemovedRatio,
                    Color.rgb(160, 165, 170),
                    Color.rgb(175, 180, 185)
                )
            )
            gradientType = PieChart.GradientType.RADIAL
            legendsIcon = PieChart.DefaultIcons.SQUARE
        }
    }

    private fun barcodeTextChanged() {
        binding.barcodeText4.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                if (binding.barcodeText4.text.toString().length == 13) {
                    getProductByBarcode(binding.barcodeText4.text.toString())
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
                                binding.productBrandText4.setText(product.productBrand)
                                binding.productNameText4.setText(product.productName)
                                Picasso.get().load(product.productImageUrl)
                                    .into(binding.productImage4)
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
                                        binding.barcodeText4.setText(barcodes[0].displayValue)
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