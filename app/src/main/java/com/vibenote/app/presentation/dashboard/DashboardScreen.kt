package com.vibenote.app.presentation.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibenote.app.core.theme.VibeColors
import com.vibenote.app.domain.model.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNoteClick: (String) -> Unit,
    onNewNote: () -> Unit
) {
    val notes: List<Note> by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var selectedNote by remember { mutableStateOf<Note?>(null) }
    var showNoteActions by remember { mutableStateOf(false) }

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
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut())
                            }.using(
                                androidx.compose.animation.SizeTransform(clip = false)
                            )
                        },
                        label = "SearchTransition"
                    ) { isSearching ->
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search notes...", color = VibeColors.TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = VibeColors.TextPrimary,
                                    unfocusedTextColor = VibeColors.TextPrimary,
                                    focusedBorderColor = VibeColors.BrandGreen,
                                    unfocusedBorderColor = VibeColors.BorderStandard
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column {
                                Text(
                                    text = "Bloom",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = VibeColors.TextPrimary
                                )
                                Text(
                                    text = "${notes.size} note${if (notes.size != 1) "s" else ""}",
                                    fontSize = 11.sp,
                                    color = VibeColors.TextMuted,
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
                            Text("Cancel", color = VibeColors.TextMuted)
                        }
                    }
                },
                actions = {
                    if (!showSearch) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort notes",
                                    tint = VibeColors.TextPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Newest first", color = VibeColors.TextPrimary) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.NEWEST_FIRST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest first", color = VibeColors.TextPrimary) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.OLDEST_FIRST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Last modified", color = VibeColors.TextPrimary) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.LAST_MODIFIED)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("A to Z", color = VibeColors.TextPrimary) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.A_TO_Z)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Z to A", color = VibeColors.TextPrimary) },
                                    onClick = {
                                        viewModel.setSortOrder(SortOrder.Z_TO_A)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = VibeColors.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VibeColors.BackgroundDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = VibeColors.BrandGreen
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Note",
                    tint = VibeColors.SurfaceDeep
                )
            }
        },
        containerColor = VibeColors.BackgroundDark
    ) { padding ->
        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "✎✨",
                        fontSize = 80.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = if (isSearchActive) "No notes match \"$searchQuery\"" else "Your garden is empty",
                        color = VibeColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isSearchActive) "Try a different search term" else "Start blooming by creating your first note",
                        color = VibeColors.TextMuted,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontFamily = FontFamily.Monospace
                    )
                    if (!isSearchActive) {
                        Spacer(Modifier.height(32.dp))
                        androidx.compose.material3.Button(
                            onClick = onNewNote,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = VibeColors.BrandGreen,
                                contentColor = VibeColors.SurfaceDeep
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Blooming", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
items(notes) { note: Note ->
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

    // Delete confirmation dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
title = { Text("Delete Note", color = VibeColors.TextPrimary) },
            text = { Text("Delete \"${note.title}\"?", color = VibeColors.TextMuted) },
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
                    Text("Cancel", color = VibeColors.TextMuted)
                }
            },
            containerColor = VibeColors.BackgroundDark
        )
    }

    // Note actions bottom sheet
    if (showNoteActions && selectedNote != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showNoteActions = false
                selectedNote = null
            },
            containerColor = VibeColors.SurfaceDeep
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedNote!!.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = VibeColors.TextPrimary,
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
                        text = if (selectedNote!!.isFavorite) "Remove from favorites" else "Add to favorites",
                        color = VibeColors.TextPrimary,
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
                        color = VibeColors.TextPrimary,
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
                        color = VibeColors.TextMuted,
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
            containerColor = VibeColors.SurfaceDeep
        ),
        border = BorderStroke(1.5.dp, VibeColors.BorderStandard.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = note.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = VibeColors.TextPrimary
            )
            Text(
                text = formatDate(note.updatedAt),
                fontSize = 12.sp,
                color = VibeColors.TextMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}