package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Suppress("UNUSED_PARAMETER")
@Composable
fun RhythmicJournalApp(
    store: LocalAppStore,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val profileRepository = remember { ProfileRepository() }

    val startDestination =
        if (firebaseAuth.currentUser != null) AppRoute.Home.route else AppRoute.Login.route

    var currentProfileImageUrl by remember { mutableStateOf("") }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val selectedBottomRoute = when (currentRoute) {
        AppRoute.AlbumDetail.route -> AppRoute.Albums.route
        else -> currentRoute
    }

    val showBottomBar = selectedBottomRoute in bottomNavItems.map { it.route }
    val showDrawer =
        currentRoute != AppRoute.Login.route && currentRoute != AppRoute.EditProfile.route

    LaunchedEffect(currentRoute, firebaseAuth.currentUser?.uid) {
        if (drawerState.isOpen) {
            drawerState.close()
        }

        if (firebaseAuth.currentUser == null) {
            currentProfileImageUrl = ""
        } else if (currentRoute != AppRoute.Login.route) {
            runCatching {
                currentProfileImageUrl =
                    profileRepository.getOrCreateCurrentUserProfile().profileImageUrl
            }
        }
    }

    fun navigateTo(route: String) {
        scope.launch {
            drawerState.close()
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                if (route in bottomNavItems.map { it.route }) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
            }
        }
    }

    fun returnToHome() {
        val popped = navController.popBackStack(AppRoute.Home.route, false)
        if (!popped) {
            navController.navigate(AppRoute.Home.route) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer,
        drawerContent = {
            if (showDrawer) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(0.82f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
                        Text(
                            text = "✕",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clickable { scope.launch { drawerState.close() } }
                        )

                        Spacer(modifier = Modifier.padding(top = 20.dp))
                        JournalWordmark()
                        Spacer(modifier = Modifier.padding(top = 28.dp))

                        DrawerMenuItem(text = "Profile") { navigateTo(AppRoute.Profile.route) }
                        DrawerMenuItem(text = "Favorites") { navigateTo(AppRoute.Favorites.route) }
                        DrawerMenuItem(text = "Settings") { navigateTo(AppRoute.Settings.route) }
                        DrawerMenuItem(text = "About") { navigateTo(AppRoute.About.route) }
                        DrawerMenuItem(text = "Sign Out") {
                            scope.launch {
                                runCatching { firebaseAuth.signOut() }
                                runCatching {
                                    CredentialManager.create(context)
                                        .clearCredentialState(ClearCredentialStateRequest())
                                }

                                currentProfileImageUrl = ""

                                drawerState.close()
                                navController.navigate(AppRoute.Login.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    JournalBottomBar(
                        currentRoute = selectedBottomRoute,
                        navController = navController,
                        currentProfileImageUrl = currentProfileImageUrl
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        top = innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
                        bottom = 0.dp
                    )
            ) {
                composable(AppRoute.Login.route) {
                    LoginScreen(
                        onLogin = {
                            if (firebaseAuth.currentUser != null) {
                                scope.launch {
                                    runCatching {
                                        currentProfileImageUrl =
                                            profileRepository.getOrCreateCurrentUserProfile().profileImageUrl
                                    }
                                    navController.navigate(AppRoute.Home.route) {
                                        popUpTo(AppRoute.Login.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        },
                        onCreateAccount = { _ ->
                            if (firebaseAuth.currentUser != null) {
                                scope.launch {
                                    runCatching {
                                        currentProfileImageUrl =
                                            profileRepository.getOrCreateCurrentUserProfile().profileImageUrl
                                    }
                                    navController.navigate(AppRoute.Home.route) {
                                        popUpTo(AppRoute.Login.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    )
                }

                composable(AppRoute.Home.route) {
                    HomeScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenEntry = { journalId ->
                            navController.navigate(AppRoute.EntryDetail.createRoute(journalId))
                        },
                        onNewEntry = { navController.navigate(AppRoute.NewEntry.route) }
                    )
                }

                composable(AppRoute.Posts.route) {
                    PostsScreen(
                        onBack = {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                navController.navigate(AppRoute.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
                                }
                            }
                        },
                        onOpenEntry = { journalId ->
                            navController.navigate(AppRoute.EntryDetail.createRoute(journalId))
                        },
                        onEditPost = { journalId ->
                            navController.navigate(AppRoute.EditEntry.createRoute(journalId))
                        },
                        onOpenAlbum = { albumId ->
                            navController.navigate(AppRoute.AlbumDetail.createRoute(albumId))
                        }
                    )
                }

                composable(AppRoute.NewEntry.route) {
                    NewEntryScreen(
                        onCancel = { navController.popBackStack() },
                        onSaveSuccess = { returnToHome() }
                    )
                }

                composable(AppRoute.Albums.route) {
                    AlbumsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { albumId ->
                            navController.navigate(AppRoute.AlbumDetail.createRoute(albumId))
                        }
                    )
                }

                composable(AppRoute.Profile.route) {
                    ProfileScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onOpenEditProfile = {
                            navController.navigate(AppRoute.EditProfile.route)
                        },
                        onOpenEntry = { journalId ->
                            navController.navigate(AppRoute.EntryDetail.createRoute(journalId))
                        }
                    )
                }

                composable(AppRoute.EditProfile.route) {
                    EditProfileScreen(
                        onClose = { navController.popBackStack() }
                    )
                }

                composable(AppRoute.Favorites.route) {
                    FavoritesScreen(
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { journalId ->
                            navController.navigate(AppRoute.EntryDetail.createRoute(journalId))
                        }
                    )
                }

                composable(AppRoute.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(AppRoute.Settings.route) {
                    SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = AppRoute.EntryDetail.route,
                    arguments = listOf(
                        navArgument("journalId") { type = NavType.StringType }
                    )
                ) { entry ->
                    val journalId = entry.arguments?.getString("journalId").orEmpty()

                    EntryDetailScreen(
                        journalId = journalId,
                        onBack = { navController.popBackStack() },
                        onEdit = { selectedJournalId ->
                            navController.navigate(AppRoute.EditEntry.createRoute(selectedJournalId))
                        },
                        onShare = { navController.navigate(AppRoute.ShareExport.route) },
                        onDeleted = { returnToHome() },
                        onOpenAlbum = { albumId ->
                            navController.navigate(AppRoute.AlbumDetail.createRoute(albumId))
                        }
                    )
                }

                composable(
                    route = AppRoute.EditEntry.route,
                    arguments = listOf(
                        navArgument("journalId") { type = NavType.StringType }
                    )
                ) { entry ->
                    val journalId = entry.arguments?.getString("journalId").orEmpty()

                    NewEntryScreen(
                        journalId = journalId,
                        onCancel = { navController.popBackStack() },
                        onSaveSuccess = { returnToHome() }
                    )
                }

                composable(
                    route = AppRoute.AlbumDetail.route,
                    arguments = listOf(
                        navArgument("albumId") { type = NavType.StringType }
                    )
                ) { entry ->
                    val albumId = entry.arguments?.getString("albumId").orEmpty()

                    AlbumDetailScreen(
                        albumId = albumId,
                        onBack = { navController.popBackStack() },
                        onOpenEntry = { journalId ->
                            navController.navigate(AppRoute.EntryDetail.createRoute(journalId))
                        }
                    )
                }

                composable(AppRoute.ShareExport.route) {
                    ShareExportScreen(
                        onBack = { navController.popBackStack() },
                        onDone = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun JournalBottomBar(
    currentRoute: String?,
    navController: NavHostController,
    currentProfileImageUrl: String
) {
    val barColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = barColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route

                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .clickable {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = if (selected) {
                                Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            } else {
                                Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                item == AppRoute.Profile && currentProfileImageUrl.isNotBlank() -> {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = currentProfileImageUrl,
                                            contentDescription = "Profile",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                item == AppRoute.Albums -> {
                                    Icon(
                                        imageVector = Icons.Outlined.Collections,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp),
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                else -> {
                                    Text(
                                        text = item.symbol,
                                        fontSize = 22.sp,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.padding(top = 2.dp))

                        Text(
                            text = item.label,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}