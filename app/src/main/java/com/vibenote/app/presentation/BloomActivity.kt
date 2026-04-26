package com.vibenote.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.vibenote.app.core.theme.VibeColors
import com.vibenote.app.core.theme.VibeNoteTheme
import com.vibenote.app.domain.model.Note
import com.vibenote.app.domain.repository.NoteRepository
import com.vibenote.app.presentation.canvas.CanvasScreen
import com.vibenote.app.presentation.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class BloomActivity : ComponentActivity() {

    @Inject
    lateinit var noteRepository: NoteRepository

    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VibeNoteTheme {
                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VibeColors.BackgroundDark
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard"
                    ) {
                        composable("dashboard") {
                            var showNewNoteDialog by remember { mutableStateOf(false) }
                            var pendingNavigateTo by remember { mutableStateOf<String?>(null) }

                            DashboardScreen(
                                onNoteClick = { noteId ->
                                    navController.navigate("canvas/$noteId")
                                },
                                onNewNote = {
                                    showNewNoteDialog = true
                                }
                            )

                            if (showNewNoteDialog) {
                                NewNoteDialog(
                                    onDismiss = { showNewNoteDialog = false },
                                    onCreate = { title ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            val note = Note(
                                                id = UUID.randomUUID().toString(),
                                                title = title.ifBlank { "Untitled" }
                                            )
                                            noteRepository.insertNote(note)
                                            showNewNoteDialog = false
                                            navController.navigate("canvas/${note.id}")
                                        }
                                    }
                                )
                            }
                        }

                        composable(
                            route = "canvas/{noteId}",
                            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getString("noteId") ?: "new"
                            CanvasScreen(
                                noteId = noteId,
                                noteTitle = "",
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}