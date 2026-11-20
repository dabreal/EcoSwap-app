package com.example.ecoswap

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val productDao: ProductDao
) {

    // Flow con la lista de productos de dominio
    val products: Flow<List<Product>> =
        productDao.getAllProducts()
            .map { entities -> entities.map(ProductEntity::toDomain) }

    suspend fun addProduct(
        title: String,
        ownerName: String,
        pickupPlace: String,
        description: String,
        imageUri: String?,
        latitude: Double?,
        longitude: Double?,
        state: String = "NUEVO"
    ): Long {
        val entity = ProductEntity(
            title = title,
            ownerName = ownerName,
            pickupPlace = pickupPlace,
            description = description,
            isFavorite = false,
            imageUri = imageUri,
            isRequested = false,
            latitude = latitude,
            longitude = longitude,
            state = state
        )
        return productDao.insertProduct(entity)
    }

    suspend fun toggleFavorite(id: Long) {
        val entity = productDao.getProductById(id) ?: return
        productDao.updateProduct(
            entity.copy(isFavorite = !entity.isFavorite)
        )
    }

    suspend fun toggleRequested(id: Long) {
        val entity = productDao.getProductById(id) ?: return
        productDao.updateProduct(
            entity.copy(isRequested = !entity.isRequested)
        )
    }

    suspend fun deleteProduct(id: Long) {
        productDao.deleteById(id)
    }

    suspend fun deleteAllProducts() {
        productDao.deleteAll()
    }

    suspend fun getProductById(id: Long): Product? =
        productDao.getProductById(id)?.toDomain()

    suspend fun initializeSampleProductsIfEmpty() {
        if (productDao.getCount() == 0) {
            initializeSampleProducts()
            return
        }

        backfillSampleLocationsIfMissing()
    }   

    private suspend fun backfillSampleLocationsIfMissing() {
        val all = productDao.getAllProductsOnce()
        if (all.any { it.latitude != null && it.longitude != null }) return

        val titleToLatLon: Map<String, Pair<Double, Double>> = mapOf(
            "Bicicleta de montaña" to (41.3809 to 2.1228),
            "Lámpara de escritorio LED" to (41.3920 to 2.1649),
            "Mochila para senderismo" to (41.3892 to 2.1800),
            "Auriculares Bluetooth" to (41.4036 to 2.1744),
            "Zapatillas deportivas" to (41.3751 to 2.1492)
        )

        all.forEach { e ->
            val ll = titleToLatLon[e.title] ?: return@forEach
            if (e.latitude == null || e.longitude == null) {
                productDao.updateProduct(e.copy(latitude = ll.first, longitude = ll.second))
            }
        }
    }

    suspend fun initializeSampleProducts() {
        val sampleProducts = listOf(
            ProductEntity(
                title = "Bicicleta de montaña",
                ownerName = "Juan García",
                pickupPlace = "Centro comercial Plaza Mayor",
                description = "Bicicleta de montaña en buen estado, 21 velocidades, frenos de disco.",
                imageUri = null,
                latitude = 41.3809,
                longitude = 2.1228,
                state = "COMO_NUEVO"
            ),
            ProductEntity(
                title = "Lámpara de escritorio LED",
                ownerName = "María López",
                pickupPlace = "Parque central, zona de juegos",
                description = "Lámpara LED ajustable, 3 modos de brillo. Perfecta para trabajar o leer.",
                imageUri = null,
                latitude = 41.3920,
                longitude = 2.1649,
                state = "NUEVO"
            ),
            ProductEntity(
                title = "Mochila para senderismo",
                ownerName = "Carlos Rodríguez",
                pickupPlace = "Estación de tren",
                description = "Mochila impermeable de 40L, múltiples compartimentos, correas acolchadas.",
                imageUri = "https://www.google.com/url?sa=t&source=web&rct=j&url=https%3A%2F%2Fwww.amazon.es%2FRAINSMORE-Senderismo-Impermeable-Excursiones-Acampadas%2Fdp%2FB0BXSWPWKH&ved=0CBYQjRxqFwoTCNC46IrvsJQDFQAAAAAdAAAAABAF&opi=89978449",
                latitude = 41.3892,
                longitude = 2.1800,
                state = "USADO"
            ),
            ProductEntity(
                title = "Auriculares Bluetooth",
                ownerName = "Sofia Martínez",
                pickupPlace = "Centro comercial Luz",
                description = "Auriculares inalámbricos con cancelación de ruido, batería 20h.",
                imageUri = null,
                latitude = 41.4036,
                longitude = 2.1744,
                state = "COMO_NUEVO"
            ),
            ProductEntity(
                title = "Zapatillas deportivas",
                ownerName = "Pedro Sánchez",
                pickupPlace = "Polideportivo municipal",
                description = "Zapatillas de running profesionales, talla 42, poco uso.",
                imageUri = null,
                latitude = 41.3751,
                longitude = 2.1492,
                state = "COMO_NUEVO"
            )
        )
        sampleProducts.forEach { productDao.insertProduct(it) }
    }
}