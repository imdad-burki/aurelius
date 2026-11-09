package com.imdadburki.aurelius

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.imdadburki.aurelius.core.design.AureliusColors
import com.imdadburki.aurelius.core.design.AureliusTheme
import com.imdadburki.aurelius.core.di.AppContainer
import com.imdadburki.aurelius.core.network.YouTubeResult
import com.imdadburki.aurelius.feature.home.HomeScreen
import com.imdadburki.aurelius.feature.library.AlbumDetailScreen
import com.imdadburki.aurelius.feature.library.ArtistDetailScreen
import com.imdadburki.aurelius.feature.library.FolderDetailScreen
import com.imdadburki.aurelius.feature.library.LibraryScreen
import com.imdadburki.aurelius.feature.library.LibraryViewModel
import com.imdadburki.aurelius.feature.library.PlaylistDetailScreen
import com.imdadburki.aurelius.feature.onboarding.OnboardingScreen
import com.imdadburki.aurelius.feature.onboarding.SplashScreen
import com.imdadburki.aurelius.feature.player.LyricsScreen
import com.imdadburki.aurelius.feature.player.MiniPlayer
import com.imdadburki.aurelius.feature.player.PlayerScreen
import com.imdadburki.aurelius.feature.player.PlayerViewModel
import com.imdadburki.aurelius.feature.search.SearchScreen
import com.imdadburki.aurelius.feature.search.SearchViewModel
import com.imdadburki.aurelius.feature.settings.LanguageScreen
import com.imdadburki.aurelius.feature.settings.SettingsScreen
import com.imdadburki.aurelius.feature.youtube.YouTubePlayerScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class AureliusViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        LibraryViewModel::class.java -> LibraryViewModel(container)
        SearchViewModel::class.java -> SearchViewModel(container)
        PlayerViewModel::class.java -> PlayerViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel $modelClass")
    } as T
}

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private var currentYouTube: YouTubeResult? = null

    private val requestMusicPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* scan runs on next Library/Home visit; result shown via scan error state */ }

    override fun attachBaseContext(newBase: Context) {
        // Apply the saved language before any UI is inflated.
        val code = runBlocking {
            try {
                val app = newBase.applicationContext as? AureliusApp
                app?.container?.settings?.language?.first() ?: "en"
            } catch (e: Exception) {
                "en"
            }
        }
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as AureliusApp).container
        // Start the foreground playback service so audio survives the activity.
        startService(android.content.Intent(this, com.imdadburki.aurelius.core.playback.PlaybackService::class.java))
        setContent { AureliusRoot() }
    }

    private fun hasMusicPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestMusicPermissionIfNeeded() {
        val perms = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestMusicPermission.launch(perms)
    }

    @Composable
    private fun AureliusRoot() {
        val factory = remember { AureliusViewModelFactory(container) }
        val libraryVm: LibraryViewModel = viewModel(factory = factory)
        val searchVm: SearchViewModel = viewModel(factory = factory)
        val playerVm: PlayerViewModel = viewModel(factory = factory)
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val onboarded by container.settings.onboarded.collectAsState(false)
        val favoriteIds by libraryVm.favoriteIds.collectAsState()
        var splashDone by remember { mutableStateOf(false) }
        var trending by remember { mutableStateOf<List<YouTubeResult>>(emptyList()) }
        var trendingError by remember { mutableStateOf<String?>(null) }
        val scope = remember { CoroutineScope(Dispatchers.Main) }

        fun loadTrending() {
            scope.launch {
                try {
                    val (results, _) = container.youTubeApi.search("trending music", order = "viewCount")
                    trending = results.take(10)
                    trendingError = null
                } catch (e: Exception) {
                    trending = emptyList()
                    trendingError = "YouTube is unavailable right now (${e.message}). Local music still works."
                }
            }
        }

        LaunchedEffect(Unit) {
            if (hasMusicPermission()) libraryVm.scan(this@MainActivity)
        }

        val tabs = listOf(
            BottomTab(Routes.HOME, "Home", Icons.Default.Home),
            BottomTab(Routes.SEARCH, "Search", Icons.Default.Search),
            BottomTab(Routes.LIBRARY, "Library", Icons.Default.LibraryMusic),
            BottomTab(Routes.SETTINGS, "Settings", Icons.Default.Settings)
        )
        val showBottomBar = currentRoute in tabs.map { it.route }

        AureliusTheme {
            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        Column {
                            MiniPlayer(vm = playerVm) {
                                navController.navigate(Routes.PLAYER)
                            }
                            NavigationBar(containerColor = AureliusColors.Background) {
                                tabs.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentRoute == tab.route,
                                        onClick = {
                                            navController.navigate(tab.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(tab.icon, tab.label) },
                                        label = { Text(tab.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = AureliusColors.GoldPrimary,
                                            selectedTextColor = AureliusColors.GoldPrimary,
                                            unselectedIconColor = AureliusColors.TextTertiary,
                                            unselectedTextColor = AureliusColors.TextTertiary,
                                            indicatorColor = AureliusColors.Elevated
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(Modifier.fillMaxSize().padding(innerPadding)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.SPLASH
                    ) {
                        composable(Routes.SPLASH) {
                            SplashScreen {
                                splashDone = true
                                navController.navigate(if (onboarded) Routes.HOME else Routes.ONBOARDING) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        }
                        composable(Routes.ONBOARDING) {
                            OnboardingScreen {
                                scope.launch { container.settings.setOnboarded(true) }
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            }
                        }
                        composable(Routes.HOME) {
                            LaunchedEffect(Unit) {
                                if (trending.isEmpty() && trendingError == null) loadTrending()
                                if (hasMusicPermission() && libraryVm.tracks.value.isEmpty()) {
                                    libraryVm.scan(this@MainActivity)
                                }
                            }
                            HomeScreen(
                                libraryVm = libraryVm,
                                trending = trending,
                                trendingError = trendingError,
                                onTrendingRetry = ::loadTrending,
                                onPlayTracks = { tracks, idx ->
                                    playerVm.playTracks(tracks, idx)
                                    libraryVm.recordHistory(tracks[idx])
                                },
                                onPlayYouTube = { r ->
                                    currentYouTube = r
                                    navController.navigate(Routes.YOUTUBE_PLAYER)
                                },
                                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                                onOpenPlaylist = { p -> navController.navigate(Routes.playlist(p.id)) },
                                onOpenAlbum = { name -> navController.navigate(Routes.album(name)) }
                            )
                        }
                        composable(Routes.SEARCH) {
                            val localTracks by libraryVm.tracks.collectAsState()
                            SearchScreen(
                                vm = searchVm,
                                localTracks = localTracks,
                                onPlayLocal = { t ->
                                    playerVm.playTracks(listOf(t), 0)
                                    navController.navigate(Routes.PLAYER)
                                },
                                onPlayYouTube = { r ->
                                    currentYouTube = r
                                    navController.navigate(Routes.YOUTUBE_PLAYER)
                                }
                            )
                        }
                        composable(Routes.LIBRARY) {
                            LibraryScreen(
                                vm = libraryVm,
                                onPlayTracks = { tracks, idx ->
                                    playerVm.playTracks(tracks, idx)
                                    libraryVm.recordHistory(tracks[idx])
                                },
                                onOpenAlbum = { name -> navController.navigate(Routes.album(name)) },
                                onOpenArtist = { name -> navController.navigate(Routes.artist(name)) },
                                onOpenFolder = { name -> navController.navigate(Routes.folder(name)) },
                                onOpenPlaylist = { p -> navController.navigate(Routes.playlist(p.id)) },
                                onSearchYouTube = { navController.navigate(Routes.SEARCH) },
                                onScan = {
                                    if (hasMusicPermission()) libraryVm.scan(this@MainActivity)
                                    else requestMusicPermissionIfNeeded()
                                }
                            )
                        }
                        composable(
                            Routes.ALBUM,
                            arguments = listOf(navArgument("albumName") { type = NavType.StringType })
                        ) { entry ->
                            val name = entry.arguments?.getString("albumName") ?: ""
                            AlbumDetailScreen(
                                albumName = name,
                                vm = libraryVm,
                                onPlayTracks = { tracks, idx ->
                                    playerVm.playTracks(tracks, idx)
                                    navController.navigate(Routes.PLAYER)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            Routes.ARTIST,
                            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                        ) { entry ->
                            val name = entry.arguments?.getString("artistName") ?: ""
                            ArtistDetailScreen(
                                artistName = name,
                                vm = libraryVm,
                                onPlayTracks = { tracks, idx ->
                                    playerVm.playTracks(tracks, idx)
                                    navController.navigate(Routes.PLAYER)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            Routes.FOLDER,
                            arguments = listOf(navArgument("folderName") { type = NavType.StringType })
                        ) { entry ->
                            val name = entry.arguments?.getString("folderName") ?: ""
                            FolderDetailScreen(
                                folderName = name,
                                vm = libraryVm,
                                onPlayTracks = { tracks, idx ->
                                    playerVm.playTracks(tracks, idx)
                                    navController.navigate(Routes.PLAYER)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            Routes.PLAYLIST,
                            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                        ) { entry ->
                            val id = entry.arguments?.getLong("playlistId") ?: 0L
                            PlaylistDetailScreen(
                                playlistId = id,
                                vm = libraryVm,
                                onPlayTracks = { tracks, idx ->
                                    playerVm.playTracks(tracks, idx)
                                    navController.navigate(Routes.PLAYER)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(Routes.PLAYER) {
                            PlayerScreen(
                                vm = playerVm,
                                favoriteIds = favoriteIds,
                                onToggleFavorite = { t -> libraryVm.toggleFavorite(t) },
                                onBack = { navController.popBackStack() },
                                onOpenLyrics = { navController.navigate(Routes.LYRICS) },
                                onOpenQueue = { /* queue opens as a sheet inside PlayerScreen */ }
                            )
                        }
                        composable(Routes.LYRICS) {
                            LyricsScreen(vm = playerVm, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.YOUTUBE_PLAYER) {
                            val result = currentYouTube
                            if (result != null) {
                                YouTubePlayerScreen(
                                    result = result,
                                    onBack = { navController.popBackStack() },
                                    onOpenInYouTubeApp = {
                                        try {
                                            startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://www.youtube.com/watch?v=${result.videoId}")
                                                )
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(this@MainActivity, "YouTube app is not installed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settings = container.settings,
                                onOpenLanguage = { navController.navigate(Routes.LANGUAGE) }
                            )
                        }
                        composable(Routes.LANGUAGE) {
                            LanguageScreen(
                                settings = container.settings,
                                onBack = { navController.popBackStack() },
                                onLanguageChanged = { recreate() }
                            )
                        }
                    }
                }
            }
        }
    }
}
