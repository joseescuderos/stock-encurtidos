package com.photosdbrowser.app.ui.photoviewer

import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.photosdbrowser.app.ui.components.ZoomableImage
import kotlinx.coroutines.delay

private const val SLIDESHOW_INTERVAL_MS = 4000L

/**
 * Chrome-free full-screen viewer on the brand's light background, meant for showing
 * reportages to clients on a tablet. Swipe left/right to move between photos, pinch to
 * zoom and pan within a photo — pager swipes are disabled while the current photo is
 * zoomed in so the two gestures don't fight each other. A play/pause button starts an
 * optional slideshow that advances photos automatically, so the photographer can talk
 * through the work hands-free; system bars are hidden for a distraction-free, immersive
 * presentation.
 */
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
            else -> PhotoPager(uiState = uiState)
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

/** Hides the system status/navigation bars for a clean, distraction-free screen, restoring them on exit. */
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
private fun PhotoPager(uiState: PhotoViewerUiState) {
    val pagerState = rememberPagerState(initialPage = uiState.startIndex) { uiState.photos.size }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var isSlideshowPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) { isCurrentPageZoomed = false }

    LaunchedEffect(isSlideshowPlaying, isCurrentPageZoomed, uiState.photos.size) {
        if (isSlideshowPlaying && !isCurrentPageZoomed) {
            while (true) {
                delay(SLIDESHOW_INTERVAL_MS)
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

        IconButton(
            onClick = { isSlideshowPlaying = !isSlideshowPlaying },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
        ) {
            Icon(
                imageVector = if (isSlideshowPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isSlideshowPlaying) "Pausar presentación automática" else "Iniciar presentación automática",
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
