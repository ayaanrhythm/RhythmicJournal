package ayaan.rhythm.rhythmicjournal

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlbumsScreen() {
    ScreenContainer {
        SimpleHeader(
            leftText = "‹",
            title = "Albums",
            rightText = "+"
        )

        Spacer(modifier = Modifier.height(18.dp))

        TwoColumnAlbumRow(
            leftTitle = "Weekend Notes",
            leftCount = "18 entries",
            rightTitle = "Spring Travel",
            rightCount = "12 entries"
        )

        Spacer(modifier = Modifier.height(14.dp))

        TwoColumnAlbumRow(
            leftTitle = "Campus Days",
            leftCount = "25 entries",
            rightTitle = "Family",
            rightCount = "9 entries"
        )

        Spacer(modifier = Modifier.height(22.dp))

        OutlinedButton(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Text("Create new album")
        }
    }
}