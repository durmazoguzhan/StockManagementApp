package com.oguzhandurmaz.stockmanagementapp.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.oguzhandurmaz.stockmanagementapp.databinding.ActivityNewProductBinding
import java.io.ByteArrayOutputStream
import java.util.*

class NewProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewProductBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var activityBarcodeResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedBarcode: Bitmap? = null
    private var selectedPicture: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewProductBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        firestore = Firebase.firestore
        storage = Firebase.storage

        registerLauncher()
    }

    fun addNewProductClicked(view: View) {
            val barcode = binding.barcodeText.text.toString()
            val brand = binding.productBrandText.text.toString()
            val name = binding.productNameText.text.toString()
            if (barcode.isEmpty() || brand.isEmpty() || name.isEmpty())
                Toast.makeText(this, "Please enter informations.", Toast.LENGTH_SHORT).show()
            else{
                val uuid = UUID.randomUUID()
                val imageName = "$uuid.jpeg"
                val reference = storage.reference
                val imageReference = reference.child("ProductImages").child(imageName)
                if (selectedPicture != null) {
                    val baos = ByteArrayOutputStream()
                    selectedPicture!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()
                    imageReference.putBytes(data).addOnSuccessListener {
                        //download url
                        val uploadPictureReference = storage.reference.child("ProductImages").child(imageName)
                        uploadPictureReference.downloadUrl.addOnSuccessListener {

                            val downloadUrl = it.toString()
                            val productMap = hashMapOf<String, Any>()
                            productMap.put("barcode",binding.barcodeText.text.toString())
                            productMap.put("productBrand",binding.productBrandText.text.toString())
                            productMap.put("productName",binding.productNameText.text.toString())
                            productMap.put("productImageUrl", downloadUrl)

                            firestore.collection("Products").add(productMap).addOnSuccessListener {
                                Toast.makeText(this, "new product added successfully.", Toast.LENGTH_SHORT).show()
                                finish()
                            }.addOnFailureListener {ex->
                                Toast.makeText(this, ex.localizedMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    fun productImageClicked(view: View) {
        if (checkCameraPermission(view))
            intentToCamera()
    }

    fun barcodeImageClicked(view: View) {

        if (checkCameraPermission(view))
            intentToBarcode()
    }

    private fun checkCameraPermission(view:View):Boolean{
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

    private fun registerLauncher() {
        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    intentFromResult?.let { intentFromResult ->
                        selectedPicture = intentFromResult.extras!!.get("data") as Bitmap
                        selectedPicture?.let {
                            binding.productImage.setImageBitmap(it)
                        }
                    }
                }
            }

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
                            val result = scanner.process(it,0)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.size==0)
                                        Toast.makeText(this, "The barcode didn't read.", Toast.LENGTH_SHORT).show()
                                    else{
                                        binding.barcodeText.setText(barcodes[0].displayValue)
                                    }
                                }
                                .addOnFailureListener {exception->
                                    Toast.makeText(this, exception.localizedMessage, Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    //granted
                    intentToCamera()
                } else {
                    //denied
                    Toast.makeText(this, "Permission needed!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun intentToCamera() {
        val intentToCamera =
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        activityResultLauncher.launch(intentToCamera)
    }
}