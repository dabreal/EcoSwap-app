package com.example.ecoswap

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val ownerName: String,
    val pickupPlace: String,
    val description: String,
    val isFavorite: Boolean = false,
    val imageUri: String? = null,
    val isRequested: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val state: String = "NUEVO"
)

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    title = title,
    ownerName = ownerName,
    pickupPlace = pickupPlace,
    description = description,
    isFavorite = isFavorite,
    imageUri = imageUri,
    isRequested = isRequested,
    latitude = latitude,
    longitude = longitude,
    state = try {
        ProductState.valueOf(state)
    } catch (e: Exception) {
        ProductState.NUEVO
    }
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    title = title,
    ownerName = ownerName,
    pickupPlace = pickupPlace,
    description = description,
    isFavorite = isFavorite,
    imageUri = imageUri,
    isRequested = isRequested,
    latitude = latitude,
    longitude = longitude,
    state = state.name
)