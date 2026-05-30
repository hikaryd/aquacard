@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import net.aquadx.aquacard.data.AquaUser
import net.aquadx.aquacard.data.GameSummary
import net.aquadx.aquacard.data.ScoreFormat
import net.aquadx.aquacard.data.UserDetailDto
import net.aquadx.aquacard.ui.theme.AquaCardTheme

@Composable
fun ProfileHeader(summary: GameSummary, detail: UserDetailDto?, game: String) {
    val displayName = summary.aquaUser?.displayName?.takeIf { it.isNotBlank() }
        ?: summary.aquaUser?.username
        ?: summary.name
        ?: "Игрок"
    val username = summary.aquaUser?.username
    val rating = detail?.playerRating ?: summary.rating

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(displayName)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (username != null) {
                        Text(
                            "@$username",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    summary.aquaUser?.country?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        ScoreFormat.formatRating(game, rating),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Рейтинг",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            summary.aquaUser?.profileBio?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Игр", summary.plays?.toString() ?: "—")
                StatCell("Макс", summary.ratingHighest?.let { ScoreFormat.formatRating(game, it) } ?: "—")
                StatCell("Ранг", summary.serverRank?.let { "#$it" } ?: "—")
                StatCell("Точность", summary.accuracy?.let { String.format(Locale.US, "%.2f%%", it) } ?: "—")
            }

            if (summary.maxCombo != null || summary.fullCombo != null || summary.allPerfect != null) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCell("Max Combo", summary.maxCombo?.toString() ?: "—")
                    StatCell("Full Combo", summary.fullCombo?.toString() ?: "—")
                    StatCell("All Perfect", summary.allPerfect?.toString() ?: "—")
                }
            }
        }
    }
}

@Composable
private fun AvatarCircle(displayName: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials(displayName),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun RowScope.StatCell(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(' ', '　').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Preview
@Composable
private fun ProfileHeaderPreview() {
    AquaCardTheme {
        ProfileHeader(
            summary = GameSummary(
                name = "Ivy",
                aquaUser = AquaUser(username = "Sigma", displayName = "DEMONDX", country = "US", profileBio = "main maimai"),
                serverRank = 1, rating = 16666, ratingHighest = 16800, accuracy = 94.79,
                plays = 2795, maxCombo = 1282, fullCombo = 140, allPerfect = 111
            ),
            detail = UserDetailDto(playerRating = 16666),
            game = "mai2"
        )
    }
}
