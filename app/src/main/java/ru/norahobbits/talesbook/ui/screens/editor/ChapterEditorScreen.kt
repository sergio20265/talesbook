package ru.norahobbits.talesbook.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Typeface
import android.text.Editable
import android.text.Html
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AlignmentSpan
import android.text.style.CharacterStyle
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.KeyEvent as AndroidKeyEvent
import android.view.inputmethod.EditorInfo
import android.content.Context
import android.widget.EditText
import androidx.core.content.res.ResourcesCompat
import ru.norahobbits.talesbook.R
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    var titleValue by remember(chapter?.id) { mutableStateOf(chapter?.title ?: "") }
    var contentHtml by remember(chapter?.id) { mutableStateOf(chapter?.content ?: "") }
    var contentPlainText by remember(chapter?.id) { mutableStateOf(htmlToPlainText(chapter?.content.orEmpty())) }
    var focusMode by remember { mutableStateOf(false) }
    var localFontSize by remember(appSettings.fontSize) { mutableFloatStateOf(appSettings.fontSize) }
    var editorActions by remember { mutableStateOf<RichEditorActions?>(null) }
    var editorAtBottom by remember { mutableStateOf(false) }
    var editorCanScroll by remember { mutableStateOf(false) }
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

    fun flushEditorToViewModel() {
        editorActions?.flush?.invoke()
    }

    BackHandler {
        scope.launch {
            flushEditorToViewModel()
            viewModel.saveNowAndWait()
            onBack()
        }
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

        // imePadding shrinks the editor to sit above the soft keyboard so the caret/text
        // stays visible while typing. With no soft keyboard (e.g. a tablet's hardware
        // keyboard) the IME inset is 0, so the editor keeps the full screen.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            AnimatedVisibility(
                visible = !focusMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                flushEditorToViewModel()
                                viewModel.saveNowAndWait()
                                onBack()
                            }
                        }) {
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
                            scope.launch {
                                flushEditorToViewModel()
                                viewModel.saveNowAndWait()
                                onOpenChapter(it)
                            }
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
                                onUnderline = { editorActions?.underline?.invoke() },
                                onAlignLeft = { editorActions?.alignLeft?.invoke() },
                                onAlignCenter = { editorActions?.alignCenter?.invoke() },
                                onAlignRight = { editorActions?.alignRight?.invoke() }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            // Only build the editor once the chapter has loaded. The chapter
                            // is fetched asynchronously, so on the first frame it is null and
                            // contentHtml is empty; creating the EditText then (keyed by the
                            // stable route id) left it permanently empty because setText only
                            // runs in the factory. Keying by the loaded id rebuilds it with the
                            // real content the moment it arrives.
                            if (chapter != null) {
                                key(chapter.id) {
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
                                    onScrollStateChanged = { atBottom, canScroll ->
                                        editorAtBottom = atBottom
                                        editorCanScroll = canScroll
                                    },
                                    focusMode = focusMode,
                                    modifier = Modifier.fillMaxSize()
                                )
                                }
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
                    onFontSizeChange = { /* через AppearanceSettings */ },
                    showJump = editorCanScroll,
                    atBottom = editorAtBottom,
                    onJump = {
                        if (editorAtBottom) editorActions?.scrollToTop?.invoke()
                        else editorActions?.scrollToBottom?.invoke()
                    }
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
    onUnderline: () -> Unit,
    onAlignLeft: () -> Unit,
    onAlignCenter: () -> Unit,
    onAlignRight: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
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
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(1.dp)
                .background(colors.textHint.copy(alpha = 0.4f))
        )
        FilledTonalIconButton(onClick = onAlignLeft) {
            Icon(Icons.Default.FormatAlignLeft, contentDescription = "По левому краю", tint = colors.textPrimary)
        }
        FilledTonalIconButton(onClick = onAlignCenter) {
            Icon(Icons.Default.FormatAlignCenter, contentDescription = "По центру", tint = colors.textPrimary)
        }
        FilledTonalIconButton(onClick = onAlignRight) {
            Icon(Icons.Default.FormatAlignRight, contentDescription = "По правому краю", tint = colors.textPrimary)
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
    val underline: () -> Unit,
    val alignLeft: () -> Unit,
    val alignCenter: () -> Unit,
    val alignRight: () -> Unit,
    val scrollToTop: () -> Unit,
    val scrollToBottom: () -> Unit,
    val flush: () -> Unit
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
    onScrollStateChanged: (atBottom: Boolean, canScroll: Boolean) -> Unit,
    focusMode: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalTalesbookColors.current
    val context = LocalContext.current
    var editTextRef by remember { mutableStateOf<EditText?>(null) }
    // Tracks the externally-loaded HTML we have applied to the EditText, so the update
    // block can re-apply when the chapter content loads/changes without clobbering live typing.
    var appliedHtml by remember { mutableStateOf<String?>(null) }

    DisposableEffect(editTextRef) {
        val editText = editTextRef ?: return@DisposableEffect onDispose {}
        // Span-only changes (bold/italic/underline/alignment) do not fire the TextWatcher,
        // so push the current HTML to the model explicitly after each formatting action —
        // otherwise formatting applied without further typing would be lost on save.
        val sync = {
            val src = editText.text ?: SpannableStringBuilder("")
            onContentChanged(spannedToStorageHtml(src), src.toString())
        }
        onActionsReady(
            RichEditorActions(
                bold = { editText.toggleStyle(Typeface.BOLD); sync() },
                italic = { editText.toggleStyle(Typeface.ITALIC); sync() },
                underline = { editText.toggleUnderline(); sync() },
                alignLeft = { editText.setAlignment(Layout.Alignment.ALIGN_NORMAL); sync() },
                alignCenter = { editText.setAlignment(Layout.Alignment.ALIGN_CENTER); sync() },
                alignRight = { editText.setAlignment(Layout.Alignment.ALIGN_OPPOSITE); sync() },
                scrollToTop = {
                    editText.setSelection(0)
                    editText.post {
                        editText.scrollTo(0, 0)
                        editText.reportScroll(onScrollStateChanged)
                    }
                },
                scrollToBottom = {
                    editText.setSelection(editText.text?.length ?: 0)
                    editText.post {
                        editText.scrollTo(0, editText.maxScrollY())
                        editText.reportScroll(onScrollStateChanged)
                    }
                },
                flush = sync
            )
        )
        onDispose { sync() }
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
                typeface = androidTypeface(context, fontFamily)
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
                minLines = 16
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                imeOptions = EditorInfo.IME_ACTION_NONE
                setPadding(18.dp.value.toInt(), 16.dp.value.toInt(), 18.dp.value.toInt(), 16.dp.value.toInt())
                setText(storageHtmlToSpanned(initialHtml))
                appliedHtml = initialHtml
                setSelection(0)
                requestFocus()
                post { reportScroll(onScrollStateChanged) }

                setOnScrollChangeListener { _, _, _, _, _ -> reportScroll(onScrollStateChanged) }
                // Keyboard show/hide and text reflow change the scrollable height, so re-evaluate.
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> post { reportScroll(onScrollStateChanged) } }

                addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                    override fun afterTextChanged(s: Editable?) {
                        onContentChanged(spannedToStorageHtml(s ?: SpannableStringBuilder("")), s?.toString().orEmpty())
                        post { reportScroll(onScrollStateChanged) }
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
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_L -> {
                            setAlignment(Layout.Alignment.ALIGN_NORMAL)
                            true
                        }
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_E -> {
                            setAlignment(Layout.Alignment.ALIGN_CENTER)
                            true
                        }
                        ctrl && keyCode == AndroidKeyEvent.KEYCODE_R -> {
                            setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
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
            editText.typeface = androidTypeface(editText.context, fontFamily)
            // The chapter content may arrive (or change) after the EditText is built — e.g. the
            // async DB load. Re-apply it here, but only when it genuinely differs from what the
            // editor already shows, so we never overwrite text the user is actively typing.
            if (initialHtml != appliedHtml) {
                val current = spannedToStorageHtml(editText.text ?: SpannableStringBuilder(""))
                if (current != initialHtml) {
                    editText.setText(storageHtmlToSpanned(initialHtml))
                    editText.setSelection(editText.text?.length ?: 0)
                }
                appliedHtml = initialHtml
            }
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

/**
 * Aligns whole paragraphs intersecting the current selection (or caret line).
 * [AlignmentSpan] is a paragraph span, so the range is expanded to line boundaries
 * and any previous alignment in that range is cleared first.
 */
/** Maximum vertical scroll offset (0 when the content fits without scrolling). */
private fun EditText.maxScrollY(): Int {
    val l = layout ?: return 0
    val viewport = height - totalPaddingTop - totalPaddingBottom
    return (l.height - viewport).coerceAtLeast(0)
}

/** Reports (atBottom, canScroll) for driving the jump-to-top/bottom control. */
private fun EditText.reportScroll(cb: (atBottom: Boolean, canScroll: Boolean) -> Unit) {
    val max = maxScrollY()
    cb(scrollY >= max - 2, max > 0)
}

private fun EditText.setAlignment(alignment: Layout.Alignment) {
    val editable = text ?: return
    val len = editable.length
    if (len == 0) return
    val selStart = selectionStart.coerceIn(0, len)
    val selEnd = selectionEnd.coerceIn(0, len)
    var start = minOf(selStart, selEnd)
    var end = maxOf(selStart, selEnd)
    // Expand backward to the start of the paragraph (start of text or just after a newline).
    while (start > 0 && editable[start - 1] != '\n') start--
    // Expand forward past the end of the paragraph, including its trailing newline so the
    // span ends on a paragraph boundary (required by SPAN_PARAGRAPH).
    while (end < len && editable[end] != '\n') end++
    if (end < len) end++
    if (start >= end) return
    editable.getSpans(start, end, AlignmentSpan::class.java).forEach { editable.removeSpan(it) }
    editable.setSpan(AlignmentSpan.Standard(alignment), start, end, Spanned.SPAN_PARAGRAPH)
}

// --- Content (de)serialization -------------------------------------------------------------
//
// We deliberately do NOT use Html.toHtml/fromHtml for paragraph alignment: Android's round-trip
// for AlignmentSpan is unreliable (the alignment renders in the EditText but is dropped on
// toHtml on some devices, so it was lost on reload). Instead we serialize/parse alignment
// ourselves as `<p style="text-align:...">` and only lean on the framework for inline marks
// (bold/italic/underline) within each paragraph, which round-trip reliably.

private val blockRegex = Regex(
    "<(p|div|h[1-6]|blockquote)\\b([^>]*)>(.*?)</\\1>",
    setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
)
private val textAlignRegex =
    Regex("text-align\\s*:\\s*(left|right|center|start|end)", RegexOption.IGNORE_CASE)

private fun parseAlignment(attrs: String): Layout.Alignment? {
    val m = textAlignRegex.find(attrs) ?: return null
    return when (m.groupValues[1].lowercase()) {
        "center" -> Layout.Alignment.ALIGN_CENTER
        "right", "end" -> Layout.Alignment.ALIGN_OPPOSITE
        else -> Layout.Alignment.ALIGN_NORMAL
    }
}

/** Parses stored content (our `<p style=…>` HTML, or legacy/markdown) into a styled [Spanned]. */
private fun storageHtmlToSpanned(html: String): Spanned {
    val normalized = normalizeToHtml(html)
    val blocks = blockRegex.findAll(normalized).toList()
    if (blocks.isEmpty()) return Html.fromHtml(normalized, Html.FROM_HTML_MODE_LEGACY)
    val out = SpannableStringBuilder()
    // Collect alignment ranges and apply them only after the whole text is assembled. Applying
    // a SPAN_PARAGRAPH span and then appending more text would let that span grow into the
    // following paragraphs (insert-at-boundary expansion), which made one aligned paragraph
    // turn the rest of the chapter the same way on reload.
    val alignRanges = ArrayList<Triple<Int, Int, Layout.Alignment>>()
    blocks.forEachIndexed { index, m ->
        val tag = m.groupValues[1].lowercase()
        val attrs = m.groupValues[2]
        val inner = m.groupValues[3]
        val align = parseAlignment(attrs)
        // Render inner marks via the framework (keeping the tag so headings keep their look),
        // then strip any framework-applied alignment so ours stays authoritative.
        val rendered = SpannableStringBuilder(
            Html.fromHtml("<$tag>$inner</$tag>", Html.FROM_HTML_MODE_LEGACY)
        )
        while (rendered.isNotEmpty() && rendered.last() == '\n') rendered.delete(rendered.length - 1, rendered.length)
        while (rendered.isNotEmpty() && rendered.first() == '\n') rendered.delete(0, 1)
        rendered.getSpans(0, rendered.length, AlignmentSpan::class.java).forEach { rendered.removeSpan(it) }
        val start = out.length
        out.append(rendered)
        if (index < blocks.size - 1) out.append("\n")
        if (align != null && align != Layout.Alignment.ALIGN_NORMAL && out.length > start) {
            alignRanges.add(Triple(start, out.length, align))
        }
    }
    for ((start, end, align) in alignRanges) {
        out.setSpan(AlignmentSpan.Standard(align), start, end, Spanned.SPAN_PARAGRAPH)
    }
    return out
}

/** Serializes the editor's [Spanned] to storage HTML, encoding per-paragraph alignment. */
private fun spannedToStorageHtml(text: Spanned): String {
    val len = text.length
    val sb = StringBuilder()
    var p = 0
    while (true) {
        var nl = p
        while (nl < len && text[nl] != '\n') nl++
        val align = text.getSpans(p, maxOf(p, nl), AlignmentSpan::class.java).lastOrNull()?.alignment
        val style = when (align) {
            Layout.Alignment.ALIGN_CENTER -> " style=\"text-align:center\""
            Layout.Alignment.ALIGN_OPPOSITE -> " style=\"text-align:right\""
            else -> ""
        }
        sb.append("<p").append(style).append(">")
            .append(inlineToHtml(text, p, nl))
            .append("</p>")
        if (nl >= len) break
        p = nl + 1
    }
    return sb.toString()
}

/** Serializes bold/italic/underline runs within a single paragraph range. */
private fun inlineToHtml(text: Spanned, start: Int, end: Int): String {
    if (end <= start) return ""
    val sb = StringBuilder()
    var i = start
    while (i < end) {
        val next = text.nextSpanTransition(i, end, CharacterStyle::class.java)
        var bold = false
        var italic = false
        var underline = false
        for (s in text.getSpans(i, next, CharacterStyle::class.java)) {
            when (s) {
                is StyleSpan -> {
                    if (s.style and Typeface.BOLD != 0) bold = true
                    if (s.style and Typeface.ITALIC != 0) italic = true
                }
                is UnderlineSpan -> underline = true
            }
        }
        if (bold) sb.append("<b>")
        if (italic) sb.append("<i>")
        if (underline) sb.append("<u>")
        sb.append(escapeForHtml(text.subSequence(i, next).toString()))
        if (underline) sb.append("</u>")
        if (italic) sb.append("</i>")
        if (bold) sb.append("</b>")
        i = next
    }
    return sb.toString()
}

private fun escapeForHtml(s: String): String = buildString {
    for (c in s) when (c) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '\n' -> append("<br>")
        else -> append(c)
    }
}

private fun androidTypeface(context: Context, fontFamily: String): Typeface = when (fontFamily) {
    "serif" -> Typeface.SERIF
    "monospace" -> Typeface.MONOSPACE
    // Bundled handwriting font with Cyrillic glyphs — the system "casual"/"cursive"
    // families are unreliable and Latin-only, so Russian text fell back to default.
    "cursive" -> ResourcesCompat.getFont(context, R.font.caveat) ?: Typeface.SANS_SERIF
    else -> Typeface.SANS_SERIF
}

private fun htmlToPlainText(html: String): String =
    Html.fromHtml(normalizeToHtml(html), Html.FROM_HTML_MODE_LEGACY).toString()

private val htmlTagRegex =
    Regex("</?(p|br|b|i|u|em|strong|span|div|h[1-6]|ul|ol|li|blockquote)\\b", RegexOption.IGNORE_CASE)

private fun looksLikeHtml(s: String): Boolean = htmlTagRegex.containsMatchIn(s)

/**
 * Chapters created with the old markdown/plain-text editor were stored as raw text.
 * Feeding that straight into [Html.fromHtml] mangled it (any `<` swallowed the rest,
 * line breaks collapsed) and re-saving overwrote the original. Convert legacy text to
 * HTML — interpreting common markdown (headings, bold, italic) and preserving
 * paragraphs/line breaks — before loading. Content that already looks like HTML is
 * returned untouched so the conversion only ever runs once per chapter.
 */
private fun normalizeToHtml(raw: String): String {
    if (raw.isBlank() || looksLikeHtml(raw)) return raw
    return raw
        .replace("\r\n", "\n")
        .split(Regex("\\n[ \\t]*\\n+"))
        .map { it.trim('\n') }
        .filter { it.isNotBlank() }
        .joinToString("") { block -> renderMarkdownBlock(block) }
        .ifBlank { "<p>${inlineMarkdown(raw)}</p>" }
}

private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")

private fun renderMarkdownBlock(block: String): String {
    val heading = headingRegex.find(block)
    if (heading != null && !heading.groupValues[2].contains('\n')) {
        val level = heading.groupValues[1].length
        return "<h$level>${inlineMarkdown(heading.groupValues[2].trim())}</h$level>"
    }
    val body = block.split("\n").joinToString("<br>") { inlineMarkdown(it) }
    return "<p>$body</p>"
}

/** Escapes HTML entities, then applies inline markdown (bold before italic). */
private fun inlineMarkdown(line: String): String {
    var s = line
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    s = Regex("\\*\\*(.+?)\\*\\*").replace(s) { "<b>${it.groupValues[1]}</b>" }
    s = Regex("__(.+?)__").replace(s) { "<b>${it.groupValues[1]}</b>" }
    s = Regex("(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])").replace(s) { "<i>${it.groupValues[1]}</i>" }
    s = Regex("(?<![_\\w])_(?!\\s)(.+?)(?<!\\s)_(?![_\\w])").replace(s) { "<i>${it.groupValues[1]}</i>" }
    return s
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
    onFontSizeChange: (Float) -> Unit,
    showJump: Boolean,
    atBottom: Boolean,
    onJump: () -> Unit
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
            if (showJump) {
                IconButton(onClick = onJump) {
                    Icon(
                        if (atBottom) Icons.Default.KeyboardDoubleArrowUp
                        else Icons.Default.KeyboardDoubleArrowDown,
                        contentDescription = if (atBottom) "В начало главы" else "В конец главы",
                        tint = colors.accent
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
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
