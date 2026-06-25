package ru.norahobbits.talesbook.ui.screens.appearance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import ru.norahobbits.talesbook.settings.AppTheme
import ru.norahobbits.talesbook.settings.ContentViewMode
import ru.norahobbits.talesbook.ui.components.WindowSizeClass
import ru.norahobbits.talesbook.ui.components.rememberWindowSizeClass
import ru.norahobbits.talesbook.ui.theme.*
import ru.norahobbits.talesbook.ui.utils.shareTextFile

private data class ThemeOption(
    val theme: AppTheme,
    val label: String,
    val palette: TalesbookPalette
)

private data class TextColorOption(
    val label: String,
    val value: Long
)

private val themes = listOf(
    ThemeOption(AppTheme.EVENING_FOREST, "Вечерний лес", EveningForestPalette),
    ThemeOption(AppTheme.WITCH_LIBRARY, "Библиотека ведьмы", WitchLibraryPalette),
    ThemeOption(AppTheme.MOON_GARDEN, "Лунный сад", MoonGardenPalette),
    ThemeOption(AppTheme.HOBBIT_ROOM, "Нора хоббита", HobbitRoomPalette)
)

private val textColors = listOf(
    TextColorOption("Пергамент", 0xFFEFE8D8),
    TextColorOption("Тёплый свет", 0xFFFFE1A8),
    TextColorOption("Лунное серебро", 0xFFDDE6F0),
    TextColorOption("Мягкая трава", 0xFFCFE3C2)
)

