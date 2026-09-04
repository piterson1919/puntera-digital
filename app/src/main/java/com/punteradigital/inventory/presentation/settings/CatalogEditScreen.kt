package com.punteradigital.inventory.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.punteradigital.inventory.R
import com.punteradigital.inventory.data.local.entity.CatalogModelEntity
import com.punteradigital.inventory.presentation.components.ButtonType
import com.punteradigital.inventory.presentation.components.KineticButton
import com.punteradigital.inventory.presentation.components.KineticCard
import com.punteradigital.inventory.presentation.components.KineticTextField
import com.punteradigital.inventory.presentation.viewmodel.InventoryViewModel
import com.punteradigital.inventory.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogEditScreen(
    viewModel: InventoryViewModel,
    itemId: String?,
    onBack: () -> Unit
) {
    val models by viewModel.catalogModels.collectAsState(initial = emptyList())
    val item = remember(itemId, models) { models.find { it.id == itemId } }
    val isNew = item == null

    var sku by remember(item?.id) { mutableStateOf(item?.code ?: "") }
    var nombreComercial by remember(item?.id) { mutableStateOf(item?.name ?: "") }
    var tallaMin by remember(item?.id) { mutableStateOf((item?.sizeMin ?: 36).toString()) }
    var tallaMax by remember(item?.id) { mutableStateOf((item?.sizeMax ?: 46).toString()) }
    var paresPorCaja by remember(item?.id) { mutableStateOf((item?.pairsPerBox ?: 8).toString()) }
    var isActive by remember(item?.id) { mutableStateOf(item?.isActive ?: true) }

    var imageUri by remember(item?.id) { mutableStateOf<Uri?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
    }

    LaunchedEffect(item?.id) {
        if (item != null) {
            imageUri = item.imageUri?.let { Uri.parse(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (isNew) "Nuevo Modelo" else "Editar Modelo", fontWeight = FontWeight.Bold)
                        if (!isNew) {
                            Text(
                                sku,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showImagePicker = true },
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto del producto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.boot_black),
                            contentDescription = sku.ifBlank { "Modelo" },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (imageUri != null) "Foto Actualizada ✓" else "📸 Tocar para cambiar foto",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            KineticCard(padding = 20.dp) {
                Text(
                    "Información del Modelo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                KineticTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = "Código SKU",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                KineticTextField(
                    value = nombreComercial,
                    onValueChange = { nombreComercial = it },
                    label = "Nombre Comercial",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    KineticTextField(
                        value = tallaMin,
                        onValueChange = { tallaMin = it },
                        label = "Talla Mín",
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    KineticTextField(
                        value = tallaMax,
                        onValueChange = { tallaMax = it },
                        label = "Talla Máx",
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                KineticTextField(
                    value = paresPorCaja,
                    onValueChange = { paresPorCaja = it },
                    label = "Pares por Caja Master",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            KineticCard(padding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Modelo Activo", fontWeight = FontWeight.Bold)
                        Text("Visible en entrada y despacho", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = KineticPrimary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            KineticButton(
                text = "GUARDAR CAMBIOS",
                onClick = {
                    val newModel = CatalogModelEntity(
                        id = item?.id ?: UUID.randomUUID().toString(),
                        code = sku.trim(),
                        name = nombreComercial.trim(),
                        sizeMin = tallaMin.toIntOrNull() ?: 36,
                        sizeMax = tallaMax.toIntOrNull() ?: 46,
                        pairsPerBox = paresPorCaja.toIntOrNull() ?: 8,
                        isActive = isActive,
                        imageUri = imageUri?.toString(),
                        updatedAt = System.currentTimeMillis(),
                        createdAt = item?.createdAt ?: System.currentTimeMillis()
                    )
                    if (newModel.code.isNotBlank() && newModel.name.isNotBlank()) {
                        viewModel.saveCatalogModel(newModel)
                        onBack()
                    }
                },
                type = ButtonType.PRIMARY
            )

            if (!isNew) {
                Spacer(modifier = Modifier.height(8.dp))
                KineticButton(
                    text = "ELIMINAR MODELO",
                    onClick = {
                        item?.let { viewModel.deleteCatalogModel(it.id) }
                        onBack()
                    },
                    type = ButtonType.DANGER
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showImagePicker) {
        LaunchedEffect(showImagePicker) {
            galleryLauncher.launch("image/*")
            showImagePicker = false
        }
    }
}
