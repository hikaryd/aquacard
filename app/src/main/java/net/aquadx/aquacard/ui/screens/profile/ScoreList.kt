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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aquadx.aquacard.data.BestEntry
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.ProfileScore
import net.aquadx.aquacard.data.ScoreFormat
import net.aquadx.aquacard.ui.theme.AquaCardTheme

@Composable
fun SectionHeader(title: String, count: Int? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BestRow(index: Int, entry: BestEntry, game: String, meta: Map<Int, MusicMeta>) {
    ScoreRowShell(
        rankIndex = index + 1,
        title = ScoreFormat.songName(meta, entry.musicId),
        level = entry.level,
        levelText = ScoreFormat.levelName(game, entry.level),
        value = ScoreFormat.bestValueLabel(game, entry.value),
        badges = emptyList()
    )
}

@Composable
fun ScoreRow(score: ProfileScore, meta: Map<Int, MusicMeta>) {
    ScoreRowShell(
        rankIndex = null,
        title = ScoreFormat.songName(meta, score.musicId),
        level = score.level,
        levelText = levelLabel(score),
        value = valueLabel(score),
        badges = badges(score)
    )
}

@Composable
private fun ScoreRowShell(
    rankIndex: Int?,
    title: String,
    level: Int,
    levelText: String,
    value: String,
    badges: List<String>
) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rankIndex != null) {
                Text(
                    rankIndex.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        levelText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = difficultyColor(level)
                    )
                    badges.forEach { badge ->
                        Spacer(Modifier.width(6.dp))
                        Badge(badge)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

// --- per-game разбор через sealed when (без строковых проверок game в UI) ---

private fun levelLabel(score: ProfileScore): String = when (score) {
    is ProfileScore.Mai -> ScoreFormat.levelName("mai2", score.level)
    is ProfileScore.Chu -> ScoreFormat.levelName("chu3", score.level)
}

private fun valueLabel(score: ProfileScore): String = when (score) {
    is ProfileScore.Mai -> ScoreFormat.achievementPercent(score.achievement)
    is ProfileScore.Chu -> ScoreFormat.chuScore(score.score)
}

private fun badges(score: ProfileScore): List<String> = when (score) {
    is ProfileScore.Mai -> listOfNotNull(
        ScoreFormat.comboLabel(score.comboStatus),
        ScoreFormat.syncLabel(score.syncStatus)
    )
    is ProfileScore.Chu -> listOfNotNull(
        when {
            score.isAllJustice == true -> "AJ"
            score.isFullCombo == true -> "FC"
            else -> null
        }
    )
}

private fun difficultyColor(level: Int): Color = when (level) {
    0 -> Color(0xFF22C55E)
    1 -> Color(0xFFF59E0B)
    2 -> Color(0xFFEF4444)
    3 -> Color(0xFFA855F7)
    4 -> Color(0xFFE879F9)
    else -> Color(0xFF9BA1A8)
}

@Preview
@Composable
private fun ScoreRowPreview() {
    AquaCardTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(12.dp)) {
            BestRow(0, BestEntry(834, 4, 1008790), "mai2", mapOf(834 to MusicMeta(name = "Oshama Scramble!")))
            ScoreRow(
                ProfileScore.Mai(22, 3, achievement = 1005000, deluxscore = 2625, comboStatus = 3, syncStatus = 2, scoreRank = 13),
                mapOf(22 to MusicMeta(name = "PANDORA PARADOXXX"))
            )
            ScoreRow(
                ProfileScore.Chu(2252, 2, score = 1009500, scoreRank = 7, isFullCombo = true, isAllJustice = false),
                mapOf(2252 to MusicMeta(name = "Trichromatic"))
            )
        }
    }
}
