package ru.norahobbits.talesbook.ui.screens.library

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import ru.norahobbits.talesbook.data.model.Book
import ru.norahobbits.talesbook.settings.AppSettings
import ru.norahobbits.talesbook.settings.ContentViewMode
import ru.norahobbits.talesbook.ui.components.WindowSizeClass
import ru.norahobbits.talesbook.ui.components.ConfirmDeleteDialog
import ru.norahobbits.talesbook.ui.components.CreateBookDialog
import ru.norahobbits.talesbook.ui.components.contentMaxWidth
import ru.norahobbits.talesbook.ui.components.rememberWindowSizeClass
import ru.norahobbits.talesbook.ui.theme.LocalTalesbookColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    appSettings: AppSettings,
    onOpenBook: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val colors = LocalTalesbookColors.current
    val windowSize = rememberWindowSizeClass()
    val books by viewModel.books.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var editingBook by remember { mutableStateOf<Book?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        appSettings.appBackgroundImageUri?.let { bgUri ->
            AsyncImage(
                model = bgUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.22f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = 0.74f))
            )
        }

    Scaffold(
        containerColor = if (appSettings.appBackgroundImageUri != null) Color.Transparent else colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Моя библиотека",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Оформление",
                            tint = colors.accent
                        )
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
                onClick = { showCreateDialog = true },
                containerColor = colors.accent,
                contentColor = colors.background,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Новая история")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (books.isEmpty()) {
                EmptyLibrary(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = if (windowSize == WindowSizeClass.Compact) 360.dp else 520.dp),
                    onCreateBook = { showCreateDialog = true }
                )
            } else if (appSettings.bookViewMode == ContentViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (windowSize == WindowSizeClass.Compact) Modifier
                            else Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = windowSize.contentMaxWidth())
                        ),
                    contentPadding = PaddingValues(
                        horizontal = if (windowSize == WindowSizeClass.Compact) 16.dp else 24.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookTile(
                            book = book,
                            onClick = { onOpenBook(book.id) },
                            onDelete = { bookToDelete = book },
                            onEdit = { editingBook = book }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (windowSize == WindowSizeClass.Compact) 240.dp else 300.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = if (windowSize == WindowSizeClass.Compact) Dp.Unspecified else windowSize.contentMaxWidth())
                        .align(Alignment.TopCenter),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onClick = { onOpenBook(book.id) },
                            onDelete = { bookToDelete = book },
                            onEdit = { editingBook = book }
                        )
                    }
                }
            }
        }
    }
    }

    if (showCreateDialog) {
        CreateBookDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description ->
                viewModel.createBook(title, description)
                showCreateDialog = false
            }
        )
    }

    bookToDelete?.let { book ->
        ConfirmDeleteDialog(
            text = "Удалить книгу «${book.title}»? Все главы будут удалены.",
            onConfirm = {
                viewModel.deleteBook(book)
                bookToDelete = null
            },
            onDismiss = { bookToDelete = null }
        )
    }

    editingBook?.let { book ->
        CreateBookDialog(
            initialTitle = book.title,
            initialDescription = book.description,
            confirmLabel = "Сохранить",
            onDismiss = { editingBook = null },
            onCreate = { title, description ->
                viewModel.updateBook(book.copy(title = title, description = description))
                editingBook = null
            }
        )
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier, onCreateBook: () -> Unit) {
    val colors = LocalTalesbookColors.current
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Outlined.AutoStories,
            contentDescription = null,
            tint = colors.accentSoft,
            modifier = Modifier.size(64.dp)
        )
        Text(
            "Здесь пока пусто,\nно история уже рядом",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        TextButton(onClick = onCreateBook) {
            Text("Начать новую историю", color = colors.accent)
        }
    }
}

@Composable
private fun BookTile(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale("ru")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .background(colors.surfaceSoft)
            ) {
                if (book.coverImageUri != null) {
                    AsyncImage(
                        model = book.coverImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = colors.accentSoft,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    colors.background.copy(alpha = 0.42f)
                                )
                            )
                        )
                )
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = colors.textPrimary)
                    }
                    BookMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.description.isNotBlank()) {
                    Text(
                        text = book.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = dateFormat.format(Date(book.updatedAt)),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textHint
                )
            }
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale("ru")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // Book background behind card content
            if (book.backgroundImageUri != null) {
                AsyncImage(
                    model = book.backgroundImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                    alpha = 0.12f
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Обложка
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                        .background(colors.surfaceSoft)
                        .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                ) {
                    if (book.coverImageUri != null) {
                        AsyncImage(
                            model = book.coverImageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Outlined.AutoStories,
                            contentDescription = null,
                            tint = colors.accentSoft,
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.Center)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        colors.surface.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )
                }

                // Контент
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = dateFormat.format(Date(book.updatedAt)),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textHint
                    )
                }

                // Меню
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = colors.textHint)
                    }
                    BookMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun BookMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(colors.surfaceSoft)
    ) {
        DropdownMenuItem(
            text = { Text("Редактировать", color = colors.textPrimary) },
            onClick = { onDismiss(); onEdit() },
            leadingIcon = {
                Icon(Icons.Default.Edit, null, tint = colors.accent)
            }
        )
        DropdownMenuItem(
            text = { Text("Удалить", color = colors.danger) },
            onClick = { onDismiss(); onDelete() },
            leadingIcon = {
                Icon(Icons.Default.Delete, null, tint = colors.danger)
            }
        )
    }
}
