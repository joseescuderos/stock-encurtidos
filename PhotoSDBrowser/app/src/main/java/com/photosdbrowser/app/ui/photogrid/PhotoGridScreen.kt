package com.photosdbrowser.app.ui.photogrid

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

private const val THUMBNAIL_SIZE_PX = 320
import com.photosdbrowser.app.data.model.PhotoInfo
import com.photosdbrowser.app.ui.components.LoadingContent
import com.photosdbrowser.app.ui.components.MessageContent

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás")
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
                    photos = state.photos,
                    onPhotoClick = onPhotoClick
                )
            }
        }
    }
}

@Composable
private fun PhotoGrid(photos: List<PhotoInfo>, onPhotoClick: (Int) -> Unit) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(photos, key = { _, photo -> photo.uri.toString() }) { index, photo ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photo.uri)
                    // Decode grid thumbnails small so the JPG is read/decoded at reduced
                    // resolution instead of full size — much faster and lighter on memory.
                    .size(THUMBNAIL_SIZE_PX)
                    .crossfade(false)
                    .build(),
                contentDescription = photo.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onPhotoClick(index) }
            )
        }
    }
}
