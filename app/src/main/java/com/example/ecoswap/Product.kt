package com.example.ecoswap

enum class ProductState {
    NUEVO,
    COMO_NUEVO,
    USADO,
    MAL_ESTADO
}

data class Product(
    val id: Long,
    val title: String,
    val ownerName: String,
    val pickupPlace: String,
    val description: String,
    val isFavorite: Boolean = false,
    val imageUri: String? = null,
    val isRequested: Boolean = false,
    val latitude: Double? = null,      
    val longitude: Double? = null,
    val state: ProductState
)