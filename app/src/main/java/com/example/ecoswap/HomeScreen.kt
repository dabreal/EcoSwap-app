package com.example.ecoswap

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ecoswap.ui.components.EcoMapHeader
import com.example.ecoswap.ui.components.EcoSectionCard
import com.example.ecoswap.ui.components.EcoSwapBottomBar
import com.example.ecoswap.ui.components.EcoSwapHeader
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.net.toUri

private enum class HomeSortMode { RECENT, DISTANCE, TITLE, OWNER, STATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (Long) -> Unit,
    homeViewModel: HomeViewModel,
    startTab: Int = 0,
    focusProductId: Long? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val products by homeViewModel.products.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(startTab.coerceIn(0, 2)) }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    var promptedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* si concede POST_NOTIFICATIONS, el alta mostrará la notificación */ }

    var filterStateName by rememberSaveable { mutableStateOf<String?>(null) }
    var ownerQuery by rememberSaveable { mutableStateOf("") }
    var sortModeName by rememberSaveable { mutableStateOf(HomeSortMode.RECENT.name) }
    var userLocation by remember { mutableStateOf<android.location.Location?>(null) }

    val filterState: ProductState? = filterStateName?.let { name ->
        runCatching { ProductState.valueOf(name) }.getOrNull()
    }
    val sortMode: HomeSortMode = runCatching { HomeSortMode.valueOf(sortModeName) }
        .getOrElse { HomeSortMode.RECENT }

    LaunchedEffect(startTab, focusProductId) {
        if (startTab in 0..2) selectedTab = startTab
        if (focusProductId != null) selectedTab = 2
    }

    LaunchedEffect(selectedTab, promptedNotificationPermission) {
        if (selectedTab != 1 || promptedNotificationPermission) return@LaunchedEffect
        promptedNotificationPermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val baseProducts = remember(products, showOnlyFavorites) {
        if (showOnlyFavorites) products.filter { it.isFavorite } else products
    }

    val displayedProducts = remember(baseProducts, filterState, ownerQuery, sortMode, userLocation) {
        var list = baseProducts

        val st = filterState
        if (st != null) {
            list = list.filter { it.state == st }
        }
        val q = ownerQuery.trim()
        if (q.isNotEmpty()) {
            list = list.filter { it.ownerName.contains(q, ignoreCase = true) }
        }

        list = when (sortMode) {
            HomeSortMode.RECENT -> list.sortedByDescending { it.id }
            HomeSortMode.TITLE -> list.sortedBy { it.title.lowercase() }
            HomeSortMode.OWNER -> list.sortedBy { it.ownerName.lowercase() }
            HomeSortMode.STATE -> list.sortedBy { it.state.ordinal }
            HomeSortMode.DISTANCE -> {
                val loc = userLocation
                if (loc == null) list
                else {
                    list.sortedBy { p ->
                        val lat = p.latitude
                        val lon = p.longitude
                        if (lat == null || lon == null) Double.POSITIVE_INFINITY
                        else {
                            val dLat = (lat - loc.latitude)
                            val dLon = (lon - loc.longitude)
                            dLat * dLat + dLon * dLon
                        }
                    }
                }
            }
        }

        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (selectedTab == 2) {
            EcoMapHeader(
                showOnlyFavorites = showOnlyFavorites,
                onToggleFavorites = { showOnlyFavorites = !showOnlyFavorites }
            )
        } else {
            EcoSwapHeader(
                showOnlyFavorites = showOnlyFavorites,
                onToggleFavorites = { showOnlyFavorites = !showOnlyFavorites },
                onClearDatabase = { confirmDeleteAll = true }
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds(),
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                ) togetherWith fadeOut(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            },
            label = "homeTabs"
        ) { tab ->
            when (tab) {
                0 -> ProductsList(
                    modifier = Modifier.fillMaxSize(),
                    products = displayedProducts,
                    onFavoriteClick = { id -> homeViewModel.onFavoriteClick(id) },
                    onProductClick = onProductClick,
                    onDeleteClick = { id -> homeViewModel.deleteProduct(id) },
                    headerContent = {
                        HomeFiltersBar(
                            products = baseProducts,
                            filterState = filterState,
                            onFilterStateChange = { filterStateName = it?.name },
                            ownerQuery = ownerQuery,
                            onOwnerQueryChange = { ownerQuery = it },
                            sortMode = sortMode,
                            onSortModeChange = { sortModeName = it.name },
                            onRequestLocationForDistance = { ctx ->
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    ctx,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarseGranted = ContextCompat.checkSelfPermission(
                                    ctx,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted || coarseGranted) {
                                    scope.launch {
                                        userLocation = LocationUtils.getCurrentLocation(ctx)
                                    }
                                }
                            }
                        )
                    }
                )

                1 -> AddProductForm(
                    modifier = Modifier.fillMaxSize(),
                    onSubmit = { title, ownerName, pickupPlace, description, imageUri, lat, lon, state ->
                        homeViewModel.addProduct(
                            title,
                            ownerName,
                            pickupPlace,
                            description,
                            imageUri,
                            lat,
                            lon,
                            state
                        )
                        selectedTab = 0
                    }
                )

                2 -> MapTabPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    products = displayedProducts,
                    onProductClick = onProductClick,
                    focusProductId = focusProductId
                )

                else -> Box(Modifier.fillMaxSize())
            }
        }

        EcoSwapBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Vaciar productos") },
            text = { Text("Se eliminarán todos los productos guardados en Room. ¿Continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        homeViewModel.deleteAllProducts()
                        confirmDeleteAll = false
                        selectedTab = 0
                    }
                ) { Text("Vaciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HomeFiltersBar(
    products: List<Product>,
    filterState: ProductState?,
    onFilterStateChange: (ProductState?) -> Unit,
    ownerQuery: String,
    onOwnerQueryChange: (String) -> Unit,
    sortMode: HomeSortMode,
    onSortModeChange: (HomeSortMode) -> Unit,
    onRequestLocationForDistance: (android.content.Context) -> Unit
) {
    val context = LocalContext.current
    var stateExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }
    var panelExpanded by rememberSaveable { mutableStateOf(false) }

    val total = products.size
    val withLoc = remember(products) { products.count { it.latitude != null && it.longitude != null } }

    val estadoResumen = when (filterState) {
        null -> "Todos"
        ProductState.NUEVO -> "Nuevo"
        ProductState.COMO_NUEVO -> "Como nuevo"
        ProductState.USADO -> "Usado"
        ProductState.MAL_ESTADO -> "Mal estado"
    }
    val ordenResumen = when (sortMode) {
        HomeSortMode.RECENT -> "Recientes"
        HomeSortMode.DISTANCE -> "Cercanía"
        HomeSortMode.TITLE -> "Título"
        HomeSortMode.OWNER -> "Usuario"
        HomeSortMode.STATE -> "Estado"
    }
    val resumenCompacto = buildString {
        append("$total prod. • $estadoResumen • $ordenResumen")
        if (ownerQuery.isNotBlank()) append(" • \"${ownerQuery.trim()}\"")
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { panelExpanded = !panelExpanded }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (panelExpanded) "Filtros y orden" else "Filtros",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (panelExpanded) "$total productos • $withLoc con ubicación"
                        else resumenCompacto,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (panelExpanded) 2 else 1
                    )
                }
                Icon(
                    imageVector = if (panelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (panelExpanded) "Ocultar filtros" else "Mostrar filtros",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = panelExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "$total productos • $withLoc con ubicación",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            OutlinedButton(onClick = { stateExpanded = true }) {
                                Text(
                                    text = when (filterState) {
                                        null -> "Estado: Todos"
                                        ProductState.NUEVO -> "Estado: Nuevo"
                                        ProductState.COMO_NUEVO -> "Estado: Como nuevo"
                                        ProductState.USADO -> "Estado: Usado"
                                        ProductState.MAL_ESTADO -> "Estado: Mal estado"
                                    }
                                )
                            }
                            DropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Todos") },
                                    onClick = { onFilterStateChange(null); stateExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Nuevo") },
                                    onClick = { onFilterStateChange(ProductState.NUEVO); stateExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Como nuevo") },
                                    onClick = { onFilterStateChange(ProductState.COMO_NUEVO); stateExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Usado") },
                                    onClick = { onFilterStateChange(ProductState.USADO); stateExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Mal estado") },
                                    onClick = { onFilterStateChange(ProductState.MAL_ESTADO); stateExpanded = false }
                                )
                            }
                        }

                        Box {
                            OutlinedButton(onClick = { sortExpanded = true }) {
                                Text(
                                    text = when (sortMode) {
                                        HomeSortMode.RECENT -> "Orden: Recientes"
                                        HomeSortMode.DISTANCE -> "Orden: Cercanía"
                                        HomeSortMode.TITLE -> "Orden: Título"
                                        HomeSortMode.OWNER -> "Orden: Usuario"
                                        HomeSortMode.STATE -> "Orden: Estado"
                                    }
                                )
                            }
                            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Recientes") },
                                    onClick = { onSortModeChange(HomeSortMode.RECENT); sortExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cercanía") },
                                    onClick = {
                                        onSortModeChange(HomeSortMode.DISTANCE)
                                        onRequestLocationForDistance(context)
                                        sortExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Título") },
                                    onClick = { onSortModeChange(HomeSortMode.TITLE); sortExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Usuario") },
                                    onClick = { onSortModeChange(HomeSortMode.OWNER); sortExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Estado") },
                                    onClick = { onSortModeChange(HomeSortMode.STATE); sortExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = ownerQuery,
                        onValueChange = onOwnerQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Filtrar por usuario (owner)") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProductsList(
    modifier: Modifier = Modifier,
    products: List<Product>,
    onFavoriteClick: (Long) -> Unit,
    onProductClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    headerContent: @Composable (() -> Unit)? = null
) {
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = products.isEmpty(),
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(260, easing = FastOutSlowInEasing)
                ) togetherWith fadeOut(
                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                )
            },
            label = "productsOrEmpty"
        ) { isEmpty ->
            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EcoSectionCard {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "No hay productos disponibles",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "¡Empieza a compartir! Añade tu primer objeto desde la pestaña Añadir.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (headerContent != null) {
                        item(key = "filters_header") { headerContent() }
                    }
                    items(products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onFavoriteClick = { onFavoriteClick(product.id) },
                            onClick = { onProductClick(product.id) },
                            onLongClick = { productToDelete = product }
                        )
                    }
                }
            }
        }

        if (productToDelete != null) {
            AlertDialog(
                onDismissRequest = { productToDelete = null },
                title = { Text("Eliminar producto") },
                text = { Text("¿Seguro que quieres eliminar \"${productToDelete!!.title}\"?") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteClick(productToDelete!!.id)
                        productToDelete = null
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { productToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductCard(
    modifier: Modifier = Modifier,
    product: Product,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val stateColor = when (product.state) {
        ProductState.NUEVO -> MaterialTheme.colorScheme.primary
        ProductState.COMO_NUEVO -> MaterialTheme.colorScheme.tertiary
        ProductState.USADO -> MaterialTheme.colorScheme.secondary
        ProductState.MAL_ESTADO -> MaterialTheme.colorScheme.error
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        stateColor,
                        RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    //estado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        //estado del producto (NUEVO, USADO, etc)
                        val stateText = when (product.state) {
                            ProductState.NUEVO ->  "Nuevo"
                            ProductState.COMO_NUEVO -> "Como nuevo"
                            ProductState.USADO -> "Usado"
                            ProductState.MAL_ESTADO -> "Mal estado"
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text(stateText, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = stateColor.copy(alpha = 0.15f),
                                labelColor = stateColor
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        //Libre/Solicitado
                        val statusText = if (product.isRequested) "Solicitado" else "Libre"
                        val statusColor =
                            if (product.isRequested) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.primary

                        AssistChip(
                            onClick = { },
                            label = { Text(statusText, fontSize = MaterialTheme.typography.labelSmall.fontSize) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = statusColor
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (product.isFavorite)
                            Icons.Default.Favorite
                        else
                            Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (product.isFavorite) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text("👤 ${product.ownerName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("📍 ${product.pickupPlace}", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (product.imageUri != null) {
                val uri = remember(product.imageUri) { product.imageUri.toUri() }
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Imagen del producto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                product.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
            }
        }
    }
}

@Composable
fun AddProductForm(
    modifier: Modifier = Modifier,
    onSubmit: (
        title: String,
        ownerName: String,
        pickupPlace: String,
        description: String,
        imageUri: String?,
        latitude: Double?,
        longitude: Double?,
        state: String
    ) -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var pickupPlace by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var selectedState by remember { mutableStateOf("NUEVO") }
    var expandedState by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var titleTouched by remember { mutableStateOf(false) }
    var ownerTouched by remember { mutableStateOf(false) }
    var pickupTouched by remember { mutableStateOf(false) }
    var descriptionTouched by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            //persistir permiso para que la imagen se siga viendo tras reiniciar la app
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                //si el proveedor no permite persistir, al menos no crashea
            }
        }
    }

    var pendingCameraCapture by remember { mutableStateOf<CameraPhotoTarget?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingCameraCapture
        pendingCameraCapture = null
        if (pending != null) {
            if (success) {
                selectedImageUri = pending.uri
            } else {
                pending.file.delete()
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching {
                val target = ProductImageCapture.createNewPhoto(context)
                pendingCameraCapture = target
                takePictureLauncher.launch(target.uri)
            }
        }
    }

    //ñauncher para permisos de ubicación
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        //cuando el usuario responde al diálogo de permisos
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationStatus = if (granted) {
            "Permisos concedidos, vuelve a pulsar 'Usar mi ubicación'"
        } else {
            "Permiso de ubicación denegado"
        }
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nuevo objeto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        EcoSectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                if (it.isNotBlank()) formError = null
            },
            label = { Text("Nombre del objeto") },
            modifier = Modifier.fillMaxWidth(),
            isError = titleTouched && title.isBlank(),
            supportingText = {
                if (titleTouched && title.isBlank()) {
                    Text(stringResource(R.string.field_required))
                }
            }
        )

        OutlinedTextField(
            value = ownerName,
            onValueChange = {
                ownerName = it
                if (it.isNotBlank()) formError = null
            },
            label = { Text("Nombre de contacto") },
            modifier = Modifier.fillMaxWidth(),
            isError = ownerTouched && ownerName.isBlank(),
            supportingText = {
                if (ownerTouched && ownerName.isBlank()) {
                    Text(stringResource(R.string.field_required))
                }
            }
        )

        OutlinedTextField(
            value = pickupPlace,
            onValueChange = {
                pickupPlace = it
                if (it.isNotBlank()) formError = null
            },
            label = { Text("Lugar de recogida") },
            modifier = Modifier.fillMaxWidth(),
            isError = pickupTouched && pickupPlace.isBlank(),
            supportingText = {
                if (pickupTouched && pickupPlace.isBlank()) {
                    Text(stringResource(R.string.field_required))
                }
            }
        )

        OutlinedTextField(
            value = description,
            onValueChange = {
                description = it
                if (it.isNotBlank()) formError = null
            },
            label = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
            isError = descriptionTouched && description.isBlank(),
            supportingText = {
                if (descriptionTouched && description.isBlank()) {
                    Text(stringResource(R.string.field_required))
                }
            }
        )

        //estado del producto
        Text(text = "Estado del producto", style = MaterialTheme.typography.titleSmall)
        
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedState = !expandedState },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when (selectedState) {
                        "NUEVO" -> "Nuevo"
                        "COMO_NUEVO" -> "Como Nuevo"
                        "USADO" -> "Usado"
                        "MAL_ESTADO" -> "Mal Estado"
                        else -> "Seleccionar estado"
                    }
                )
            }
            DropdownMenu(
                expanded = expandedState,
                onDismissRequest = { expandedState = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                DropdownMenuItem(
                    text = { Text("Nuevo") },
                    onClick = {
                        selectedState = "NUEVO"
                        expandedState = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Como Nuevo") },
                    onClick = {
                        selectedState = "COMO_NUEVO"
                        expandedState = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Usado") },
                    onClick = {
                        selectedState = "USADO"
                        expandedState = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Mal Estado") },
                    onClick = {
                        selectedState = "MAL_ESTADO"
                        expandedState = false
                    }
                )
            }
        }

        //imagen
        Text(text = "Imagen", style = MaterialTheme.typography.titleSmall)

        if (selectedImageUri != null) {
            Image(
                painter = rememberAsyncImagePainter(selectedImageUri),
                contentDescription = "Imagen seleccionada",
                modifier = Modifier
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = {
                imagePickerLauncher.launch(arrayOf("image/*"))
            }, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.pick_image_gallery))
            }
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        runCatching {
                            val target = ProductImageCapture.createNewPhoto(context)
                            pendingCameraCapture = target
                            takePictureLauncher.launch(target.uri)
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.pick_image_camera))
            }
        }

        //ubi
        Text(text = "Ubicación", style = MaterialTheme.typography.titleSmall)

        val hasLocation = latitude != null && longitude != null
        val locationText = when {
            hasLocation -> "Ubicación guardada (${String.format(Locale.getDefault(),"%.5f", latitude)}, ${String.format(Locale.getDefault(),"%.5f", longitude)})"
            else -> "Ubicación no establecida"
        }

        Text(locationText, style = MaterialTheme.typography.bodySmall)

        if (locationStatus != null) {
            Text(
                text = locationStatus!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    //comprobar permisos
                    val fineGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val coarseGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!fineGranted && !coarseGranted) {
                        //lanzar diálogo de permisos
                        locationStatus = "Solicitando permisos de ubicación..."
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                        return@launch
                    }

                    //ya hay permisos
                    locationStatus = "Obteniendo ubicación..."
                    //obtenemso la ubi
                    val loc = LocationUtils.getCurrentLocation(context)
                    if (loc != null) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                        locationStatus = "Ubicación guardada correctamente"
                    } else {
                        locationStatus = "No se pudo obtener la ubicación (es null)"
                    }
                }
            }
        ) {
            Text(if (hasLocation) "Actualizar ubicación" else "Usar mi ubicación")
        }

        if (formError != null) {
            Text(
                text = formError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = {
                title = ""
                ownerName = ""
                pickupPlace = ""
                description = ""
                selectedImageUri = null
                latitude = null
                longitude = null
                locationStatus = null
                formError = null
                titleTouched = false
                ownerTouched = false
                pickupTouched = false
                descriptionTouched = false
            }) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    titleTouched = true
                    ownerTouched = true
                    pickupTouched = true
                    descriptionTouched = true

                    val valid = title.isNotBlank() &&
                            ownerName.isNotBlank() &&
                            pickupPlace.isNotBlank() &&
                            description.isNotBlank()

                    if (!valid) {
                        formError = context.getString(R.string.form_error_required_fields)
                        return@Button
                    }

                    formError = null
                    onSubmit(
                        title.trim(),
                        ownerName.trim(),
                        pickupPlace.trim(),
                        description.trim(),
                        selectedImageUri?.toString(),
                        latitude,
                        longitude,
                        selectedState
                    )
                }
            ) {
                Text("Aceptar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTabPlaceholder(
    modifier: Modifier = Modifier,
    products: List<Product>,
    onProductClick: (Long) -> Unit,
    focusProductId: Long? = null
) {
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var focused by remember { mutableStateOf(false) }

    ProductsMap(
        modifier = modifier,
        products = products,
        focusProductId = focusProductId,
        isActive = true,
        onMarkerClick = { p -> selectedProduct = p }
    )

    LaunchedEffect(focusProductId, products) {
        if (focused) return@LaunchedEffect
        val id = focusProductId ?: return@LaunchedEffect
        val p = products.firstOrNull { it.id == id && it.latitude != null && it.longitude != null } ?: return@LaunchedEffect
        selectedProduct = p
        focused = true
    }

    if (selectedProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState
        ) {
            val p = selectedProduct!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(p.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("👤 ${p.ownerName}", style = MaterialTheme.typography.bodyMedium)
                Text("📍 ${p.pickupPlace}", style = MaterialTheme.typography.bodyMedium)
                Text(p.description, style = MaterialTheme.typography.bodyMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val stateText = when (p.state) {
                        ProductState.NUEVO -> "Nuevo"
                        ProductState.COMO_NUEVO -> "Como nuevo"
                        ProductState.USADO -> "Usado"
                        ProductState.MAL_ESTADO -> "Mal estado"
                    }

                    val stateColor = when (p.state) {
                        ProductState.NUEVO -> MaterialTheme.colorScheme.primary
                        ProductState.COMO_NUEVO -> MaterialTheme.colorScheme.tertiary
                        ProductState.USADO -> MaterialTheme.colorScheme.secondary
                        ProductState.MAL_ESTADO -> MaterialTheme.colorScheme.error
                    }
                    AssistChip(
                        onClick = { },
                        label = { Text(stateText) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = stateColor.copy(alpha = 0.15f),
                            labelColor = stateColor
                        )
                    )
                    AssistChip(onClick = { }, label = { Text(if (p.isRequested) "Solicitado" else "Libre") })
                }

                Button(
                    onClick = {
                        val id = p.id
                        selectedProduct = null
                        onProductClick(id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver detalle")
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
