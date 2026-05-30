@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.aquadx.aquacard.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.aquadx.aquacard.data.AquaApi
import net.aquadx.aquacard.data.AquaProfileRepository
import net.aquadx.aquacard.data.CachePolicy
import net.aquadx.aquacard.data.Card
import net.aquadx.aquacard.data.Decision
import net.aquadx.aquacard.data.ProfileBundle
import net.aquadx.aquacard.data.ProfileCache
import net.aquadx.aquacard.data.SettingStore

@Composable
fun ProfileScreen(cards: List<Card>, baseUrl: String) {
    val context = LocalContext.current
    val cache = remember { ProfileCache.of(context) }
    val settings = remember { SettingStore(context) }
    val scope = rememberCoroutineScope()

    var selectedCardIndex by remember { mutableStateOf(0) }
    // Приоритет ника: last_username → ник первой карты → пусто. Дальше перезапишет выбор карты/ввод.
    var username by remember {
        mutableStateOf(
            settings.getLastUsername()
                ?: cards.getOrNull(0)?.linkedAquaUsername?.takeIf { it.isNotBlank() }
                ?: ""
        )
    }
    var bundle by remember { mutableStateOf<ProfileBundle?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var fatalError by remember { mutableStateOf<String?>(null) }
    var selectedDetail by remember { mutableStateOf<ScoreDetail?>(null) }

    // Сетевое обновление с guard'ом: применяем результат только если ник в поле не сменился.
    fun refresh(name: String) {
        if (name.isBlank()) return
        isRefreshing = true
        fatalError = null
        scope.launch {
            try {
                val repo = AquaProfileRepository(AquaApi.createProfileService(baseUrl))
                val result = repo.load(baseUrl, name)
                if (username.trim() == name) {
                    bundle = result
                    cache.write(name, result, System.currentTimeMillis())
                    settings.setLastUsername(name)
                }
            } catch (e: Exception) {
                if (username.trim() == name) {
                    fatalError = "Не удалось обновить профиль: ${e.message ?: "неизвестная ошибка"}"
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    // Кэш-первый: мгновенно показываем кэш, обновляем по решению CachePolicy.
    fun autoLoad(manualRefresh: Boolean) {
        val q = username.trim()
        val cached = if (q.isNotBlank()) cache.read(q) else null
        val decision = CachePolicy.decide(
            query = q.ifBlank { null },
            cached = cached,
            now = System.currentTimeMillis(),
            manualRefresh = manualRefresh
        )
        when (decision) {
            Decision.Idle -> {}
            Decision.ServeCachedOnly -> cached?.let { bundle = it.toBundle() }
            Decision.ServeCachedThenRefresh -> {
                cached?.let { bundle = it.toBundle() }
                refresh(q)
            }
            Decision.RefreshOnly -> refresh(q)
        }
    }

    // Авто-загрузка при открытии вкладки (composition пересоздаётся при возврате на вкладку).
    LaunchedEffect(Unit) { autoLoad(manualRefresh = false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Профиль AquaDX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Профиль maimai обновляется сам при открытии. Можно сменить ник или обновить вручную.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { autoLoad(manualRefresh = true) }, enabled = !isRefreshing) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Обновить")
                }
            }
        }

        if (isRefreshing) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        if (cards.isNotEmpty()) {
            item {
                CardSelector(cards, selectedCardIndex) { idx ->
                    selectedCardIndex = idx
                    cards.getOrNull(idx)?.linkedAquaUsername?.takeIf { it.isNotBlank() }?.let { username = it }
                    autoLoad(manualRefresh = false)
                }
            }
        }

        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Юзернейм AquaDX") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { autoLoad(manualRefresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Загрузить")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { autoLoad(manualRefresh = true) }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        fatalError?.let { msg -> item { Banner(msg, isError = true) } }

        bundle?.let { b ->
            val nothingLoaded = b.summary == null && b.scores.isEmpty() && b.best.isEmpty()
            if (nothingLoaded) {
                item {
                    Banner(
                        "Профиль не загружен. Проверьте ник и Base URL.\n" +
                            b.errors.joinToString("\n").ifBlank { "Игрок не найден." },
                        isError = true
                    )
                }
            } else {
                if (b.errors.isNotEmpty()) {
                    item { Banner("Часть данных недоступна: ${b.errors.joinToString(", ")}", isError = false) }
                }
                b.summary?.let { s -> item { ProfileHeader(s, b.detail, baseUrl) } }
                if (b.recent.isNotEmpty()) {
                    val recent5 = b.recent.take(5)
                    item { SectionHeader("Последние партии", recent5.size) }
                    itemsIndexed(recent5, key = { i, r -> "rc-$i-${r.musicId}" }) { _, r ->
                        RecentRow(r, b.meta, baseUrl, onClick = { selectedDetail = r.toScoreDetail() })
                    }
                }
                if (b.best.isNotEmpty()) {
                    item { SectionHeader("Best 35", b.best.size) }
                    itemsIndexed(b.best, key = { _, e -> "b1-${e.musicId}-${e.level}" }) { i, e -> BestRow(i, e, b.meta, baseUrl, onClick = { selectedDetail = e.toScoreDetail() }) }
                }
                if (b.bestSecondary.isNotEmpty()) {
                    item { SectionHeader("Best 15 (новые)", b.bestSecondary.size) }
                    itemsIndexed(b.bestSecondary, key = { _, e -> "b2-${e.musicId}-${e.level}" }) { i, e -> BestRow(i, e, b.meta, baseUrl, onClick = { selectedDetail = e.toScoreDetail() }) }
                }
                if (b.trend.isNotEmpty()) {
                    item { TrendChart(b.trend) }
                }
                if (b.summary?.ranks?.isNotEmpty() == true) {
                    item { RankDistribution(b.summary!!.ranks) }
                }
                if (b.scores.isNotEmpty()) {
                    item { SectionHeader("Все скоры", b.scores.size) }
                    items(b.scores, key = { "sc-${it.musicId}-${it.level}" }) { sc -> ScoreRow(sc, b.meta, baseUrl, onClick = { selectedDetail = sc.toScoreDetail() }) }
                }
            }
        }
    }

    selectedDetail?.let { d ->
        MusicDetailDialog(
            detail = d,
            meta = bundle?.meta ?: emptyMap(),
            baseUrl = baseUrl,
            onDismiss = { selectedDetail = null }
        )
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
