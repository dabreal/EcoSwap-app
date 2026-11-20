package com.example.ecoswap

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.ecoswap.ui.components.EcoDetailHeader
import com.example.ecoswap.ui.components.EcoSectionCard
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(
    productId: Long,
    repository: ProductRepository,
    onShowOnMap: (Long) -> Unit,
    onBack: () -> Unit
) {
    var product by remember { mutableStateOf<Product?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(productId) {
        product = repository.getProductById(productId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        EcoDetailHeader(
            title = product?.title ?: "Detalle",
            onBack = onBack
        )

        val p = product
        if (p == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (p.imageUri != null) {
                    val uri = remember(p.imageUri) { Uri.parse(p.imageUri) }
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Imagen del producto",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .padding(horizontal = 16.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin imagen",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EcoSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = p.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            repository.toggleFavorite(p.id)
                                            product = repository.getProductById(p.id)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (p.isFavorite) {
                                            Icons.Default.Favorite
                                        } else {
                                            Icons.Default.FavoriteBorder
                                        },
                                        contentDescription = if (p.isFavorite) {
                                            "Quitar de favoritos"
                                        } else {
                                            "Añadir a favoritos"
                                        },
                                        tint = if (p.isFavorite) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Ofrecido por ${p.ownerName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            val stateColor = when (p.state) {
                                ProductState.NUEVO -> Color(0xFF4CAF50)
                                ProductState.COMO_NUEVO -> Color(0xFF8BC34A)
                                ProductState.USADO -> Color(0xFFFFC107)
                                ProductState.MAL_ESTADO -> Color(0xFFF44336)
                            }
                            val stateLabel = when (p.state) {
                                ProductState.NUEVO -> "Nuevo"
                                ProductState.COMO_NUEVO -> "Como Nuevo"
                                ProductState.USADO -> "Usado"
                                ProductState.MAL_ESTADO -> "Mal Estado"
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(stateLabel) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = stateColor.copy(alpha = 0.2f),
                                    labelColor = stateColor
                                )
                            )
                        }
                    }

                    EcoSectionCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Lugar de recogida",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = p.pickupPlace,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    EcoSectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = p.description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    val statusText = if (p.isRequested) "Solicitado" else "Disponible"
                    val statusColor = if (p.isRequested) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                    val statusBgColor = if (p.isRequested)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.primaryContainer

                    AssistChip(
                        onClick = { },
                        label = { Text(statusText) },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = statusColor,
                            containerColor = statusBgColor
                        )
                    )

                    if (p.latitude != null && p.longitude != null) {
                        OutlinedButton(
                            onClick = { onShowOnMap(p.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver en mapa")
                        }
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                repository.toggleRequested(p.id)
                                product = repository.getProductById(p.id)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            if (p.isRequested) "Cancelar solicitud" else "Solicitar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