private val fontOptions = listOf(
    "default" to "Спокойный",
    "serif" to "Книжный",
    "monospace" to "Моно",
    "cursive" to "Рукописный"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel()
) {
    val colors = LocalTalesbookColors.current
    val windowSize = rememberWindowSizeClass()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf<String?>(null) }

    val editorBgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setEditorBg(it.toString())
        }
    }

    val appBgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setAppBg(it.toString())
        }
    }

    val musicLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.setMusicUri(it.toString())
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(viewModel.exportBackup().toByteArray(Charsets.UTF_8))
                    } ?: error("Не удалось открыть файл")
                }.onSuccess {
                    statusText = "Копия сохранена"
                }.onFailure {
                    statusText = "Не удалось сохранить копию"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(it)?.use { input ->
                        input.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Не удалось открыть файл")
                    viewModel.importBackup(json)
                }.onSuccess {
                    statusText = "Копия загружена"
                }.onFailure {
                    statusText = "Не удалось загрузить копию"
                }
            }
        }
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = colors.accent)
                    }
                },
                title = {
                    Text(
                        "Оформление пространства",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.textPrimary
                )
            )
        }
    ) { padding ->
        val sectionModifier = if (windowSize == WindowSizeClass.Expanded) {
            Modifier.width(360.dp)
        } else {
            Modifier.fillMaxWidth()
        }
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (windowSize == WindowSizeClass.Compact) 16.dp else 24.dp,
                    vertical = 12.dp
                )
                .wrapContentWidth(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Темы
            SettingsSection("Тема оформления", modifier = sectionModifier) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(themes) { option ->
                        ThemeCard(
                            option = option,
                            selected = settings.selectedTheme == option.theme,
                            onClick = { viewModel.setTheme(option.theme) }
                        )
                    }
                }
            }

            // Размер шрифта
            SettingsSection("Размер шрифта", modifier = sectionModifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("А", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = settings.fontSize,
                        onValueChange = { viewModel.setFontSize(it) },
                        valueRange = 12f..24f,
                        steps = 5,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.accentSoft
                        )
                    )
                    Text("А", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "${settings.fontSize.toInt()} pt",
                    color = colors.textHint,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            SettingsSection("Шрифт текста", modifier = sectionModifier) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(fontOptions) { option ->
                        FilterChip(
                            selected = settings.fontFamily == option.first,
                            onClick = { viewModel.setFontFamily(option.first) },
                            label = { Text(option.second) },
                            leadingIcon = if (settings.fontFamily == option.first) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accentDim,
                                selectedLabelColor = colors.textPrimary,
                                labelColor = colors.textSecondary
                            )
                        )
                    }
                }
            }

            SettingsSection("Цвет текста", modifier = sectionModifier) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(textColors) { option ->
                        TextColorSwatch(
                            option = option,
                            selected = settings.textColor == option.value,
                            onClick = { viewModel.setTextColor(option.value) }
                        )
                    }
                }
            }

            SettingsSection("Вид списков", modifier = sectionModifier) {
                ViewModeRow(
                    title = "Книги",
                    selected = settings.bookViewMode,
                    onSelect = viewModel::setBookViewMode
                )
                ViewModeRow(
                    title = "Главы",
                    selected = settings.chapterViewMode,
                    onSelect = viewModel::setChapterViewMode
                )
            }

            SettingsSection("Фон библиотеки и книг", modifier = sectionModifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { appBgLauncher.launch(arrayOf("image/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceSoft, contentColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Wallpaper, null, tint = colors.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Выбрать фон")
                    }
                    if (settings.appBackgroundImageUri != null) {
                        IconButton(onClick = { viewModel.setAppBg(null) }) {
                            Icon(Icons.Default.Clear, null, tint = colors.danger)
                        }
                    }
                }
            }

            // Фон редактора (глобальный)
            SettingsSection("Фон редактора по умолчанию", modifier = sectionModifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { editorBgLauncher.launch(arrayOf("image/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceSoft, contentColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Image, null, tint = colors.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Выбрать изображение")
                    }
                    if (settings.editorBackgroundImageUri != null) {
                        IconButton(onClick = { viewModel.setEditorBg(null) }) {
                            Icon(Icons.Default.Clear, null, tint = colors.danger)
                        }
                    }
                }
            }

            // Музыка
            SettingsSection("Музыка для письма", modifier = sectionModifier) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Включить музыку", color = colors.textPrimary, modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.musicEnabled,
                        onCheckedChange = { viewModel.setMusicEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.accent
                        )
                    )
                }

                if (settings.musicEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { musicLauncher.launch(arrayOf("audio/*")) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceSoft, contentColor = colors.textPrimary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, null, tint = colors.accent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (settings.selectedMusicUri != null) "Изменить трек"
                                else "Выбрать музыку"
                            )
                        }
                    }

                    if (settings.selectedMusicUri != null) {
                        Column {
                            Text("Громкость", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = settings.musicVolume,
                                onValueChange = { viewModel.setMusicVolume(it) },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accent,
                                    activeTrackColor = colors.accent,
                                    inactiveTrackColor = colors.accentSoft
                                )
                            )
                        }
                    }
                }
            }

            SettingsSection("Копия на Google Drive", modifier = sectionModifier) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { exportLauncher.launch("book-of-tales-backup.json") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceSoft, contentColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null, tint = colors.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceSoft, contentColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, null, tint = colors.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Загрузить")
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                shareTextFile(
                                    context = context,
                                    fileName = "book-of-tales-backup.json",
                                    mimeType = "application/json",
                                    content = viewModel.exportBackup(),
                                    chooserTitle = "Поделиться копией"
                                )
                            }.onFailure {
                                statusText = "Не удалось поделиться копией"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent, contentColor = colors.background
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Поделиться копией")
                }
                statusText?.let {
                    Text(it, color = colors.textHint, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ViewModeRow(
    title: String,
    selected: ContentViewMode,
    onSelect: (ContentViewMode) -> Unit
) {
    val colors = LocalTalesbookColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.textPrimary, modifier = Modifier.weight(1f))
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = selected == ContentViewMode.LIST,
                onClick = { onSelect(ContentViewMode.LIST) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.ViewList, null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("Список")
            }
            SegmentedButton(
                selected = selected == ContentViewMode.CARDS,
                onClick = { onSelect(ContentViewMode.CARDS) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.GridView, null, modifier = Modifier.size(18.dp)) }
            ) {
                Text("Карточки")
            }
        }
    }
}

@Composable
private fun TextColorSwatch(
    option: TextColorOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(option.value))
                .then(
                    if (selected) Modifier.border(2.dp, colors.accent, RoundedCornerShape(14.dp))
                    else Modifier.border(1.dp, colors.accentDim, RoundedCornerShape(14.dp))
                )
        )
        Spacer(Modifier.height(6.dp))
        Text(
            option.label,
            color = if (selected) colors.accent else colors.textHint,
            style = MaterialTheme.typography.labelMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalTalesbookColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.accentSoft
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun ThemeCard(option: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(option.palette.background)
                .then(
                    if (selected) Modifier.border(2.dp, option.palette.accent, RoundedCornerShape(16.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(option.palette.accent.copy(alpha = 0.6f))
            )
        }
        Spacer(Modifier.height(6.dp))
        val labelColor = if (selected) option.palette.accent else LocalTalesbookColors.current.textHint
        Text(
            option.label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}
