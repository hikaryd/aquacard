package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import net.aquadx.aquacard.data.BestEntry
import net.aquadx.aquacard.data.JudgeBreakdown
import net.aquadx.aquacard.data.MusicMeta
import net.aquadx.aquacard.data.NoteBreakdown
import net.aquadx.aquacard.data.ProfileScore
import net.aquadx.aquacard.data.RecentEntry
import net.aquadx.aquacard.data.ScoreFormat

/**
 * Единая UI-модель детального просмотра. Богатые поля (джаджменты, точность по нотам,
 * комбо, deluxe, fast/late, рейтинг) есть только у партий (recent) — у Best/скоров null.
 */
data class ScoreDetail(
    val musicId: Int,
    val level: Int,
    val achievement: Int?,
    val deluxscore: Int? = null,
    val deluxscoreMax: Int? = null,
    val maxCombo: Int? = null,
    val totalCombo: Int? = null,
    val comboStatus: Int? = null,
    val syncStatus: Int? = null,
    val isClear: Boolean? = null,
    val fastCount: Int? = null,
    val lateCount: Int? = null,
    val beforeRating: Int? = null,
    val afterRating: Int? = null,
    val playDate: String? = null,
    val placeName: String? = null,
    val trackNo: Int? = null,
    val judges: JudgeBreakdown? = null,
    val notes: NoteBreakdown? = null
)

fun RecentEntry.toScoreDetail() = ScoreDetail(
    musicId = musicId, level = level, achievement = achievement,
    deluxscore = deluxscore, deluxscoreMax = totalCombo?.takeIf { it > 0 }?.let { it * 3 },
    maxCombo = maxCombo, totalCombo = totalCombo,
    comboStatus = comboStatus, syncStatus = syncStatus, isClear = isClear,
    fastCount = fastCount, lateCount = lateCount,
    beforeRating = beforeRating, afterRating = afterRating,
    playDate = playDate, placeName = placeName, trackNo = trackNo,
    judges = judges, notes = notes
)

fun ProfileScore.toScoreDetail() = ScoreDetail(
    musicId = musicId, level = level, achievement = achievement,
    deluxscore = deluxscore, comboStatus = comboStatus, syncStatus = syncStatus
)

fun BestEntry.toScoreDetail() = ScoreDetail(
    musicId = musicId, level = level, achievement = value
)

/** Детальный экран по песне/партии (как maimai TRACK RESULT). */
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
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // --- шапка ---
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
                            buildString {
                                append(ScoreFormat.levelName(detail.level))
                                detail.trackNo?.let { append("  ·  TRACK %02d".format(it)) }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = difficultyColor(detail.level)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // --- ачивка + ранг ---
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

                // --- судейство (только у партий) ---
                detail.judges?.let { j ->
                    SectionLabel("Судейство")
                    Row(Modifier.fillMaxWidth()) {
                        StatNum("CRIT", j.crit, JUDGE_CRIT)
                        StatNum("PERFECT", j.perfect, JUDGE_PERFECT)
                        StatNum("GREAT", j.great, JUDGE_GREAT)
                        StatNum("GOOD", j.good, JUDGE_GOOD)
                        StatNum("MISS", j.miss, JUDGE_MISS)
                    }
                }

                detail.notes?.let { n ->
                    SectionLabel("Точность по нотам")
                    Row(Modifier.fillMaxWidth()) {
                        StatNum("TAP", n.tap)
                        StatNum("HOLD", n.hold)
                        StatNum("SLIDE", n.slide)
                        StatNum("TOUCH", n.touch)
                        StatNum("BREAK", n.brk)
                    }
                }

                // --- статистика ---
                Spacer(Modifier.height(14.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                DetailRow("MAX COMBO", combo(detail.maxCombo, detail.totalCombo))
                DetailRow("DELUXE", combo(detail.deluxscore, detail.deluxscoreMax))
                DetailRow("FAST / LATE", fastLate(detail.fastCount, detail.lateCount))
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
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(14.dp))
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun RowScope.StatNum(label: String, value: Int, color: Color = Color.Unspecified) {
    Column(Modifier.weight(1f)) {
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private val JUDGE_CRIT = Color(0xFFFFD166)
private val JUDGE_PERFECT = Color(0xFFFFA94D)
private val JUDGE_GREAT = Color(0xFF63E6BE)
private val JUDGE_GOOD = Color(0xFF74C0FC)
private val JUDGE_MISS = Color(0xFFFF6B6B)

private fun combo(value: Int?, max: Int?): String? = when {
    value == null -> null
    max != null && max > 0 -> "$value / $max"
    else -> value.toString()
}

private fun fastLate(fast: Int?, late: Int?): String? {
    if (fast == null && late == null) return null
    return "${fast ?: 0} / ${late ?: 0}"
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

/** "2026-05-29T20:11:00" / "2026-05-29 20:11:00.0" -> "2026-05-29 20:11". */
private fun prettyDateTime(raw: String): String {
    val normalized = raw.replace('T', ' ').substringBefore('.')
    return if (normalized.length >= 16) normalized.substring(0, 16) else normalized
}
