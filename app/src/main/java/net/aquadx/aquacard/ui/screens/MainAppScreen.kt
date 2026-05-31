@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.aquadx.aquacard.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.aquadx.aquacard.crypto.AccessCode
import net.aquadx.aquacard.crypto.CardFormat
import net.aquadx.aquacard.data.*
import net.aquadx.aquacard.nfc.HceController
import net.aquadx.aquacard.ui.screens.profile.ProfileScreen
import java.security.SecureRandom
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    hceController: HceController,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val settingStore = remember { SettingStore(context) }
    
    // State management
    var currentTab by remember { mutableStateOf(0) }
    val cardsFlow = remember { db.cardDao().getAllCardsFlow() }
    val cards by cardsFlow.collectAsState(initial = emptyList())
    
    var emulatingCard by remember { mutableStateOf<Card?>(null) }
    var isHceSupported by remember { mutableStateOf(hceController.isHceFSupported()) }
    var isNfcEnabled by remember { mutableStateOf(hceController.isNfcEnabled()) }

    // Floating add dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<Card?>(null) }

    // Active URL configuration
    var baseUrl by remember { mutableStateOf(settingStore.getBaseUrl()) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "AquaCard", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                actions = {
                    if (currentTab == 0) {
                        IconButton(onClick = { showImportDialog = true }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Import card")
                        }
                    }
                    IconButton(onClick = {
                        isHceSupported = hceController.isHceFSupported()
                        isNfcEnabled = hceController.isNfcEnabled()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check Hardware")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Cards") },
                    label = { Text("Карты") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Nfc, contentDescription = "NFC") },
                    label = { Text("Эмуляция") }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.AccountBox, contentDescription = "Profile") },
                    label = { Text("Статистика") }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Настройки") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = { 
                    editingCard = null
                    showAddDialog = true 
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentTab) {
                0 -> CardsTab(
                    cards = cards,
                    onEmulate = { card ->
                        emulatingCard = card
                        currentTab = 1 // Switch to emulation tab
                    },
                    onEdit = { card ->
                        editingCard = card
                        showAddDialog = true
                    },
                    onDelete = { card ->
                        coroutineScope.launch {
                            db.cardDao().deleteCard(card)
                        }
                    }
                )
                1 -> EmulationTab(
                    card = emulatingCard,
                    cards = cards,
                    hceController = hceController,
                    isHceSupported = isHceSupported,
                    isNfcEnabled = isNfcEnabled,
                    onOpenSettings = onOpenSettings,
                    onSelectCard = { emulatingCard = it }
                )
                2 -> ProfileScreen(
                    cards = cards,
                    baseUrl = baseUrl
                )
                3 -> SettingsTab(
                    baseUrl = baseUrl,
                    onBaseUrlChange = {
                        baseUrl = it
                        settingStore.setBaseUrl(it)
                    },
                    onResetBaseUrl = {
                        settingStore.resetToDefault()
                        baseUrl = settingStore.getBaseUrl()
                    },
                    cards = cards
                )
            }

            if (showAddDialog) {
                AddEditCardDialog(
                    card = editingCard,
                    onDismiss = { showAddDialog = false },
                    onSave = { name, idm, color, note, username ->
                        coroutineScope.launch {
                            val newCard = Card(
                                id = editingCard?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                idm = idm.uppercase(),
                                color = color,
                                note = note.takeIf { it.isNotBlank() },
                                linkedAquaUsername = username.takeIf { it.isNotBlank() },
                                createdAt = editingCard?.createdAt ?: System.currentTimeMillis()
                            )
                            db.cardDao().insertCard(newCard)
                            showAddDialog = false
                        }
                    }
                )
            }

            if (showImportDialog) {
                ImportCardDialog(
                    onDismiss = { showImportDialog = false },
                    onImport = { name, idm ->
                        coroutineScope.launch {
                            db.cardDao().insertCard(Card(name = name, idm = idm, color = "#7C9CFF"))
                            showImportDialog = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CardsTab(
    cards: List<Card>,
    onEmulate: (Card) -> Unit,
    onEdit: (Card) -> Unit,
    onDelete: (Card) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    if (cards.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    Icons.Default.CreditCard, 
                    contentDescription = null, 
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("У вас пока нет сохраненных карт", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Нажмите кнопку + внизу, чтобы добавить Amusement IC карту.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cards, key = { it.id }) { card ->
                CardItemRow(
                    card = card,
                    onEmulate = { onEmulate(card) },
                    onEdit = { onEdit(card) },
                    onDelete = { onDelete(card) },
                    onCopyAccessCode = {
                        val code = AccessCode.aquaAccessCode(card.idm)
                        clipboard.setText(AnnotatedString(code))
                        Toast.makeText(context, "Access Code скопирован!", Toast.LENGTH_SHORT).show()
                    },
                    onCopySerial = {
                        clipboard.setText(AnnotatedString(CardFormat.serialNumber(card.idm)))
                        Toast.makeText(context, "Serial Number скопирован!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun CardItemRow(
    card: Card,
    onEmulate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyAccessCode: () -> Unit,
    onCopySerial: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val emulatable = remember(card.idm) { CardFormat.isEmulatable(card.idm) }
    val parsedColor = remember(card.color) {
        try {
            Color(android.graphics.Color.parseColor(card.color))
        } catch (e: Exception) {
            Color(0xFF3F51B5)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = parsedColor.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Colored Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(parsedColor)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("IDm: ${card.idm}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    Text(
                        if (emulatable) "Эмулируемая" else "Физическая · без эмуляции",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (emulatable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (emulatable) {
                    IconButton(onClick = onEmulate) {
                        Icon(Icons.Default.Nfc, contentDescription = "Emulate", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Details"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                if (!card.note.isNullOrBlank()) {
                    Text("Заметка:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(card.note, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!card.linkedAquaUsername.isNullOrBlank()) {
                    Text("Пользователь AquaDX:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(card.linkedAquaUsername, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    "ПРИВЯЗКА В AQUADX (Link Card): вставь Serial Number или Access Code → выбери игры → Link",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Serial Number (IDm):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        CardFormat.serialNumber(card.idm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCopySerial, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Serial", modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Text("Access Code (20 цифр):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        CardFormat.groupedAccessCode(card.idm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCopyAccessCode, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Access", modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (!emulatable) {
                    Text(
                        "Физическая карта: телефон не может её эмулировать (Android HCE-F требует IDm 02FE). Привязать и смотреть статистику можно.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Править")
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Удалить")
                    }
                }
            }
        }
    }
}

@Composable
fun EmulationTab(
    card: Card?,
    cards: List<Card>,
    hceController: HceController,
    isHceSupported: Boolean,
    isNfcEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onSelectCard: (Card) -> Unit
) {
    val activity = LocalContext.current as? Activity
    var activeIdm by remember { mutableStateOf(card?.idm ?: "") }
    var isEmulating by remember { mutableStateOf(false) }

    val selectedCard = cards.find { it.idm == activeIdm }

    // Pulsing Animation configuration
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "pulseAlpha"
    )

    // Automatically manage NFC Service registration
    LaunchedEffect(activeIdm, isEmulating) {
        if (isEmulating && activeIdm.isNotBlank() && activity != null) {
            hceController.selectCard(activeIdm)
            hceController.enableEmulation(activity)
        } else if (activity != null) {
            hceController.disableEmulation(activity)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (activity != null) {
                hceController.disableEmulation(activity)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (!isHceSupported) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "HCE-F не поддерживается",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Ваше устройство не имеет поддержки эмуляции карт Felica (HCE-F). Вы не сможете эмулировать карты по NFC в кабинетах, однако можете использовать Access Code на сайте.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else if (!isNfcEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NFC отключен", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Пожалуйста, включите NFC для осуществления эмуляции.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onOpenSettings) {
                        Text("Открыть настройки")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Выбор активной карты для NFC:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // Card picker UI
        var showPickerDropdown by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showPickerDropdown = true }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CreditCard, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    selectedCard?.name ?: "Выберите карту...",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = showPickerDropdown,
                onDismissRequest = { showPickerDropdown = false },
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                cards.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.name} (${c.idm})") },
                        onClick = {
                            activeIdm = c.idm
                            onSelectCard(c)
                            showPickerDropdown = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Circular Pulsing Emulator View
        if (isEmulating && selectedCard != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(240.dp)
            ) {
                // Pulse waves
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .size((200 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha))
                )
                // Solid center
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Nfc, 
                            contentDescription = null, 
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Излучение...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Приложите телефон к ридеру", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Серверный код: 4000 • IDm: $activeIdm",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            IconButton(
                onClick = { 
                    if (selectedCard != null && isHceSupported && isNfcEnabled) {
                        isEmulating = true 
                    }
                },
                enabled = selectedCard != null && isHceSupported && isNfcEnabled,
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedCard != null && isHceSupported && isNfcEnabled) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
            ) {
                Icon(
                    Icons.Default.Nfc, 
                    contentDescription = "Tap to Emulate",
                    modifier = Modifier.size(72.dp),
                    tint = if (selectedCard != null && isHceSupported && isNfcEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (selectedCard == null) "Выберите карту для запуска эмуляции" else "Нажмите для запуска эмуляции",
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(0.5f))

        if (isEmulating) {
            Button(
                onClick = { isEmulating = false },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Остановить эмуляцию")
            }
        }
    }
}

@Composable
fun SettingsTab(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    onResetBaseUrl: () -> Unit,
    cards: List<Card>
) {
    var urlText by remember(baseUrl) { mutableStateOf(baseUrl) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Настройки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Базовый URL AquaDX", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Используется для обращения к веб-интерфейсу API AquaNet для связывания и статистики.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onBaseUrlChange(urlText) }) {
                            Text("Сохранить")
                        }
                        TextButton(onClick = {
                            onResetBaseUrl()
                            Toast.makeText(context, "Сброшено на стандартный URL", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Сброс")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Инструкция по привязке карт", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "1. Сгенерируйте в приложении новую виртуальную карту (02FE...).\n" +
                        "2. Нажмите правой кнопкой мыши или раскройте карту, скопируйте её 20-значный Access Code.\n" +
                        "3. Зайдите в личный кабинет на сайте вашего сервера AquaDX (например, aquadx.net).\n" +
                        "4. В разделе \"Link Card\" введите скопированный 20-значный код и свяжите его со своей учётной записью.\n" +
                        "5. Начните эмуляцию карты в приложении AquaCard и приложите телефон к NFC-ридеру автомата!",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Экспорт / Импорт карт", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Вы можете скопировать список ваших карт в формате JSON, совместимым с eamemu.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            // Compile JSON export
                            val jsonBuilder = StringBuilder("[")
                            cards.forEachIndexed { index, card ->
                                jsonBuilder.append("""{"name":"${card.name}","sid":"${card.idm}","image":"${card.color}"}""")
                                if (index < cards.size - 1) jsonBuilder.append(",")
                            }
                            jsonBuilder.append("]")
                            clipboard.setText(AnnotatedString(jsonBuilder.toString()))
                            Toast.makeText(context, "Импорт-JSON скопирован в буфер обмена!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Экспортировать JSON")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardDialog(
    card: Card?,
    onDismiss: () -> Unit,
    onSave: (name: String, idm: String, color: String, note: String, username: String) -> Unit
) {
    var name by remember { mutableStateOf(card?.name ?: "") }
    var idmRaw by remember { mutableStateOf(card?.idm?.removePrefix("02FE") ?: "") }
    var color by remember { mutableStateOf(card?.color ?: "#4CAF50") }
    var note by remember { mutableStateOf(card?.note ?: "") }
    var username by remember { mutableStateOf(card?.linkedAquaUsername ?: "") }

    val isIdmValid = idmRaw.length == 12 && idmRaw.matches(Regex("[0-9A-Fa-f]{12}"))

    val colorPresets = listOf(
        "#4CAF50" to "Зеленый",
        "#2196F3" to "Синий",
        "#9C27B0" to "Фиолетовый",
        "#FF9800" to "Оранжевый",
        "#E91E63" to "Розовый",
        "#607D8B" to "Серый"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (card == null) "Добавить карту" else "Редактировать карту") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название карты") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = idmRaw,
                        onValueChange = { 
                            if (it.length <= 12) {
                                idmRaw = it.filter { char -> char.isDigit() || char.lowercaseChar() in 'a'..'f' }
                            }
                        },
                        label = { Text("IDm суффикс (12 hex)") },
                        prefix = { Text("02FE") },
                        isError = idmRaw.isNotEmpty() && !isIdmValid,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("DEADBEEF1234") }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val secureRandom = SecureRandom()
                            val bytes = ByteArray(6)
                            secureRandom.nextBytes(bytes)
                            idmRaw = bytes.joinToString("") { String.format("%02X", it) }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Рандом", fontSize = 11.sp)
                    }
                }

                if (idmRaw.isNotEmpty() && !isIdmValid) {
                    Text(
                        "Требуется ровно 12 шестнадцатеричных символов (A-F, 0-9)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Юзернейм AquaDX (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Цветовое оформление:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPresets.forEach { (hex, _) ->
                        val presetColor = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .clickable { color = hex }
                                .padding(2.dp)
                        ) {
                            if (color == hex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && isIdmValid) {
                        onSave(name, "02FE${idmRaw.uppercase()}", color, note, username)
                    }
                },
                enabled = name.isNotBlank() && isIdmValid
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
