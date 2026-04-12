package ayaan.rhythm.rhythmicjournal

sealed class AppRoute(val route: String, val label: String = "", val symbol: String = "") {
    data object Login : AppRoute("login")
    data object Home : AppRoute("home", "Home", "⌂")
    data object Posts : AppRoute("posts", "Posts", "◫")
    data object NewEntry : AppRoute("new_entry", "Journal", "+")
    data object Albums : AppRoute("albums", "Albums", "▣")
    data object Profile : AppRoute("profile", "Profile", "☺")

    data object Favorites : AppRoute("favorites")
    data object About : AppRoute("about")
    data object Settings : AppRoute("settings")
    data object EditProfile : AppRoute("edit_profile")

    data object EntryDetail : AppRoute("entry_detail/{journalId}") {
        fun createRoute(journalId: String): String = "entry_detail/$journalId"
    }

    data object EditEntry : AppRoute("edit_entry/{journalId}") {
        fun createRoute(journalId: String): String = "edit_entry/$journalId"
    }

    data object ShareExport : AppRoute("share_export")
}

val bottomNavItems = listOf(
    AppRoute.Home,
    AppRoute.Posts,
    AppRoute.NewEntry,
    AppRoute.Albums,
    AppRoute.Profile
)