package ru.norahobbits.talesbook.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
    var contentHtml by remember(chapter?.id) { mutableStateOf(chapter?.content ?: "") }
    var contentPlainText by remember(chapter?.id) { mutableStateOf(htmlToPlainText(chapter?.content.orEmpty())) }
    var focusMode by remember { mutableStateOf(false) }
    var localFontSize by remember(appSettings.fontSize) { mutableFloatStateOf(appSettings.fontSize) }
    var editorActions by remember { mutableStateOf<RichEditorActions?>(null) }
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
            if (contentHtml != chapter.content) {
                contentHtml = chapter.content
                contentPlainText = htmlToPlainText(chapter.content)
            }
        }
    }

    BackHandler {
        viewModel.saveNow()
        onBack()
    }

    val wordCount = remember(contentPlainText) {
        contentPlainText.trim().split("\\s+".toRegex()).count { it.isNotEmpty() }
    }
    val charCountWithSpaces = contentPlainText.length
    val charCountWithoutSpaces = remember(contentPlainText) {
        contentPlainText.count { !it.isWhitespace() }
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
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = editorMaxWidth)
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(
                                horizontal = if (windowSize == WindowSizeClass.Compact) 16.dp else 32.dp,
                                vertical = if (windowSize == WindowSizeClass.Compact) 10.dp else 20.dp
                            )
                    ) {
                        ChapterTitleStrip(title = titleValue)
                        AnimatedVisibility(visible = !focusMode) {
                            FormattingToolbar(
                                onBold = { editorActions?.bold?.invoke() },
                                onItalic = { editorActions?.italic?.invoke() },
                                onUnderline = { editorActions?.underline?.invoke() }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            key(chapterId) {
                                RichTextEditor(
                                    initialHtml = contentHtml,
                                    textColor = textColor,
                                    fontSize = fontSize,
                                    fontFamily = appSettings.fontFamily,
                                    onActionsReady = { editorActions = it },
                                    onContentChanged = { html, plain ->
                                        contentHtml = html
                                        contentPlainText = plain
                                        viewModel.updateContent(html)
                                    },
                                    onSave = { viewModel.saveNow() },
                                    onIncreaseFont = { localFontSize = (localFontSize + 1f).coerceAtMost(28f) },
                                    onDecreaseFont = { localFontSize = (localFontSize - 1f).coerceAtLeast(12f) },
                                    onExitFocus = { focusMode = false },
                                    focusMode = focusMode,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    if (windowSize == WindowSizeClass.Expanded) {
                        FontSizeCornerControls(
                            onIncrease = { localFontSize = (localFontSize + 1f).coerceAtMost(28f) },
                            onDecrease = { localFontSize = (localFontSize - 1f).coerceAtLeast(12f) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 14.dp, end = 14.dp)
                        )
                    }
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
private fun ChapterTitleStrip(title: String) {
    val colors = LocalTalesbookColors.current
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title.ifBlank { "Без названия" },
            color = colors.textSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun FormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        FilledTonalIconButton(onClick = onBold) {
            Text("B", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        }
        FilledTonalIconButton(onClick = onItalic) {
            Text("I", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        }
        FilledTonalIconButton(onClick = onUnderline) {
            Text("U", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FontSizeCornerControls(
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalesbookColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface.copy(alpha = 0.78f))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(onClick = onIncrease) {
            Icon(Icons.Default.Add, null, tint = colors.accent)
        }
        IconButton(onClick = onDecrease) {
            Icon(Icons.Default.Remove, null, tint = colors.accent)
        }
    }
}

private data class RichEditorActions(
    val bold: () -> Unit,
    val italic: () -> Unit,
    val underline: () -> Unit
)

@Composable
private fun RichTextEditor(
    initialHtml: String,
    textColor: Color,
    fontSize: Float,
    fontFamily: String,
    onActionsReady: (RichEditorActions) -> Unit,
    onContentChanged: (html: String, plain: String) -> Unit,
    onSave: () -> Unit,
    onIncreaseFont: () -> Unit,
    onDecreaseFont: () -> Unit,
    onExitFocus: () -> Unit,
    focusMode: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalesbookColors.current
    val context = LocalContext.current
    var editTextRef by remember { mutableStateOf<EditText?>(null) }

    DisposableEffect(editTextRef) {
        val editText = editTextRef ?: return@DisposableEffect onDispose {}
        onActionsReady(
            RichEditorActions(
                bold = { editText.toggleStyle(Typeface.BOLD) },
                italic = { editText.toggleStyle(Typeface.ITALIC) },
                underline = { editText.toggleUnderline() }
            )
        )
        onDispose {}
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface.copy(alpha = 0.42f)),
        factory = {
            EditText(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setTextColor(textColor.toArgb())
                setHintTextColor(colors.textHint.toArgb())
                hint = "Начните свою историю здесь..."
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize)
                typeface = androidTypeface(fontFamily)
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                minLines = 16
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                imeOptions = EditorInfo.IME_ACTION_NONE
                setPadding(18.dp.value.toInt(), 16.dp.value.toInt(), 18.dp.value.toInt(), 16.dp.value.toInt())
                setText(Html.fromHtml(initialHtml, Html.FROM_HTML_MODE_LEGACY))
                setSelection(text?.length ?: 0)
                requestFocus()

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        val html = Html.toHtml(s ?: Spannable.Factory.getInstance().newSpannable(""), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                        onContentChanged(html, s?.toString().orEmpty())
                    }
                })

                setOnKeyListener { _, keyCode, event ->
                    if (event.action != AndroidKeyEvent.ACTION_DOWN) return@setOnKeyListener false
                    val ctrl = event.isCtrlPressed
                    when {
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_S -> {
                            onSave()
                            true
                        }
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_B -> {
                            toggleStyle(Typeface.BOLD)
                            true
                        }
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_I -> {
                            toggleStyle(Typeface.ITALIC)
                            true
                        }
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_EQUALS -> {
                            onIncreaseFont()
                            true
                        }
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_MINUS -> {
                            onDecreaseFont()
                            true
                        }
                        focusMode && keyCode == AndroidKeyEvent.KEYCODE_ESCAPE -> {
                            onExitFocus()
                            true
                        }
                        else -> false
                    }
                }
                editTextRef = this
            }
        },
        update = { editText ->
            editText.setTextColor(textColor.toArgb())
            editText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize)
            editText.typeface = androidTypeface(fontFamily)
        }
    )
}

private fun EditText.toggleStyle(style: Int) {
    val start = selectionStart.coerceAtMost(selectionEnd).coerceAtLeast(0)
    val end = selectionStart.coerceAtLeast(selectionEnd).coerceAtMost(text.length)
    if (start == end) return
    text.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

private fun EditText.toggleUnderline() {
    val start = selectionStart.coerceAtMost(selectionEnd).coerceAtLeast(0)
    val end = selectionStart.coerceAtLeast(selectionEnd).coerceAtMost(text.length)
    if (start == end) return
    text.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

private fun androidTypeface(fontFamily: String): Typeface = when (fontFamily) {
    "serif" -> Typeface.SERIF
    "monospace" -> Typeface.MONOSPACE
    "cursive" -> Typeface.create("casual", Typeface.NORMAL)
    else -> Typeface.SANS_SERIF
}

private fun htmlToPlainText(html: String): String =
    Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()

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
