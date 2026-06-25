package ru.norahobbits.talesbook.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import ru.norahobbits.talesbook.data.model.Chapter
import ru.norahobbits.talesbook.settings.AppSettings
import ru.norahobbits.talesbook.ui.components.WindowSizeClass
import ru.norahobbits.talesbook.ui.components.rememberWindowSizeClass
import ru.norahobbits.talesbook.ui.theme.LocalTalesbookColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterEditorScreen(
    chapterId: Long,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onOpenChapter: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChapterEditorViewModel = hiltViewModel()
) {
    val colors = LocalTalesbookColors.current
    val windowSize = rememberWindowSizeClass()
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsState()
    val chapter = state.chapter

    var titleValue by remember(chapter?.id) { mutableStateOf(chapter?.title ?: "") }
    var contentValue by remember(chapter?.id) { mutableStateOf(TextFieldValue(chapter?.content ?: "")) }
    var focusMode by remember { mutableStateOf(false) }
    var localFontSize by remember(appSettings.fontSize) { mutableFloatStateOf(appSettings.fontSize) }
    val focusRequester = remember { FocusRequester() }

    val context = LocalContext.current
    val bgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateBackground(it.toString())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.saveNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.saveNow()
        }
    }

    LaunchedEffect(chapter) {
        if (chapter != null) {
            if (titleValue != chapter.title) titleValue = chapter.title
            if (contentValue.text != chapter.content) {
                contentValue = TextFieldValue(chapter.content)
            }
            focusRequester.requestFocus()
        }
    }

    BackHandler {
        viewModel.saveNow()
        onBack()
    }

    val wordCount = remember(contentValue.text) {
        contentValue.text.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val charCountWithSpaces = contentValue.text.length
    val charCountWithoutSpaces = remember(contentValue.text) {
        contentValue.text.count { !it.isWhitespace() }
    }
    val authorSheets = charCountWithSpaces / 40_000.0

    val textColor = Color(appSettings.textColor)
    val fontSize = localFontSize

    // Chapter background takes priority over global setting
    val effectiveBgUri = chapter?.backgroundImageUri ?: appSettings.editorBackgroundImageUri

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image
        if (effectiveBgUri != null) {
            AsyncImage(
                model = effectiveBgUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.25f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = if (effectiveBgUri != null) 0.7f else 1f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = !focusMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.saveNow(); onBack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = colors.accent)
                        }
                    },
                    title = {
                        BasicTextField(
                            value = titleValue,
                            onValueChange = { newTitle ->
                                titleValue = newTitle
                                viewModel.updateTitle(newTitle)
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = colors.textPrimary),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    actions = {
                        IconButton(onClick = { bgLauncher.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.Wallpaper, null, tint = colors.accentSoft)
                        }
                        if (chapter?.backgroundImageUri != null) {
                            IconButton(onClick = { viewModel.updateBackground(null) }) {
                                Icon(Icons.Default.HideImage, null, tint = colors.accentSoft)
                            }
                        }
                        IconButton(onClick = { focusMode = true }) {
                            Icon(Icons.Default.VisibilityOff, null, tint = colors.accentSoft)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Tune, null, tint = colors.accentSoft)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                    )
                )
            }

            Row(modifier = Modifier.weight(1f)) {
                if (!focusMode && windowSize == WindowSizeClass.Expanded && state.chapters.size > 1) {
                    ChapterRail(
                        chapters = state.chapters,
                        currentChapterId = chapterId,
                        onOpenChapter = {
                            viewModel.saveNow()
                            onOpenChapter(it)
                        }
                    )
                }

                // Текстовый редактор
                Box(modifier = Modifier.weight(1f)) {
                val editorMaxWidth = when (windowSize) {
                    WindowSizeClass.Compact -> Dp.Unspecified
                    WindowSizeClass.Medium -> 680.dp
                    WindowSizeClass.Expanded -> 780.dp
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = contentValue,
                    onValueChange = { newValue ->
                        contentValue = newValue
                        viewModel.updateContent(newValue.text)
                    },
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.7f).sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = editorMaxWidth)
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(
                            horizontal = if (windowSize == WindowSizeClass.Compact) 20.dp else 32.dp,
                            vertical = if (windowSize == WindowSizeClass.Compact) 12.dp else 28.dp
                        )
                        .focusRequester(focusRequester)
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when {
                                event.isCtrlPressed && event.key == Key.S -> {
                                    viewModel.saveNow()
                                    true
                                }
                                event.isCtrlPressed && (event.key == Key.Plus || event.key == Key.NumPadAdd || event.key == Key.Equals) -> {
                                    localFontSize = (localFontSize + 1f).coerceAtMost(28f)
                                    true
                                }
                                event.isCtrlPressed && (event.key == Key.Minus || event.key == Key.NumPadSubtract) -> {
                                    localFontSize = (localFontSize - 1f).coerceAtLeast(12f)
                                    true
                                }
                                event.key == Key.Escape && focusMode -> {
                                    focusMode = false
                                    true
                                }
                                else -> false
                            }
                        }
                        .verticalScroll(rememberScrollState()),
                    decorationBox = { inner ->
                        Box {
                            if (contentValue.text.isEmpty()) {
                                Text(
                                    "Начните свою историю здесь...",
                                    color = colors.textHint,
                                    fontSize = fontSize.sp
                                )
                            }
                            inner()
                        }
                    }
                )
                }
            }

            // Нижняя панель
            AnimatedVisibility(
                visible = !focusMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                BottomBar(
                    wordCount = wordCount,
                    charCountWithSpaces = charCountWithSpaces,
                    charCountWithoutSpaces = charCountWithoutSpaces,
                    authorSheets = authorSheets,
                    isSaving = state.isSaving,
                    fontSize = fontSize,
                    onFontSizeChange = { /* через AppearanceSettings */ }
                )
            }

            // Кнопка выхода из фокус-режима
            if (focusMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { focusMode = false },
                        modifier = Modifier
                            .background(colors.surfaceSoft.copy(alpha = 0.7f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Visibility, null, tint = colors.accentSoft)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRail(
    chapters: List<Chapter>,
    currentChapterId: Long,
    onOpenChapter: (Long) -> Unit
) {
    val colors = LocalTalesbookColors.current
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(colors.surface.copy(alpha = 0.72f))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Главы",
            color = colors.accentSoft,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            chapters.forEach { chapter ->
                val selected = chapter.id == currentChapterId
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) colors.surfaceElevated.copy(alpha = 0.9f)
                            else Color.Transparent
                        )
                        .clickable(enabled = !selected) { onOpenChapter(chapter.id) }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text(
                        chapter.title,
                        color = if (selected) colors.textPrimary else colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chapter.wordCount > 0) {
                        Text(
                            "${chapter.wordCount} сл",
                            color = colors.textHint,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    singleLine: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalesbookColors.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = textStyle,
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier
    )
}

@Composable
private fun BottomBar(
    wordCount: Int,
    charCountWithSpaces: Int,
    charCountWithoutSpaces: Int,
    authorSheets: Double,
    isSaving: Boolean,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit
) {
    val colors = LocalTalesbookColors.current
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "$wordCount сл · $charCountWithSpaces зн · ${"%.2f".format(authorSheets)} а. л.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textHint
                )
                Text(
                    "$charCountWithoutSpaces зн без пробелов",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textHint
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = isSaving) {
                Text(
                    "Сохраняется...",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentSoft
                )
            }
            AnimatedVisibility(visible = !isSaving) {
                Text(
                    "Сохранено",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textHint
                )
            }
        }
    }
}
