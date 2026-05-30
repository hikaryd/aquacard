package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import net.aquadx.aquacard.R
import net.aquadx.aquacard.data.AquaAssets

/** Обложка песни maimai. Placeholder/error — локальный drawable (никогда внешний ассет). */
@Composable
fun JacketImage(baseUrl: String, musicId: Int, size: Dp = 44.dp) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(AquaAssets.jacketUrl(baseUrl, musicId))
            .crossfade(true)
            .build(),
        contentDescription = null,
        placeholder = painterResource(R.drawable.ic_jacket_placeholder),
        error = painterResource(R.drawable.ic_jacket_placeholder),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
    )
}

/** Портрет игрока: реальный аватар или локальные инициалы (при пусто/загрузке/404). */
@Composable
fun PlayerAvatar(baseUrl: String, profilePicture: String?, name: String, size: Dp = 56.dp) {
    val url = AquaAssets.avatarUrlOrNull(baseUrl, profilePicture)
    if (url == null) {
        InitialsAvatar(name, size)
        return
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        loading = { InitialsAvatar(name, size) },
        error = { InitialsAvatar(name, size) },
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
    )
}

/** Локальный аватар: детерминированный цвет из имени + инициалы. */
@Composable
fun InitialsAvatar(name: String, size: Dp = 56.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initialsOf(name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / 2.6f).sp
        )
    }
}

private val AVATAR_PALETTE = listOf(
    Color(0xFF7C9CFF), Color(0xFF22C55E), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFA855F7), Color(0xFF06B6D4)
)

private fun avatarColor(name: String): Color {
    if (name.isBlank()) return AVATAR_PALETTE[0]
    val idx = (name.hashCode() and 0x7fffffff) % AVATAR_PALETTE.size
    return AVATAR_PALETTE[idx]
}

private fun initialsOf(name: String): String {
    val parts = name.trim().split(' ', '　').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

/** Цвет сложности maimai по индексу уровня (BASIC..Re:MASTER). */
internal fun difficultyColor(level: Int): Color = when (level) {
    0 -> Color(0xFF22C55E)
    1 -> Color(0xFFF59E0B)
    2 -> Color(0xFFEF4444)
    3 -> Color(0xFFA855F7)
    4 -> Color(0xFFE879F9)
    else -> Color(0xFF9BA1A8)
}
