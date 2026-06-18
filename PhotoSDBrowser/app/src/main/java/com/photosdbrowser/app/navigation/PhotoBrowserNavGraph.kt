package com.photosdbrowser.app.navigation

import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photosdbrowser.app.ui.linkconfig.LinkConfigScreen
import com.photosdbrowser.app.ui.links.LinksScreen
import com.photosdbrowser.app.ui.folderlist.FolderListScreen
import com.photosdbrowser.app.ui.photogrid.PhotoGridScreen
import com.photosdbrowser.app.ui.photoviewer.PhotoViewerScreen

private const val BASE64_FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

/**
 * SAF document URIs contain ':' and '/' inside their document id (e.g. "1234-5678:BODAS/Sesion1").
 * Percent-encoding them and letting Navigation decode once leads to a second decode that corrupts
 * the URI. Encoding the whole value as URL-safe Base64 sidesteps all percent-encoding ambiguity.
 */
private fun encodeArg(value: String): String =
    Base64.encodeToString(value.toByteArray(Charsets.UTF_8), BASE64_FLAGS)

private fun decodeArg(value: String): String =
    String(Base64.decode(value, BASE64_FLAGS), Charsets.UTF_8)

private object Routes {
    const val LINKS = "links"
    const val LINK_CONFIG = "link_config"
    const val FOLDER_LIST = "folder_list"
    const val PHOTO_GRID = "photo_grid/{folderUri}/{folderName}"
    const val PHOTO_VIEWER = "photo_viewer/{folderUri}/{startIndex}"

    fun photoGrid(folderUri: Uri, folderName: String) =
        "photo_grid/${encodeArg(folderUri.toString())}/${encodeArg(folderName)}"

    fun photoViewer(folderUri: Uri, startIndex: Int) =
        "photo_viewer/${encodeArg(folderUri.toString())}/$startIndex"
}

@Composable
fun PhotoBrowserNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LINKS) {
        composable(Routes.LINKS) {
            LinksScreen(
                onSettingsClick = { navController.navigate(Routes.LINK_CONFIG) },
                onPhotosClick = { navController.navigate(Routes.FOLDER_LIST) }
            )
        }

        composable(Routes.LINK_CONFIG) {
            LinkConfigScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Routes.FOLDER_LIST) {
            FolderListScreen(
                onFolderClick = { folder ->
                    navController.navigate(Routes.photoGrid(folder.uri, folder.name))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PHOTO_GRID,
            arguments = listOf(
                navArgument("folderUri") { type = NavType.StringType },
                navArgument("folderName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderUri = backStackEntry.requireUriArg("folderUri")
            val folderName = decodeArg(backStackEntry.arguments?.getString("folderName").orEmpty())

            PhotoGridScreen(
                folderUri = folderUri,
                folderName = folderName,
                onPhotoClick = { index ->
                    navController.navigate(Routes.photoViewer(folderUri, index))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PHOTO_VIEWER,
            arguments = listOf(
                navArgument("folderUri") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val folderUri = backStackEntry.requireUriArg("folderUri")
            val startIndex = backStackEntry.arguments?.getInt("startIndex") ?: 0

            PhotoViewerScreen(
                folderUri = folderUri,
                startIndex = startIndex,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun NavBackStackEntry.requireUriArg(key: String): Uri =
    Uri.parse(decodeArg(requireNotNull(arguments?.getString(key)) { "Missing nav argument: $key" }))
