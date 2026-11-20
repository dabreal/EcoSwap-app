package com.example.ecoswap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val repository: ProductRepository
) : AndroidViewModel(application) {

    val products: StateFlow<List<Product>> =
        repository.products.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )
    init {
        viewModelScope.launch {
            repository.initializeSampleProductsIfEmpty()  
        }
    }
    fun onFavoriteClick(id: Long) {
        viewModelScope.launch { repository.toggleFavorite(id) }
    }

    fun addProduct(
        title: String,
        ownerName: String,
        pickupPlace: String,
        description: String,
        imageUri: String?,
        latitude: Double?,
        longitude: Double?,
        state: String = "NUEVO"
    ) {
        viewModelScope.launch {
            val id = repository.addProduct(
                title = title,
                ownerName = ownerName,
                pickupPlace = pickupPlace,
                description = description,
                imageUri = imageUri,
                latitude = latitude,
                longitude = longitude,
                state = state
            )
            EcoSwapNotifications.notifyNewProduct(getApplication(), title, id)
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch { repository.deleteProduct(id) }
    }

    fun deleteAllProducts() {
        viewModelScope.launch { repository.deleteAllProducts() }
    }

    fun toggleRequested(id: Long) {
        viewModelScope.launch { repository.toggleRequested(id) }
    }
}