package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PostsScreen() {
    ScreenContainer {
        SimpleHeader(
            leftText = "‹",
            title = "Posts",
            rightText = "⌕"
        )

        Spacer(modifier = Modifier.height(18.dp))

        SearchLikeBar("Search posts, places, or tags")

        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TagChip("#travel")
            TagChip("#coffee")
            TagChip("#daily")
            TagChip("#film")
        }

        Spacer(modifier = Modifier.height(18.dp))

        PublicPostCard(
            user = "@maya.walks",
            meta = "Lisbon • shared publicly",
            likes = "♡ 124",
            comments = "comment 18",
            shares = "share"
        )

        Spacer(modifier = Modifier.height(14.dp))

        PublicPostCard(
            user = "@you",
            meta = "Campus lawn • shared publicly",
            likes = "♡ 32",
            comments = "comment 4",
            shares = "share"
        )
    }
}