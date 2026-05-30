package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.aquadx.aquacard.data.BestEntry
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.ProfileScore
import net.aquadx.aquacard.data.RecentEntry
import net.aquadx.aquacard.data.ScoreFormat

/**
 * Единая UI-модель детального просмотра песни/партии. Play-специфичные поля
 * (дата, место, рейтинг) опциональны — показываются только когда есть.
 */
data class ScoreDetail(
    val musicId: Int,
    val level: Int,
    val achievement: Int?,
    val comboStatus: Int? = null,
    val syncStatus: Int? = null,
    val deluxscore: Int? = null,
    val isClear: Boolean? = null,
    val playDate: String? = null,
    val placeName: String? = null,
    val beforeRating: Int? = null,
    val afterRating: Int? = null
)

fun RecentEntry.toScoreDetail() = ScoreDetail(
    musicId = musicId, level = level, achievement = achievement,
    comboStatus = comboStatus, syncStatus = syncStatus, deluxscore = deluxscore,
    isClear = isClear, playDate = playDate, placeName = placeName,
    beforeRating = beforeRating, afterRating = afterRating
)

fun ProfileScore.toScoreDetail() = ScoreDetail(
    musicId = musicId, level = level, achievement = achievement,
    comboStatus = comboStatus, syncStatus = syncStatus, deluxscore = deluxscore
)

fun BestEntry.toScoreDetail() = ScoreDetail(
    musicId = musicId, level = level, achievement = value
)

/** Детальный экран по песне: одинаков для Best, всех скоров и последних партий. */
@Composable
fun MusicDetailDialog(
    detail: ScoreDetail,
    meta: Map<Int, MusicMeta>,
    baseUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    JacketImage(baseUrl, detail.musicId, size = 64.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            ScoreFormat.songName(meta, detail.musicId),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            ScoreFormat.levelName(detail.level),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor(detail.level)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            ScoreFormat.achievementPercent(detail.achievement),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Достижение",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        ScoreFormat.maiRank(detail.achievement),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val badges = listOfNotNull(
                    ScoreFormat.comboLabel(detail.comboStatus),
                    ScoreFormat.syncLabel(detail.syncStatus),
                    if (detail.isClear == true) "CLEAR" else null
                )
                if (badges.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        badges.forEach { Badge(it) }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                DetailRow("DX-счёт", detail.deluxscore?.toString())
                DetailRow("Рейтинг", ratingDelta(detail.beforeRating, detail.afterRating))
                DetailRow("Место", detail.placeName?.takeIf { it.isNotBlank() })
                DetailRow("Сыграно", detail.playDate?.let(::prettyDateTime))

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Закрыть") }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private fun ratingDelta(before: Int?, after: Int?): String? {
    if (before == null && after == null) return null
    val b = before?.toString() ?: "?"
    val a = after?.toString() ?: "?"
    val delta = if (before != null && after != null) {
        val d = after - before
        " (${if (d >= 0) "+" else ""}$d)"
    } else ""
    return "$b → $a$delta"
}

/** "2026-05-29T20:11:00" -> "2026-05-29 20:11". */
private fun prettyDateTime(raw: String): String {
    val normalized = raw.replace('T', ' ')
    return if (normalized.length >= 16) normalized.substring(0, 16) else normalized
}
