package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
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

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    val showDrawer = currentRoute != AppRoute.Login.route && currentRoute != AppRoute.EditProfile.route

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
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                                .clickable {
                                    scope.launch { drawerState.close() }
                                }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        JournalWordmark()
                        Spacer(modifier = Modifier.height(28.dp))

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
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    JournalBottomBar(
                        currentRoute = currentRoute,
                        navController = navController,
                        currentProfileImageUrl = currentProfileImageUrl
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
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
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
                        onOpenEntry = { journalId ->
                            navController.navigate(AppRoute.EntryDetail.createRoute(journalId))
                        },
                        onNewEntry = { navController.navigate(AppRoute.NewEntry.route) }
                    )
                }

                composable(AppRoute.Posts.route) {
                    PostsScreen()
                }

                composable(AppRoute.NewEntry.route) {
                    NewEntryScreen(
                        onCancel = { navController.popBackStack() },
                        onSaveSuccess = { returnToHome() }
                    )
                }

                composable(AppRoute.Albums.route) {
                    AlbumsScreen()
                }

                composable(AppRoute.Profile.route) {
                    ProfileScreen(
                        onOpenDrawer = {
                            scope.launch { drawerState.open() }
                        },
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
                        onDeleted = { returnToHome() }
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
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    if (item == AppRoute.Profile && currentProfileImageUrl.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = currentProfileImageUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Text(
                            text = item.symbol,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onBackground,
                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}