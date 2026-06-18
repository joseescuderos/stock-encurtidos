package com.photosdbrowser.app.ui.photoviewer

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photosdbrowser.app.ui.components.ZoomableImage
import kotlinx.coroutines.delay

private val SLIDESHOW_SPEED_OPTIONS = listOf(3_000L to "3 segundos", 5_000L to "5 segundos", 8_000L to "8 segundos")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    folderUri: Uri,
    startIndex: Int,
    onBackClick: () -> Unit,
    viewModel: PhotoViewerViewModel = viewModel()
) {
    LaunchedEffect(folderUri) { viewModel.load(folderUri, startIndex) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val slideshowInterval by viewModel.slideshowIntervalMs.collectAsStateWithLifecycle()

    ImmersiveSystemBars()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
            uiState.photos.isEmpty() -> Text(
                text = "No hay fotos para mostrar",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
            else -> PhotoPager(
                uiState = uiState,
                slideshowIntervalMs = slideshowInterval,
                onSelectInterval = viewModel::setSlideshowInterval
            )
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Atrás",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ImmersiveSystemBars() {
    val view = LocalView.current
    if (view.isInEditMode) return

    DisposableEffect(view) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoPager(
    uiState: PhotoViewerUiState,
    slideshowIntervalMs: Long,
    onSelectInterval: (Long) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = uiState.startIndex) { uiState.photos.size }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var isSlideshowPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) { isCurrentPageZoomed = false }

    LaunchedEffect(isSlideshowPlaying, isCurrentPageZoomed, slideshowIntervalMs, uiState.photos.size) {
        if (isSlideshowPlaying && !isCurrentPageZoomed) {
            while (true) {
                delay(slideshowIntervalMs)
                val nextPage = (pagerState.currentPage + 1) % uiState.photos.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isCurrentPageZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = uiState.photos[page]
            ZoomableImage(
                model = photo.uri,
                contentDescription = photo.name,
                modifier = Modifier.fillMaxSize(),
                onScaleChanged = { scale ->
                    if (page == pagerState.currentPage) {
                        isCurrentPageZoomed = scale > 1.01f
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeedMenu(currentMs = slideshowIntervalMs, onSelect = onSelectInterval)
            PillIconButton(
                onClick = { isSlideshowPlaying = !isSlideshowPlaying },
                icon = if (isSlideshowPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isSlideshowPlaying) "Pausar presentación" else "Iniciar presentación",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${uiState.photos.size}",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PillIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun SpeedMenu(currentMs: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PillIconButton(
            onClick = { expanded = true },
            icon = Icons.Outlined.Timer,
            contentDescription = "Velocidad de la presentación",
            tint = MaterialTheme.colorScheme.onSurface
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SLIDESHOW_SPEED_OPTIONS.forEach { (ms, label) ->
                DropdownMenuItem(
                    text = { Text(if (ms == currentMs) "● $label" else label) },
                    onClick = {
                        onSelect(ms)
                        expanded = false
                    }
                )
            }
        }
    }
}
