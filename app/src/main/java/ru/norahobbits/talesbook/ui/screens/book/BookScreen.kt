package ru.norahobbits.talesbook.ui.screens.book

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ru.norahobbits.talesbook.data.model.Chapter
import ru.norahobbits.talesbook.settings.AppSettings
import ru.norahobbits.talesbook.settings.ContentViewMode
import ru.norahobbits.talesbook.ui.components.ConfirmDeleteDialog
import ru.norahobbits.talesbook.ui.components.CreateChapterDialog
import ru.norahobbits.talesbook.ui.components.CreateBookDialog
import ru.norahobbits.talesbook.ui.components.WindowSizeClass
import ru.norahobbits.talesbook.ui.components.contentMaxWidth
import ru.norahobbits.talesbook.ui.components.rememberWindowSizeClass
import ru.norahobbits.talesbook.ui.theme.LocalTalesbookColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookScreen(
    bookId: Long,
    appSettings: AppSettings,
    onOpenChapter: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookViewModel = hiltViewModel()
) {
    val colors = LocalTalesbookColors.current
    val windowSize = rememberWindowSizeClass()
    val book by viewModel.book.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val stats by viewModel.stats.collectAsState()

    var showCreateChapter by remember { mutableStateOf(false) }
    var chapterToDelete by remember { mutableStateOf<Chapter?>(null) }
    var chapterToRename by remember { mutableStateOf<Chapter?>(null) }
    var chapterForBackground by remember { mutableStateOf<Chapter?>(null) }
    var showEditBook by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val coverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateCover(it.toString())
        }
    }

    val bookBgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.updateBackground(it.toString())
        }
    }

    val chapterBgLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            chapterForBackground?.let { ch ->
                viewModel.updateChapterBackground(ch, it.toString())
            }
            chapterForBackground = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val effectiveBackgroundUri = book?.backgroundImageUri ?: appSettings.appBackgroundImageUri

        // Book or shared app background
        effectiveBackgroundUri?.let { bgUri ->
            AsyncImage(
                model = bgUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = if (book?.backgroundImageUri != null) 0.18f else 0.22f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = if (book?.backgroundImageUri != null) 0.78f else 0.74f))
            )
        }

        Scaffold(
            containerColor = if (effectiveBackgroundUri != null) Color.Transparent else colors.background,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = colors.accent)
                        }
                    },
                    title = {
                        Text(
                            book?.title ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        IconButton(onClick = { bookBgLauncher.launch(arrayOf("image/*")) }) {
                            Icon(Icons.Default.Wallpaper, contentDescription = "Фон книги", tint = colors.accentSoft)
                        }
                        if (book?.backgroundImageUri != null) {
                            IconButton(onClick = { viewModel.updateBackground(null) }) {
                                Icon(Icons.Default.HideImage, contentDescription = "Убрать фон", tint = colors.accent)
                            }
                        }
                        IconButton(onClick = { showEditBook = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = colors.accent)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Tune, contentDescription = "Оформление", tint = colors.accent)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = colors.textPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showCreateChapter = true },
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить главу")
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .then(
                        if (windowSize == WindowSizeClass.Compact) Modifier
                        else Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = windowSize.contentMaxWidth())
                    ),
                contentPadding = PaddingValues(
                    horizontal = if (windowSize == WindowSizeClass.Compact) 16.dp else 24.dp,
                    vertical = if (windowSize == WindowSizeClass.Compact) 8.dp else 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    BookHeader(
                        coverUri = book?.coverImageUri,
                        description = book?.description ?: "",
                        stats = stats,
                        onChangeCover = { coverLauncher.launch(arrayOf("image/*")) }
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Главы",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (chapters.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Тихое место для слов.\nДобавьте первую главу.",
                                color = colors.textHint,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (appSettings.chapterViewMode == ContentViewMode.LIST) {
                    itemsIndexed(chapters, key = { _, ch -> ch.id }) { _, chapter ->
                        ChapterItem(
                            chapter = chapter,
                            onClick = { onOpenChapter(chapter.id) },
                            onDelete = { chapterToDelete = chapter },
                            onRename = { chapterToRename = chapter },
                            onChangeBackground = {
                                chapterForBackground = chapter
                                chapterBgLauncher.launch(arrayOf("image/*"))
                            },
                            onClearBackground = {
                                viewModel.updateChapterBackground(chapter, null)
                            }
                        )
                    }
                } else {
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chapters.forEach { chapter ->
                                ChapterItem(
                                    chapter = chapter,
                                    modifier = if (windowSize == WindowSizeClass.Compact) {
                                        Modifier.fillMaxWidth()
                                    } else {
                                        Modifier.width(280.dp)
                                    },
                                    onClick = { onOpenChapter(chapter.id) },
                                    onDelete = { chapterToDelete = chapter },
                                    onRename = { chapterToRename = chapter },
                                    onChangeBackground = {
                                        chapterForBackground = chapter
                                        chapterBgLauncher.launch(arrayOf("image/*"))
                                    },
                                    onClearBackground = {
                                        viewModel.updateChapterBackground(chapter, null)
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreateChapter) {
        CreateChapterDialog(
            onDismiss = { showCreateChapter = false },
            onCreate = { title ->
                viewModel.createChapter(title)
                showCreateChapter = false
            }
        )
    }

    chapterToDelete?.let { chapter ->
        ConfirmDeleteDialog(
            text = "Удалить главу «${chapter.title}»?",
            onConfirm = { viewModel.deleteChapter(chapter); chapterToDelete = null },
            onDismiss = { chapterToDelete = null }
        )
    }

    chapterToRename?.let { chapter ->
        CreateChapterDialog(
            onDismiss = { chapterToRename = null },
            onCreate = { newTitle ->
                viewModel.renameChapter(chapter, newTitle)
                chapterToRename = null
            }
        )
    }

    if (showEditBook) {
        book?.let { b ->
            CreateBookDialog(
                initialTitle = b.title,
                initialDescription = b.description,
                confirmLabel = "Сохранить",
                onDismiss = { showEditBook = false },
                onCreate = { newTitle, newDescription ->
                    viewModel.updateBook(b.copy(title = newTitle, description = newDescription))
                    showEditBook = false
                }
            )
        }
    }
}

@Composable
private fun BookHeader(
    coverUri: String?,
    description: String,
    stats: BookStats,
    onChangeCover: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceSoft),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(110.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onChangeCover)
                .background(colors.surface)
        ) {
            if (coverUri != null) {
                AsyncImage(
                    model = coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = colors.accentSoft, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Обложка", color = colors.accentSoft, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (stats.words > 0 || stats.charsWithSpaces > 0) {
                Text(
                    "${stats.words} слов · ${stats.charsWithSpaces} зн · ${"%.2f".format(stats.authorSheets)} а. л.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentSoft
                )
                Text(
                    "${stats.charsWithoutSpaces} знаков без пробелов",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textHint
                )
            }
            TextButton(
                onClick = onChangeCover,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Заменить обложку", color = colors.accent)
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: Chapter,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onChangeBackground: () -> Unit,
    onClearBackground: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            if (chapter.backgroundImageUri != null) {
                AsyncImage(
                    model = chapter.backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    alpha = 0.14f
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = colors.accentSoft,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chapter.wordCount > 0) {
                        Text(
                            "${chapter.wordCount} слов · ${chapter.charCountWithSpaces} зн · ${"%.2f".format(chapter.charCountWithSpaces / 40_000.0)} а. л.",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textHint
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = colors.textHint)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(colors.surfaceSoft)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Переименовать", color = colors.textPrimary) },
                            onClick = { showMenu = false; onRename() },
                            leadingIcon = { Icon(Icons.Default.Edit, null, tint = colors.accent) }
                        )
                        DropdownMenuItem(
                            text = { Text("Фон главы", color = colors.textPrimary) },
                            onClick = { showMenu = false; onChangeBackground() },
                            leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = colors.accent) }
                        )
                        if (chapter.backgroundImageUri != null) {
                            DropdownMenuItem(
                                text = { Text("Убрать фон", color = colors.textSecondary) },
                                onClick = { showMenu = false; onClearBackground() },
                                leadingIcon = { Icon(Icons.Default.HideImage, null, tint = colors.textHint) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Удалить", color = colors.danger) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = colors.danger) }
                        )
                    }
                }
            }
        }
    }
}
