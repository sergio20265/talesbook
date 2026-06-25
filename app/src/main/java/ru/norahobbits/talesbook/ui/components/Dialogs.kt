package ru.norahobbits.talesbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.norahobbits.talesbook.ui.theme.LocalTalesbookColors

@Composable
fun CreateBookDialog(
    initialTitle: String = "",
    initialDescription: String = "",
    confirmLabel: String = "Создать",
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    val colors = LocalTalesbookColors.current
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(colors.surface, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Новая история",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название", color = colors.textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.accentSoft,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Короткое описание", color = colors.textSecondary) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.accentSoft,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = colors.textSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) onCreate(title.trim(), description.trim())
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}

@Composable
fun CreateChapterDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    val colors = LocalTalesbookColors.current
    var title by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(colors.surface, RoundedCornerShape(24.dp))
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Новая глава",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название главы", color = colors.textSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.accentSoft,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = colors.textSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (title.isNotBlank()) onCreate(title.trim()) },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Добавить")
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTalesbookColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text("Удалить?", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Удалить", color = colors.danger)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = colors.textSecondary)
            }
        }
    )
}
