package ayaan.rhythm.rhythmicjournal

import android.net.Uri

data class AppRoute(
    val route: String,
    val label: String = "",
    val symbol: String = "",
    private val pathArgs: List<String> = emptyList()
) {
    fun createRoute(vararg values: String): String {
        require(values.size == pathArgs.size) {
            "Expected ${pathArgs.size} args for route $route but got ${values.size}"
        }

        var built = route
        pathArgs.zip(values).forEach { (key, value) ->
            built = built.replace("{$key}", Uri.encode(value))
        }
        return built
    }

    companion object {
        val Login = AppRoute("login", "Login")
        val Home = AppRoute("home", "Home", "⌂")
        val Posts = AppRoute("posts", "Posts", "▥")
        val NewEntry = AppRoute("new_entry", "Journal", "+")
        val Albums = AppRoute("albums", "Albums", "◫")
        val Profile = AppRoute("profile", "Profile", "◯")

        val EditEntry = AppRoute(
            route = "edit_entry/{journalId}",
            label = "Edit Entry",
            pathArgs = listOf("journalId")
        )
        val EntryDetail = AppRoute(
            route = "entry_detail/{journalId}",
            label = "Entry Detail",
            pathArgs = listOf("journalId")
        )
        val AlbumDetail = AppRoute(
            route = "album_detail/{albumId}",
            label = "Album Detail",
            pathArgs = listOf("albumId")
        )

        val EditProfile = AppRoute("edit_profile", "Edit Profile")
        val Favorites = AppRoute("favorites", "Favorites")
        val Settings = AppRoute("settings", "Settings")
        val About = AppRoute("about", "About")
        val ShareExport = AppRoute("share_export", "Share")
    }
}

val bottomNavItems = listOf(
    AppRoute.Home,
    AppRoute.Posts,
    AppRoute.NewEntry,
    AppRoute.Albums,
    AppRoute.Profile
)