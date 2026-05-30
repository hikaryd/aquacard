@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.aquadx.aquacard.data.AquaApi
import net.aquadx.aquacard.data.AquaProfileRepository
import net.aquadx.aquacard.data.Card
import net.aquadx.aquacard.data.ProfileBundle

private val GAMES = listOf(
    "mai2" to "maimai DX",
    "chu3" to "CHUNITHM",
    "ongeki" to "O.N.G.E.K.I.",
    "wacca" to "WACCA"
)

@Composable
fun ProfileScreen(cards: List<Card>, baseUrl: String) {
    var selectedCardIndex by remember { mutableStateOf(0) }
    var username by remember { mutableStateOf("") }
    var game by remember { mutableStateOf("mai2") }
    var isLoading by remember { mutableStateOf(false) }
    var bundle by remember { mutableStateOf<ProfileBundle?>(null) }
    var loadedGame by remember { mutableStateOf("mai2") }
    var fatalError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedCardIndex, cards) {
        cards.getOrNull(selectedCardIndex)?.linkedAquaUsername?.takeIf { it.isNotBlank() }?.let {
            username = it
        }
    }

    fun load() {
        val name = username.trim()
        if (name.isBlank() || isLoading) return
        val requestedGame = game
        isLoading = true
        fatalError = null
        scope.launch {
            try {
                val repo = AquaProfileRepository(AquaApi.createProfileService(baseUrl))
                val result = repo.load(baseUrl, requestedGame, name)
                bundle = result
                loadedGame = requestedGame
            } catch (e: Exception) {
                fatalError = "Не удалось загрузить профиль: ${e.message ?: "неизвестная ошибка"}"
            } finally {
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Профиль AquaDX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Профили AquaDX ищутся по нику (username). Введите ник или выберите карту с привязанным ником.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (cards.isNotEmpty()) {
            item { CardSelector(cards, selectedCardIndex) { selectedCardIndex = it } }
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Юзернейм AquaDX") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { GameChips(game) { game = it } }

        item {
            Button(
                onClick = { load() },
                enabled = username.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Загрузить профиль")
            }
        }

        fatalError?.let { msg -> item { Banner(msg, isError = true) } }

        bundle?.let { b ->
            val nothingLoaded = b.summary == null && b.scores.isEmpty() && b.best.isEmpty()
            if (nothingLoaded) {
                item {
                    Banner(
                        "Профиль не загружен. Проверьте ник, игру и Base URL.\n" +
                            b.errors.joinToString("\n").ifBlank { "Игрок не найден." },
                        isError = true
                    )
                }
            } else {
                if (b.errors.isNotEmpty()) {
                    item { Banner("Часть данных недоступна: ${b.errors.joinToString(", ")}", isError = false) }
                }
                b.summary?.let { s -> item { ProfileHeader(s, b.detail, loadedGame) } }
                if (b.summary?.ranks?.isNotEmpty() == true) {
                    item { RankDistribution(b.summary!!.ranks) }
                }
                if (b.trend.isNotEmpty()) {
                    item { TrendChart(b.trend) }
                }
                if (b.best.isNotEmpty()) {
                    item { SectionHeader(primaryTitle(loadedGame), b.best.size) }
                    itemsIndexed(b.best) { i, e -> BestRow(i, e, loadedGame, b.meta) }
                }
                if (b.bestSecondary.isNotEmpty()) {
                    item { SectionHeader(secondaryTitle(loadedGame), b.bestSecondary.size) }
                    itemsIndexed(b.bestSecondary) { i, e -> BestRow(i, e, loadedGame, b.meta) }
                }
                if (b.recent.isNotEmpty()) {
                    item { SectionHeader("Недавние партии", b.recent.size) }
                    items(b.recent) { r -> RecentRow(r, b.meta) }
                }
                if (b.scores.isNotEmpty()) {
                    item { SectionHeader("Все скоры", b.scores.size) }
                    items(b.scores) { sc -> ScoreRow(sc, b.meta) }
                }
            }
        }
    }
}

@Composable
private fun CardSelector(cards: List<Card>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val current = cards.getOrNull(selectedIndex)
    Column {
        Text("Карта", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    current?.let { "${it.name} (${it.linkedAquaUsername ?: "нет ника"})" } ?: "Выберите карту",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                cards.forEachIndexed { idx, c ->
                    DropdownMenuItem(
                        text = { Text("${c.name} (${c.linkedAquaUsername ?: "нет ника"})") },
                        onClick = {
                            onSelect(idx)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameChips(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Игра", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GAMES.forEach { (code, label) ->
                FilterChip(
                    selected = selected == code,
                    onClick = { onSelect(code) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun Banner(message: String, isError: Boolean) {
    val container = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = content,
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun primaryTitle(game: String): String = when (game) {
    "chu3" -> "Best 30"
    else -> "Best 35"
}

private fun secondaryTitle(game: String): String = when (game) {
    "chu3" -> "Recent 10"
    else -> "Best 15 (новые)"
}
