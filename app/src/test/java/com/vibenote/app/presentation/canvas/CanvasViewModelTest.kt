package com.vibenote.app.presentation.canvas

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.vibenote.app.domain.model.Stroke
import com.vibenote.app.domain.repository.NoteRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CanvasViewModel
    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<NoteRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CanvasViewModel(context, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        val state = viewModel.state.value
        assertEquals("Untitled", state.noteTitle)
        assertTrue(state.strokes.isEmpty())
        // Color.White.toArgb() is 0xFFFFFFFF.toInt() which is -1
        assertEquals(-1, state.selectedColor)
    }

    @Test
    fun `finishStroke adds current stroke to list`() {
        val stroke = Stroke(
            points = listOf(Offset(0f, 0f), Offset(10f, 10f)),
            colorValue = 0xFF000000.toInt()
        )
        viewModel.startStroke(stroke)
        viewModel.finishStroke()

        val state = viewModel.state.value
        assertEquals(1, state.strokes.size)
        assertEquals(stroke, state.strokes[0])
    }

    @Test
    fun `undo removes last stroke`() {
        viewModel.startStroke(Stroke(points = listOf(Offset(0f, 0f))))
        viewModel.finishStroke()
        viewModel.startStroke(Stroke(points = listOf(Offset(1f, 1f))))
        viewModel.finishStroke()

        assertEquals(2, viewModel.state.value.strokes.size)

        viewModel.undo()
        assertEquals(1, viewModel.state.value.strokes.size)
        assertTrue(viewModel.state.value.canRedo)
    }
}
