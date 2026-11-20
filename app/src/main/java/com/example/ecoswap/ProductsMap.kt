package com.example.ecoswap

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.cos
import kotlin.math.sin

private const val DEFAULT_LAT = 39.511
private const val DEFAULT_LON = -0.422
private const val MIN_ZOOM = 5.0
private const val MAX_ZOOM = 19.0

private fun MapView.disableSurfaceOverlap() {
    fun walk(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            when (val child = viewGroup.getChildAt(i)) {
                is SurfaceView -> child.setZOrderMediaOverlay(false)
                is ViewGroup -> walk(child)
            }
        }
    }
    walk(this)
}

@Composable
fun ProductsMap(
    modifier: Modifier = Modifier,
    products: List<Product>,
    focusProductId: Long? = null,
    isActive: Boolean = true,
    onMarkerClick: (Product) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val productsWithLocation = remember(products) {
        products.filter { it.latitude != null && it.longitude != null }
    }
    val withoutLocationCount = remember(products) {
        products.count { it.latitude == null || it.longitude == null }
    }

    val markersKey = remember(productsWithLocation) {
        productsWithLocation.joinToString("|") { "${it.id}:${it.latitude}:${it.longitude}" }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(12.5)
            controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LON))
            disableSurfaceOverlap()
        }
    }

    val onMarkerClickState by rememberUpdatedState(onMarkerClick)

    fun zoomIn() {
        val next = (mapView.zoomLevelDouble + 1).coerceAtMost(MAX_ZOOM)
        mapView.controller.setZoom(next)
        mapView.invalidate()
    }

    fun zoomOut() {
        val next = (mapView.zoomLevelDouble - 1).coerceAtLeast(MIN_ZOOM)
        mapView.controller.setZoom(next)
        mapView.invalidate()
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            mapView.onResume()
            mapView.postInvalidate()
        } else {
            mapView.onPause()
        }
    }

    LaunchedEffect(markersKey) {
        val toRemove = mapView.overlays.filterIsInstance<Marker>()
        toRemove.forEach { mapView.overlays.remove(it) }

        val groups = productsWithLocation.groupBy { it.latitude!! to it.longitude!! }
        groups.forEach { (latLon, group) ->
            val (baseLat, baseLon) = latLon
            val n = group.size

            group.forEachIndexed { index, product ->
                val (lat, lon) =
                    if (n <= 1) {
                        baseLat to baseLon
                    } else {
                        val radius = 0.00006
                        val angle = (2.0 * Math.PI * index) / n.toDouble()
                        (baseLat + radius * cos(angle)) to (baseLon + radius * sin(angle))
                    }

                val marker = Marker(mapView).apply {
                    position = GeoPoint(lat, lon)
                    title = product.title
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    setOnMarkerClickListener { _, _ ->
                        onMarkerClickState(product)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }

        mapView.invalidate()
    }

    var cameraInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(markersKey, cameraInitialized) {
        if (cameraInitialized) return@LaunchedEffect
        val start = productsWithLocation.firstOrNull()?.let {
            GeoPoint(it.latitude!!, it.longitude!!)
        } ?: GeoPoint(DEFAULT_LAT, DEFAULT_LON)
        mapView.controller.setCenter(start)
        mapView.controller.setZoom(12.5)
        cameraInitialized = true
        mapView.invalidate()
    }

    LaunchedEffect(focusProductId, productsWithLocation) {
        val id = focusProductId ?: return@LaunchedEffect
        val p = productsWithLocation.firstOrNull { it.id == id } ?: return@LaunchedEffect
        mapView.controller.setCenter(GeoPoint(p.latitude!!, p.longitude!!))
        mapView.controller.setZoom(15.0)
        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView },
            update = { view ->
                view.disableSurfaceOverlap()
            }
        )

        Text(
            text = "Mapa: ${productsWithLocation.size}/${products.size} con ubicación" +
                    if (withoutLocationCount > 0) " • $withoutLocationCount sin ubicación" else "",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(2f)
                .padding(start = 12.dp, bottom = 16.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .zIndex(2f)
                .padding(end = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = { zoomIn() },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Acercar")
            }
            FilledIconButton(
                onClick = { zoomOut() },
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Alejar")
            }
        }

        if (productsWithLocation.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No hay productos con ubicación aún",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Añade un producto usando “Usar mi ubicación” para que aparezca en el mapa.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (isActive) mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
}
