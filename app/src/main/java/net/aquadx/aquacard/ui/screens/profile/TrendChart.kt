package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.aquadx.aquacard.data.TrendPoint
import net.aquadx.aquacard.ui.theme.AquaCardTheme

/** Линейный график роста рейтинга на Canvas с фиксированной высотой. */
@Composable
fun TrendChart(points: List<TrendPoint>) {
    val data = points.mapNotNull { p -> p.rating?.let { p.date to it } }

    Column {
        Text(
            "Рост рейтинга",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        if (data.size < 2) {
            Text(
                "Недостаточно данных для графика",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val ratings = data.map { it.second }
        val minR = ratings.minOrNull() ?: 0
        val maxR = ratings.maxOrNull() ?: 0
        val range = (maxR - minR).coerceAtLeast(1)
        val lineColor = MaterialTheme.colorScheme.primary
        val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val n = ratings.size
            val stepX = if (n == 1) 0f else size.width / (n - 1)
            val line = Path()
            val area = Path()
            ratings.forEachIndexed { i, r ->
                val x = stepX * i
                val y = size.height * (1f - (r - minR).toFloat() / range)
                if (i == 0) {
                    line.moveTo(x, y)
                    area.moveTo(x, size.height)
                    area.lineTo(x, y)
                } else {
                    line.lineTo(x, y)
                    area.lineTo(x, y)
                }
            }
            area.lineTo(stepX * (n - 1), size.height)
            area.close()
            drawPath(area, color = fillColor)
            drawPath(line, color = lineColor, style = Stroke(width = 3.dp.toPx()))
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(data.first().first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$minR → $maxR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(data.last().first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview
@Composable
private fun TrendChartPreview() {
    AquaCardTheme {
        TrendChart(
            listOf(
                TrendPoint("2025-05-13", 691, 3),
                TrendPoint("2025-05-20", 1368, 5),
                TrendPoint("2025-06-01", 9800, 12),
                TrendPoint("2025-07-01", 14200, 30),
                TrendPoint("2026-05-29", 16666, 60)
            )
        )
    }
}
