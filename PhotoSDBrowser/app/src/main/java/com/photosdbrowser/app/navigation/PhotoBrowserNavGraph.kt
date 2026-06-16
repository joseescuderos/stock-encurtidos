package com.photosdbrowser.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
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

private object Routes {
    const val LINKS = "links"
    const val LINK_CONFIG = "link_config"
    const val FOLDER_LIST = "folder_list"
    const val PHOTO_GRID = "photo_grid/{folderUri}/{folderName}"
    const val PHOTO_VIEWER = "photo_viewer/{folderUri}/{startIndex}"

    fun photoGrid(folderUri: Uri, folderName: String) =
        "photo_grid/${Uri.encode(folderUri.toString())}/${Uri.encode(folderName)}"

    fun photoViewer(folderUri: Uri, startIndex: Int) =
        "photo_viewer/${Uri.encode(folderUri.toString())}/$startIndex"
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
            val folderName = Uri.decode(backStackEntry.arguments?.getString("folderName").orEmpty())

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

private fun androidx.navigation.NavBackStackEntry.requireUriArg(key: String): Uri =
    Uri.parse(Uri.decode(requireNotNull(arguments?.getString(key)) { "Missing nav argument: $key" }))
