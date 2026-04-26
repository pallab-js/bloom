package com.vibenote.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.vibenote.app.core.theme.LocalVibeColors
import com.vibenote.app.core.theme.VibeColors

@Composable
fun NewNoteDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Note",
                color = LocalVibeColors.current.textPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LocalVibeColors.current.textPrimary,
                    unfocusedTextColor = LocalVibeColors.current.textPrimary,
                    focusedBorderColor = LocalVibeColors.current.brand,
                    unfocusedBorderColor = LocalVibeColors.current.borderStandard
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title.ifBlank { "Untitled" }) }
            ) {
                Text(
                    text = "Create",
                    color = LocalVibeColors.current.brand
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = LocalVibeColors.current.textMuted
                )
            }
        },
        containerColor = LocalVibeColors.current.background
    )
}