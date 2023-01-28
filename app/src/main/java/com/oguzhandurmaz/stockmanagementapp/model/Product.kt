package com.oguzhandurmaz.stockmanagementapp.model
// product model
data class Product(
    val documentId: String,
    val barcode: String,
    val productBrand: String,
    val productName: String,
    val productImageUrl: String
)