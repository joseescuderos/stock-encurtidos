package com.photosdbrowser.app.ui.photogrid

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.photosdbrowser.app.data.model.PhotoInfo
import com.photosdbrowser.app.data.model.SortOrder
import com.photosdbrowser.app.ui.components.LoadingContent
import com.photosdbrowser.app.ui.components.MessageContent
import com.photosdbrowser.app.ui.components.SortMenu

private const val THUMBNAIL_SIZE_PX = 320

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGridScreen(
    folderUri: Uri,
    folderName: String,
    onPhotoClick: (Int) -> Unit,
    onBackClick: () -> Unit,
    viewModel: PhotoGridViewModel = viewModel()
) {
    LaunchedEffect(folderUri) { viewModel.loadPhotos(folderUri) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sortOrder by rememberSaveable { mutableStateOf(SortOrder.NAME) }

    val photos = (uiState as? PhotoGridUiState.Success)?.photos.orEmpty()
    val sortedPhotos = photos.sortedWith(sortOrder.photoComparator())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(folderName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (photos.isNotEmpty()) {
                            Text(
                                text = "${photos.size} foto${if (photos.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (photos.isNotEmpty()) {
                        SortMenu(current = sortOrder, onSelect = { sortOrder = it })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PhotoGridUiState.Loading -> LoadingContent()
                is PhotoGridUiState.Empty -> MessageContent("No hay fotos en esta carpeta.")
                is PhotoGridUiState.Error -> MessageContent(state.message)
                is PhotoGridUiState.Success -> PhotoGrid(
                    photos = sortedPhotos,
                    onPhotoClick = { photo -> onPhotoClick(photos.indexOf(photo)) }
                )
            }
        }
    }
}

private fun SortOrder.photoComparator(): Comparator<PhotoInfo> = when (this) {
    SortOrder.NAME -> compareBy { it.name.lowercase() }
    SortOrder.DATE_DESC -> compareByDescending { it.lastModified }
}

@Composable
private fun PhotoGrid(
    photos: List<PhotoInfo>,
    onPhotoClick: (PhotoInfo) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { _, photo ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    .size(THUMBNAIL_SIZE_PX)
                    .crossfade(150)
                    .build(),
                contentDescription = photo.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(photo) }
            )
        }
    }
}
