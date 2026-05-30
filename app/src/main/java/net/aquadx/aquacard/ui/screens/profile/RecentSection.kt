package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.RecentEntry
import net.aquadx.aquacard.data.ScoreFormat
import net.aquadx.aquacard.ui.theme.AquaCardTheme

@Composable
fun RecentRow(entry: RecentEntry, meta: Map<Int, MusicMeta>) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    ScoreFormat.songName(meta, entry.musicId),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(levelLabel(entry))
                        entry.playDate?.let { append(" · "); append(shortDate(it)) }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                valueLabel(entry),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun levelLabel(entry: RecentEntry): String = when (entry) {
    is RecentEntry.Mai -> ScoreFormat.levelName("mai2", entry.level)
    is RecentEntry.Chu -> ScoreFormat.levelName("chu3", entry.level)
}

private fun valueLabel(entry: RecentEntry): String = when (entry) {
    is RecentEntry.Mai -> ScoreFormat.achievementPercent(entry.achievement)
    is RecentEntry.Chu -> ScoreFormat.chuScore(entry.score)
}

/** Берём дату без времени: "2025-11-25T02:42:14" -> "2025-11-25". */
private fun shortDate(raw: String): String =
    raw.substringBefore('T').substringBefore(' ').ifBlank { raw }

@Preview
@Composable
private fun RecentRowPreview() {
    AquaCardTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
            RecentRow(
                RecentEntry.Mai(834, 3, "2026-05-29T20:11:00", achievement = 1004210, rank = 13, comboStatus = 1, isClear = true),
                mapOf(834 to MusicMeta(name = "Oshama Scramble!"))
            )
            RecentRow(
                RecentEntry.Chu(2252, 2, "2025-11-25T02:42:14", score = 806267, rank = 4),
                mapOf(2252 to MusicMeta(name = "Trichromatic"))
            )
        }
    }
}
