package com.vibenote.app.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibenote.app.core.theme.LocalVibeColors
import com.vibenote.app.domain.model.Note
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit,
    onNewNote: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val notes: List<Note> by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showNoteActions by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val isSearchActive = searchQuery.isNotBlank()
    val isEmpty = notes.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.animation.AnimatedContent(
                        targetState = showSearch,
                        transitionSpec = {
                            if (targetState) {
                                fadeIn() togetherWith fadeOut()
                            } else {
                                fadeIn() togetherWith fadeOut()
                            }
                        }, label = ""
                    ) { isSearching ->
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search notes...", color = LocalVibeColors.current.textMuted) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = LocalVibeColors.current.textPrimary,
                                    unfocusedTextColor = LocalVibeColors.current.textPrimary,
                                    focusedBorderColor = LocalVibeColors.current.brand,
                                    unfocusedBorderColor = LocalVibeColors.current.borderStandard,
                                    focusedContainerColor = LocalVibeColors.current.surface,
                                    unfocusedContainerColor = LocalVibeColors.current.surface
                                ),
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Add, contentDescription = "Clear", modifier = Modifier.size(20.dp).rotate(45f))
                                        }
                                    }
                                }
                            )
                        } else {
                            Column {
                                Text(
                                    text = "LIBRARY",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = LocalVibeColors.current.textPrimary
                                )
                                Text(
                                    text = "${notes.size} note${if (notes.size != 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = LocalVibeColors.current.textMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (showSearch) {
                        TextButton(onClick = { 
                            showSearch = false
                            viewModel.setSearchQuery("")
                        }) {
                            Text("Cancel", color = LocalVibeColors.current.textMuted)
                        }
                    } else {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = LocalVibeColors.current.textPrimary)
                        }
                    }
                },
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = onToggleTheme) {
                            Icon(
                                imageVector = if (LocalVibeColors.current.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = LocalVibeColors.current.textPrimary
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort notes",
                                    tint = LocalVibeColors.current.textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.name.replace("_", " ").lowercase().capitalize()) },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (sortOrder == order) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = LocalVibeColors.current.textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LocalVibeColors.current.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = LocalVibeColors.current.brand,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        },
        containerColor = LocalVibeColors.current.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isEmpty) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSearchActive) "No notes found matching your search" else "Your library is empty",
                        color = LocalVibeColors.current.textMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onLongClick = {
                                selectedNote = note
                                showNoteActions = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note", color = LocalVibeColors.current.textPrimary) },
            text = { Text("Are you sure you want to delete '${noteToDelete?.title}'?", color = LocalVibeColors.current.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteNote(noteToDelete!!)
                    noteToDelete = null
                }) {
                    Text("Delete", color = Color(0xFFFF6B6B))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancel", color = LocalVibeColors.current.textMuted)
                }
            },
            containerColor = LocalVibeColors.current.surface
        )
    }

    if (showNoteActions && selectedNote != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showNoteActions = false
                selectedNote = null
            },
            containerColor = LocalVibeColors.current.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = LocalVibeColors.current.borderStandard) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedNote?.title ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalVibeColors.current.textPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                TextButton(
                    onClick = {
                        viewModel.toggleFavorite(selectedNote!!)
                        showNoteActions = false
                        selectedNote = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedNote?.isFavorite == true) "Remove from Favorites" else "Add to Favorites",
                        color = LocalVibeColors.current.textPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                TextButton(
                    onClick = {
                        viewModel.duplicateNote(selectedNote!!)
                        showNoteActions = false
                        selectedNote = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Duplicate",
                        color = LocalVibeColors.current.textPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                TextButton(
                    onClick = {
                        noteToDelete = selectedNote
                        showNoteActions = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete",
                        color = LocalVibeColors.current.textMuted,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = LocalVibeColors.current.surface
        ),
        border = BorderStroke(1.5.dp, LocalVibeColors.current.borderStandard.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
        ) {
            com.vibenote.app.presentation.dashboard.components.CanvasPreview(
                strokes = note.strokes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LocalVibeColors.current.background.copy(alpha = 0.3f))
                    .padding(8.dp)
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = note.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LocalVibeColors.current.textPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = formatDate(note.updatedAt),
                fontSize = 11.sp,
                color = LocalVibeColors.current.textMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
