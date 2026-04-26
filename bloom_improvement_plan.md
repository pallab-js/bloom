# Bloom — Comprehensive Improvement Plan for OpenCode AI

> **Project:** `https://github.com/pallab-js/bloom.git`
> **Stack:** Kotlin · Jetpack Compose · MVVM · Hilt · Room · Coroutines/Flow · Gson
> **Target:** Android API 24–34 · Local-only, no network, no cloud, no AI/ML, no Docker

---

## HOW TO READ THIS DOCUMENT

Every section is self-contained. Each item contains:
- **What:** exact symptom / issue
- **Where:** exact file + line context
- **Fix:** concrete implementation steps or code

Items are grouped by category and prioritized within each group (`P1 = critical, P2 = important, P3 = nice-to-have`).

---

## 1. CRITICAL BUGS

### BUG-01 · `duplicateNote` loses all strokes [P1]

**What:** Duplicating a note from the Dashboard creates a new note DB entry with a new UUID, but never copies the strokes JSON file on disk. Opening the duplicate always shows a blank canvas.

**Where:** `DashboardViewModel.kt` → `duplicateNote()`

```kotlin
// CURRENT (broken)
fun duplicateNote(note: Note) {
    viewModelScope.launch {
        val copy = note.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${note.title} (copy)",
            ...
        )
        noteRepository.insertNote(copy)   // strokes_<oldId>.json never copied
    }
}
```

**Fix:** Inject `@ApplicationContext context: Context` into `DashboardViewModel` and copy the file:

```kotlin
fun duplicateNote(note: Note) {
    viewModelScope.launch(Dispatchers.IO) {
        val newId = java.util.UUID.randomUUID().toString()
        val copy = note.copy(
            id = newId,
            title = "${note.title} (copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteRepository.insertNote(copy)
        val src = File(context.filesDir, "strokes_${note.id}.json")
        val dst = File(context.filesDir, "strokes_$newId.json")
        if (src.exists()) src.copyTo(dst, overwrite = true)
    }
}
```

---

### BUG-02 · Dashboard `deleteNote` leaks stroke files [P1]

**What:** `DashboardViewModel.deleteNote()` removes the DB row but never deletes the `strokes_<id>.json` file from internal storage. Over time this silently fills user storage.

**Where:** `DashboardViewModel.kt` → `deleteNote()`

```kotlin
// CURRENT (leaks file)
fun deleteNote(note: Note) {
    viewModelScope.launch {
        noteRepository.deleteNote(note)  // file strokes_<id>.json never deleted
    }
}
```

**Fix:**

```kotlin
fun deleteNote(note: Note) {
    viewModelScope.launch(Dispatchers.IO) {
        noteRepository.deleteNote(note)
        val strokesFile = File(context.filesDir, "strokes_${note.id}.json")
        if (strokesFile.exists()) strokesFile.delete()
    }
}
```

---

### BUG-03 · `saveNow()` is NOT synchronous — data loss on back press [P1]

**What:** `saveNow()` cancels the debounce job and calls `saveStrokesToFile()` which **launches a new coroutine** internally. The `onNavigateBack()` call immediately follows, potentially before the coroutine body executes, causing the last strokes to be lost.

**Where:** `CanvasScreen.kt` → back button handler / `CanvasViewModel.kt` → `saveNow()` + `saveStrokesToFile()`

```kotlin
// CURRENT (race condition)
fun saveNow() {
    saveJob?.cancel()
    saveStrokesToFile()  // launches coroutine; returns immediately
}
private fun saveStrokesToFile() {
    ...
    viewModelScope.launch(Dispatchers.IO) { ... }  // async!
}
```

```kotlin
// CURRENT (screen)
IconButton(onClick = {
    viewModel.saveNow()      // returns before file written
    onNavigateBack()         // navigates away too soon
})
```

**Fix:** Make `saveNow` a `suspend` function and await completion in the UI:

```kotlin
// CanvasViewModel.kt
suspend fun saveNow() {
    saveJob?.cancel()
    val noteId = _state.value.noteId
    if (noteId.isEmpty()) return
    val strokes = _state.value.strokes
    withContext(Dispatchers.IO) {
        val strokesFile = File(context.filesDir, "strokes_$noteId.json")
        strokesFile.writeText(gson.toJson(strokes))
    }
}
```

```kotlin
// CanvasScreen.kt — back button
IconButton(onClick = {
    coroutineScope.launch {
        viewModel.saveNow()
        onNavigateBack()
    }
})
```

Add `val coroutineScope = rememberCoroutineScope()` at top of `CanvasScreen`.

---

### BUG-04 · `deleteNote` in CanvasViewModel uses `insertNote` to update [P1]

**What:** `updateTitle()` calls `noteRepository.insertNote()` (REPLACE strategy) to update the title. Semantically wrong and masks errors. Should call `updateNote()`.

**Where:** `CanvasViewModel.kt` → `updateTitle()`

```kotlin
// CURRENT (wrong method)
noteRepository.insertNote(updatedNote)  // should be updateNote
```

**Fix:** Replace with `noteRepository.updateNote(updatedNote)`.

---

### BUG-05 · Tag search SQL query is broken [P1]

**What:** `NoteDao.getNotesByTag()` uses `LIKE '%' || :tag || '%'`. Tags are stored with `\u001F` (ASCII Unit Separator) as delimiter. A tag search for `"art"` would match notes tagged `"party"` or `"smartnotes"`. The LIKE query ignores the separator entirely.

**Where:** `NoteDao.kt` → `getNotesByTag()` / `NoteRepositoryImpl.kt` → tag serialization with `\u001F`

```kotlin
// CURRENT (broken)
@Query("SELECT * FROM notes WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
fun getNotesByTag(tag: String): Flow<List<NoteEntity>>
```

**Fix:** Use the same separator in the LIKE pattern so only exact tag segments match:

```kotlin
@Query("""
    SELECT * FROM notes 
    WHERE tags = :tag 
       OR tags LIKE :tag || X'1F' || '%'
       OR tags LIKE '%' || X'1F' || :tag || X'1F' || '%'
       OR tags LIKE '%' || X'1F' || :tag
    ORDER BY createdAt DESC
""")
fun getNotesByTag(tag: String): Flow<List<NoteEntity>>
```

Or simpler — switch the tag separator to a visible ASCII character like `|` for both storage and LIKE matching:

```kotlin
// NoteRepositoryImpl.kt
private const val TAG_SEPARATOR = "|"
tags = if (tags.isBlank()) emptyList() else tags.split(TAG_SEPARATOR).filter { it.isNotBlank() }
tags = tags.joinToString(TAG_SEPARATOR)
```

```kotlin
// NoteDao.kt
@Query("SELECT * FROM notes WHERE '|' || tags || '|' LIKE '%|' || :tag || '|%'")
fun getNotesByTag(tag: String): Flow<List<NoteEntity>>
```

Add a new Room DB migration (version 4→5) to migrate existing `\u001F`-separated tags to `|`-separated tags.

---

### BUG-06 · Eraser only fakes erasure — breaks on non-dark backgrounds [P2]

**What:** The eraser paints with `VibeColors.BackgroundDark` color. On white, lined, dotted, or grid backgrounds this leaves a dark smear instead of erasing anything.

**Where:** `CanvasScreen.kt` → draw loop inside `Canvas` composable

```kotlin
// CURRENT (broken on light backgrounds)
val strokeColor = when {
    stroke.isEraser -> VibeColors.BackgroundDark  // hardcoded dark
    ...
}
```

**Fix:** Match eraser color to the current canvas background color:

```kotlin
val bgColor = when (state.canvasBackground) {
    "white" -> Color.White
    else -> VibeColors.BackgroundDark
}

val strokeColor = when {
    stroke.isEraser -> bgColor   // match canvas background
    stroke.isHighlighter -> Color(stroke.colorValue).copy(alpha = 0.4f)
    else -> Color(stroke.colorValue)
}
```

Apply same fix to the `currentPoints` (in-progress stroke) draw section at the bottom of the Canvas composable.

> **Note:** A true eraser requires `BlendMode.Clear` with `CompositingStrategy.Offscreen`. For a complete fix, wrap the canvas with `Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` and use `BlendMode.Clear` on eraser strokes:
```kotlin
drawPath(
    path = path,
    color = Color.Transparent,
    style = DrawStroke(width = strokeWidth, cap = StrokeCap.Round),
    blendMode = BlendMode.Clear
)
```

---

### BUG-07 · `isLinear()` divides by `pathLength` which can be zero [P2]

**What:** For a stroke with a single point or all identical points, `pathLength` = 0. Division produces `NaN`, causing the comparison `distance / pathLength > 0.15f` to always be `false` (`NaN` comparisons return false in Kotlin/JVM), making every trivially short stroke pass as a straight line.

**Where:** `ShapeRecognitionHelper.kt` → `isLinear()`

```kotlin
// CURRENT
if (distance / pathLength > 0.15f) {  // NaN if pathLength == 0
    return false
}
```

**Fix:**

```kotlin
private fun isLinear(points: List<Offset>): Boolean {
    if (points.size < 3) return true
    val first = points.first()
    val last = points.last()
    val pathLength = calculatePathLength(points)
    if (pathLength < 1f) return true   // degenerate path, treat as point
    for (point in points) {
        val distance = pointToLineDistance(point, first, last)
        if (distance / pathLength > 0.15f) return false
    }
    return true
}
```

---

### BUG-08 · `onDragStart` guard `if (currentPoints.isNotEmpty())` prevents new strokes [P2]

**What:** If a drag sequence ends abnormally (e.g., interrupted by a system gesture) and `currentPoints` is not cleared, all subsequent draw attempts are silently ignored forever until the app restarts.

**Where:** `CanvasScreen.kt` → `detectDragGestures` → `onDragStart`

```kotlin
// CURRENT
onDragStart = { offset ->
    if (currentPoints.isNotEmpty()) return@detectDragGestures  // dead lock
    ...
}
```

**Fix:** Remove the guard and always reset state at `onDragStart`:

```kotlin
onDragStart = { offset ->
    // Always reset — handles interrupted gesture sequences cleanly
    currentPath = Path().apply { moveTo(offset.x, offset.y) }
    currentPoints = listOf(offset)
    val newStroke = Stroke(
        colorValue = state.selectedColor,
        strokeWidth = state.strokeWidth,
        isEraser = state.isEraser,
        isHighlighter = state.isHighlighter
    )
    viewModel.startStroke(newStroke)
},
```

---

### BUG-09 · Two competing `pointerInput` gesture detectors on same Box [P2]

**What:** Both `detectTransformGestures` (pinch/pan) and `detectDragGestures` (drawing) are attached to the same `Box`. These detectors fight over pointer ownership. Single-finger draw can trigger pan; two-finger pan can accidentally start strokes.

**Where:** `CanvasScreen.kt` → Canvas `Box` with two `.pointerInput(Unit)` modifiers

**Fix:** Unify into a single `awaitEachGesture` / `pointerInput` block using `pointerCount` to differentiate:

```kotlin
.pointerInput(Unit) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        
        // Peek at pointer count to decide gesture mode
        val secondDown = withTimeoutOrNull(100) {
            awaitPointerEvent().changes.let {
                if (it.size >= 2) it else null
            }
        }
        
        if (secondDown != null) {
            // Two-finger: handle pan/zoom (transform mode)
            handleTransformGesture(this) { pan, zoom ->
                canvasScale = (canvasScale * zoom).coerceIn(0.5f, 3f)
                canvasOffsetX += pan.x
                canvasOffsetY += pan.y
                viewModel.updateTransform(canvasScale, canvasOffsetX, canvasOffsetY)
            }
        } else {
            // Single-finger: draw
            handleDrawGesture(this, firstDown, state, viewModel) { pts, path ->
                currentPoints = pts
                currentPath = path
            }
        }
    }
}
```

This requires refactoring gesture logic into helper functions. Alternatively, use `Modifier.pointerInput` with `PointerEventPass.Initial` to intercept multi-touch before draw:

```kotlin
// Simpler approach: check pointer count in onDragStart
onDragStart = { offset ->
    // currentEvent is accessible in pointerInput scope
    if (currentEvent.changes.size > 1) return@detectDragGestures
    ...
}
```

---

### BUG-10 · Package name / app ID mismatch [P2]

**What:** Manifest has `applicationId "com.bloom.app"` but all Kotlin source code uses package `com.vibenote.app`. The app label is `"Bloom"` but the Dashboard title hardcodes `"VIBENOTE"`. Causes confusion and could cause build issues with Hilt code generation.

**Where:** `app/build.gradle` → `applicationId` / `DashboardScreen.kt` → Text("VIBENOTE")

**Fix:**
1. In `build.gradle`, change `applicationId "com.bloom.app"` → `"com.bloom.app"` is fine as the public package. But reconcile it: pick one canonical name.
2. In `DashboardScreen.kt`, change:
```kotlin
Text(text = "VIBENOTE", ...)  →  Text(text = "Bloom", ...)
```
3. In `CanvasViewModel.kt` → `exportAsPng()`, change the folder path:
```kotlin
put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Bloom")
```
4. Update the export success dialog text: `"Note exported to Pictures/Bloom"`.

---

## 2. PERFORMANCE OPTIMIZATIONS

### PERF-01 · Stroke points re-parsed from String on EVERY recompose frame [P1]

**What:** In `CanvasScreen.kt`, the `Canvas` composable re-parses every stroke's `points: String` (via `.split(";")`) on every single frame recompose. With 100 strokes this is 100 string-split operations per frame (~60/sec).

**Where:** `CanvasScreen.kt` → `Canvas` composable → `state.strokes.forEach { stroke -> ... stroke.points.split(";") ... }`

**Problem:** `parsedPointsCache` exists in `CanvasViewModel` but is **never called from `CanvasScreen`** — the screen does its own inline parsing.

**Fix:** Use `remember` with a derived state to parse once per unique strokes list:

```kotlin
// In CanvasScreen, above the Canvas composable:
val parsedStrokes = remember(state.strokes) {
    state.strokes.map { stroke ->
        stroke to stroke.points.split(";").mapNotNull { pair ->
            val coords = pair.split(",")
            if (coords.size == 2) {
                runCatching {
                    Offset(coords[0].toFloat(), coords[1].toFloat())
                }.getOrNull()
            } else null
        }
    }
}
```

Then replace `stroke.points.split(";").mapNotNull { ... }` inside the `Canvas` draw block with `parsedStrokes.find { it.first == stroke }?.second ?: emptyList()`.

Or more efficiently, store parsed points in a `SnapshotStateMap`:

```kotlin
val parsedPointsMap = remember { mutableStateMapOf<String, List<Offset>>() }

LaunchedEffect(state.strokes) {
    val currentIds = state.strokes.map { it.hashCode().toString() }.toSet()
    parsedPointsMap.keys.retainAll(currentIds)
    state.strokes.forEach { stroke ->
        val key = stroke.hashCode().toString()
        if (key !in parsedPointsMap) {
            parsedPointsMap[key] = parsePoints(stroke.points)
        }
    }
}
```

---

### PERF-02 · Canvas background redrawn from scratch every frame [P2]

**What:** The `"lined"`, `"dotted"`, and `"grid"` backgrounds use `while` loops that iterate over every line/dot position on every recompose. These loops run 60 times per second even when nothing is changing.

**Where:** `CanvasScreen.kt` → `Canvas` composable → `when (state.canvasBackground)` block

**Fix:** Cache background as a `Picture` or `ImageBitmap` and redraw only when background type or canvas size changes:

```kotlin
// Outside Canvas, inside CanvasScreen:
val backgroundPicture = remember(state.canvasBackground) { mutableStateOf<Picture?>(null) }

Canvas(modifier = ...) {
    val picture = backgroundPicture.value
    if (picture == null || picture.width != size.width.toInt()) {
        val newPicture = Picture()
        val canvas = newPicture.beginRecording(size.width.toInt(), size.height.toInt())
        drawBackground(canvas, state.canvasBackground, size)
        newPicture.endRecording()
        backgroundPicture.value = newPicture
        drawPicture(newPicture)
    } else {
        drawPicture(picture)
    }
    // draw strokes...
}
```

Simpler alternative: use `drawIntoCanvas { nativeCanvas -> ... }` with a cached `android.graphics.Picture`.

---

### PERF-03 · `updateNoteTimestamp()` does DB read+write on every stroke [P2]

**What:** Every time a stroke is finished, `finishStroke()` calls `updateNoteTimestamp()` which does `getNoteById(noteId)` (a DB read) then `updateNote()` (a DB write). For rapid drawing this hammers the DB.

**Where:** `CanvasViewModel.kt` → `updateNoteTimestamp()`

**Fix:** Eliminate the read. Maintain `updatedAt` only in the scheduled save:

```kotlin
// Remove updateNoteTimestamp() call from finishStroke()
fun finishStroke() {
    val currentStroke = _state.value.currentStroke ?: return
    saveToUndoStack()
    _state.update { s ->
        s.copy(strokes = s.strokes + currentStroke, currentStroke = null, canUndo = true, canRedo = false)
    }
    redoStack.clear()
    scheduleSave()  // timestamp update folded into save
}

// In saveStrokesToFile(), also update the timestamp:
private suspend fun saveStrokesToFileInternal() {
    val noteId = _state.value.noteId
    if (noteId.isEmpty()) return
    val strokes = _state.value.strokes
    withContext(Dispatchers.IO) {
        File(context.filesDir, "strokes_$noteId.json")
            .writeText(gson.toJson(strokes))
        // Single DB write, no read needed
        noteRepository.updateNoteTimestamp(noteId, System.currentTimeMillis())
    }
}
```

Add to `NoteRepository` / `NoteDao`:
```kotlin
@Query("UPDATE notes SET updatedAt = :timestamp WHERE id = :id")
suspend fun updateTimestamp(id: String, timestamp: Long)
```

---

### PERF-04 · Full JSON file written on every `scheduleSave` trigger [P2]

**What:** Every stroke completion triggers a 1500ms debounced write of ALL strokes serialized to JSON. For a note with 500 strokes, this writes a potentially large file on each save. Gson serialization also happens on the calling thread before the IO dispatch.

**Where:** `CanvasViewModel.kt` → `saveStrokesToFile()`

**Fix:** Move Gson serialization inside the IO dispatcher and consider a more efficient storage format. Short-term fix:

```kotlin
private fun scheduleSave() {
    saveJob?.cancel()
    saveJob = viewModelScope.launch {
        delay(1500)
        val noteId = _state.value.noteId
        if (noteId.isEmpty()) return@launch
        val snapshot = _state.value.strokes   // capture snapshot on Main
        withContext(Dispatchers.IO) {
            val json = gson.toJson(snapshot)  // serialize on IO thread
            File(context.filesDir, "strokes_$noteId.json").writeText(json)
            noteRepository.updateNoteTimestamp(noteId, System.currentTimeMillis())
        }
    }
}
```

Long-term: replace Gson + string-based point storage with a binary format (e.g., `DataOutputStream` with float pairs) for faster writes and smaller files.

---

### PERF-05 · `scheduleSave` nests two coroutine launches [P2]

**What:** `scheduleSave` launches a coroutine on `Dispatchers.IO`, then `saveStrokesToFile` inside launches *another* coroutine on `Dispatchers.IO`. Double launch overhead; the outer launch `delay` runs on IO (should run on Default or Main).

**Where:** `CanvasViewModel.kt` → `scheduleSave()` + `saveStrokesToFile()`

**Fix:** As shown in PERF-04, unify into a single `launch` without nesting.

---

## 3. CODE QUALITY & ARCHITECTURE

### CODE-01 · `canvasBackground` is an unvalidated magic string [P2]

**What:** Canvas background is stored and compared as a raw `String` ("dark", "white", "lined", "dotted", "grid") throughout DB, ViewModel, and Screen. A typo anywhere silently breaks background rendering.

**Where:** `NoteEntity.kt`, `CanvasState`, `CanvasScreen.kt` → `when (state.canvasBackground)`, `CanvasViewModel.kt`

**Fix:** Introduce a sealed class / enum and a DB type converter:

```kotlin
// domain/model/CanvasBackground.kt
enum class CanvasBackground(val key: String) {
    DARK("dark"), WHITE("white"), LINED("lined"), DOTTED("dotted"), GRID("grid");
    companion object {
        fun fromKey(key: String) = values().firstOrNull { it.key == key } ?: DARK
    }
}
```

```kotlin
// data/local/Converters.kt
class Converters {
    @TypeConverter fun fromBackground(bg: CanvasBackground): String = bg.key
    @TypeConverter fun toBackground(key: String): CanvasBackground = CanvasBackground.fromKey(key)
}
```

Add `@TypeConverters(Converters::class)` to `NoteDatabase`. Update `CanvasState`, `Note`, `NoteEntity` to use `CanvasBackground` type. Update all `when` blocks to switch on enum.

---

### CODE-02 · `Stroke.points` is a serialized String — domain model leak [P2]

**What:** `Stroke` is a domain model but carries `points: String` — a storage-format concern. The string format (`"x,y;x,y"`) leaks persistence details into the domain layer.

**Where:** `domain/model/Stroke.kt`, used throughout `CanvasScreen`, `CanvasViewModel`, `ShapeRecognitionHelper`

**Fix:**

```kotlin
// domain/model/Stroke.kt — clean domain model
data class Stroke(
    val points: List<Offset> = emptyList(),  // native type
    val colorValue: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false,
    val isHighlighter: Boolean = false,
    val strokeType: StrokeType = StrokeType.PEN
)
```

Create a `StrokeDto` for JSON serialization:
```kotlin
// data/local/StrokeDto.kt
data class StrokeDto(
    val points: String,
    val colorValue: Int,
    val strokeWidth: Float,
    val isEraser: Boolean,
    val isHighlighter: Boolean,
    val strokeType: String
)
fun StrokeDto.toDomain() = Stroke(
    points = points.split(";").mapNotNull { ... },
    ...
)
fun Stroke.toDto() = StrokeDto(
    points = points.joinToString(";") { "${it.x},${it.y}" },
    ...
)
```

All parsing is then confined to the data layer.

---

### CODE-03 · `toSmoothedPath()` is a top-level function in `CanvasScreen.kt` [P3]

**What:** `List<Offset>.toSmoothedPath()` is defined as a top-level extension at the top of `CanvasScreen.kt`. Should live in a utility/drawing module since it's also needed in `exportAsPng`.

**Where:** `CanvasScreen.kt` line 1 area

**Fix:** Move to `core/drawing/PathUtils.kt` and import where needed. Also use in `exportAsPng` (currently uses crude `lineTo` instead of quadratic Bézier).

---

### CODE-04 · `CanvasScreen` is a 500+ line God composable [P3]

**What:** `CanvasScreen.kt` contains the toolbar, color picker, background picker, stroke slider, canvas gesture handling, draw loop, and four dialogs — all in one function. This makes it untestable and hard to maintain.

**Where:** `CanvasScreen.kt`

**Fix:** Extract into composables:
- `CanvasToolbar(state, onAction)` — top app bar
- `DrawingToolbar(state, onAction)` — color/eraser/highlighter/shape/bg row
- `ColorPickerRow(colors, selected, onSelect)` — collapsible color row
- `BackgroundPickerRow(current, onSelect)` — collapsible bg row  
- `StrokeWidthSlider(width, onWidthChange)` — size row
- `DrawingCanvas(state, onStrokeStart, onStrokeDrag, onStrokeEnd)` — the actual canvas
- Keep dialogs as separate `@Composable` functions

---

### CODE-05 · Sort order and filter state not persisted [P2]

**What:** `DashboardViewModel` stores `_sortOrder` and `_filter` as in-memory `MutableStateFlow`. On app restart, sort always resets to `NEWEST_FIRST` and filter to `All`.

**Where:** `DashboardViewModel.kt`

**Fix:** Persist using `DataStore<Preferences>`:

```kotlin
// Add to app/build.gradle:
implementation 'androidx.datastore:datastore-preferences:1.0.0'

// di/AppModule.kt — provide DataStore
@Provides @Singleton
fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("settings") }
```

```kotlin
// DashboardViewModel.kt
private val SORT_KEY = stringPreferencesKey("sort_order")

init {
    viewModelScope.launch {
        val saved = dataStore.data.first()[SORT_KEY]
        _sortOrder.value = saved?.let { SortOrder.valueOf(it) } ?: SortOrder.NEWEST_FIRST
    }
}

fun setSortOrder(order: SortOrder) {
    _sortOrder.value = order
    viewModelScope.launch {
        dataStore.edit { prefs -> prefs[SORT_KEY] = order.name }
    }
}
```

---

### CODE-06 · `RectangleFit.fitRectangle()` rejects wide/tall rectangles [P2]

**What:** `fitRectangle()` only recognizes shapes with aspect ratio between 0.5 and 2.0 (`isRectangular = aspectRatio > 0.5f && aspectRatio < 2f`). Drawing a tall thin rectangle (e.g., 3:1 aspect) never gets recognized.

**Where:** `ShapeRecognitionHelper.kt` → `fitRectangle()`

**Fix:** Remove the aspect ratio restriction. Instead, require minimum width AND height independently:

```kotlin
// Replace aspect ratio check with minimum dimension check:
if (width < 30f || height < 30f) return null
// Remove: val isRectangular = ... / if (!isRectangular) return null
```

---

### CODE-07 · `fitCircle()` uses centroid as center — wrong for arcs [P2]

**What:** `fitCircle()` approximates the circle center as the average (centroid) of all input points. For a partial arc or circle drawn starting and ending at the same area, the centroid is biased and the radius estimate is inaccurate.

**Where:** `ShapeRecognitionHelper.kt` → `fitCircle()`

**Fix:** Use Kåsa's algebraic circle fit (or a simplified 3-point fit using first, middle, last):

```kotlin
private fun fitCircle(points: List<Offset>): CircleFit? {
    if (points.size < 5) return null
    // Use first, middle, last three points for a robust 3-point circle fit
    val p1 = points.first()
    val p2 = points[points.size / 2]
    val p3 = points.last()
    
    val ax = p2.x - p1.x; val ay = p2.y - p1.y
    val bx = p3.x - p1.x; val by = p3.y - p1.y
    val D = 2 * (ax * by - ay * bx)
    if (kotlin.math.abs(D) < 1e-6f) return null  // collinear
    
    val ux = (by * (ax*ax + ay*ay) - ay * (bx*bx + by*by)) / D
    val uy = (ax * (bx*bx + by*by) - bx * (ax*ax + ay*ay)) / D
    val center = Offset(p1.x + ux, p1.y + uy)
    val radius = hypot(ux, uy)
    if (radius < 10f) return null
    
    // Measure how well all points fit this circle
    var totalError = 0f
    for (pt in points) {
        totalError += kotlin.math.abs(hypot(pt.x - center.x, pt.y - center.y) - radius)
    }
    val avgError = totalError / points.size
    val confidence = (1f - (avgError / radius)).coerceIn(0f, 1f)
    
    return CircleFit(center, radius, confidence)
}
```

---

### CODE-08 · `setColor()` clears `isShapeMode` but `toggleEraser()` does not [P3]

**What:** Activating eraser or highlighter does NOT deactivate shape mode. Shape mode is only deactivated by `setColor()` and `toggleHighlighter()` (partially). This allows eraser + shape mode to be simultaneously active, producing undefined behavior.

**Where:** `CanvasViewModel.kt` → `toggleEraser()`

```kotlin
// CURRENT — missing isShapeMode = false
fun toggleEraser() {
    _state.update { it.copy(isEraser = !it.isEraser, isHighlighter = false) }
}
```

**Fix:**

```kotlin
fun toggleEraser() {
    _state.update { it.copy(isEraser = !it.isEraser, isHighlighter = false, isShapeMode = false) }
}
```

---

### CODE-09 · Strokes are stored with `isHighlighter` flag AND `StrokeType` redundantly [P3]

**What:** `Stroke` has both `isHighlighter: Boolean` and `strokeType: StrokeType` enum. The highlighter is not represented in `StrokeType` (which has PEN, HIGHLIGHTER, LINE, RECTANGLE, CIRCLE). `StrokeType.HIGHLIGHTER` exists but is never assigned. This duplication causes rendering logic to check both flags.

**Where:** `domain/model/Stroke.kt`, `CanvasScreen.kt` draw loop

**Fix:** Unify: remove `isHighlighter: Boolean` and `isEraser: Boolean` from `Stroke`. Use `StrokeType`:

```kotlin
enum class StrokeType { PEN, HIGHLIGHTER, ERASER, LINE, RECTANGLE, CIRCLE }

data class Stroke(
    val points: String = "",
    val colorValue: Int = 0xFFFFFFFF.toInt(),
    val strokeWidth: Float = 4f,
    val strokeType: StrokeType = StrokeType.PEN
)
```

Update all references. The render switch becomes clean:
```kotlin
when (stroke.strokeType) {
    StrokeType.ERASER -> bgColor
    StrokeType.HIGHLIGHTER -> Color(stroke.colorValue).copy(alpha = 0.4f)
    else -> Color(stroke.colorValue)
}
```

---

## 4. SECURITY FIXES

### SEC-01 · `allowBackup="true"` exposes all user notes via ADB [P1]

**What:** With `android:allowBackup="true"` in the manifest, the app's internal storage (including all `strokes_<id>.json` files and the Room database) can be extracted by anyone with USB access using `adb backup`. No root required on debug builds.

**Where:** `AndroidManifest.xml`

**Fix:**

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="false"
    ...>
```

If users should be able to back up their notes, implement explicit backup rules excluding sensitive files, or implement a proper export mechanism controlled by the user.

---

### SEC-02 · ProGuard rules file is essentially empty [P2]

**What:** `proguard-rules.pro` contains only boilerplate comments. Release builds have minification enabled but no rules to keep Gson-serialized classes, Hilt components, or Room entities. This will likely cause runtime crashes in release builds when Gson tries to reflect on obfuscated `Stroke` and `Note` classes.

**Where:** `app/proguard-rules.pro`

**Fix:** Add:

```proguard
# Gson serialization — keep all data classes used in JSON
-keepclassmembers class com.vibenote.app.domain.model.** { *; }
-keepclassmembers class com.vibenote.app.data.local.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
```

---

### SEC-03 · File names are predictable — `strokes_<noteId>.json` [P3]

**What:** Stroke files are named `strokes_<uuid>.json` in `context.filesDir`. This is fine for normal use, but if `allowBackup` were ever re-enabled, the mapping is trivially reversible. Additionally, no input validation on `noteId` before using it in a file path (a crafted `noteId` with path traversal characters could theoretically write to unexpected locations).

**Where:** All `File(context.filesDir, "strokes_$noteId.json")` usages

**Fix:** Sanitize noteId before file use:

```kotlin
private fun safeFileName(noteId: String): String {
    return "strokes_${noteId.replace(Regex("[^a-zA-Z0-9\\-]"), "")}.json"
}
```

Since noteIds are UUIDs generated internally, this is low risk but good practice.

---

## 5. UI/UX ENHANCEMENTS

### UX-01 · Background picker swatches are indistinguishable [P1]

**What:** The background picker shows "lined", "dotted", and "grid" options all using the same `VibeColors.SurfaceDeep` solid color swatch. The user cannot tell which is which.

**Where:** `CanvasScreen.kt` → `showBackgroundPicker` → background picker `Row`

**Fix:** Draw each swatch as a mini Canvas that actually renders its pattern:

```kotlin
@Composable
fun BackgroundSwatch(type: String, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) VibeColors.BrandGreen else VibeColors.BorderStandard
    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (type) {
                "dark"  -> drawRect(Color(0xFF171717))
                "white" -> drawRect(Color.White)
                "lined" -> {
                    drawRect(Color(0xFF171717))
                    for (y in 0..size.height.toInt() step 8) {
                        drawLine(Color(0xFF2E2E2E), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                    }
                }
                "dotted" -> {
                    drawRect(Color(0xFF171717))
                    for (y in 0..size.height.toInt() step 8)
                        for (x in 0..size.width.toInt() step 8)
                            drawCircle(Color(0xFF2E2E2E), 1f, Offset(x.toFloat(), y.toFloat()))
                }
                "grid" -> {
                    drawRect(Color(0xFF171717))
                    for (v in 0..size.height.toInt() step 8)
                        drawLine(Color(0xFF2E2E2E), Offset(0f, v.toFloat()), Offset(size.width, v.toFloat()), 1f)
                    for (h in 0..size.width.toInt() step 8)
                        drawLine(Color(0xFF2E2E2E), Offset(h.toFloat(), 0f), Offset(h.toFloat(), size.height), 1f)
                }
            }
        }
    }
}
```

---

### UX-02 · No loading indicator while note loads [P2]

**What:** `CanvasState.isLoading` is properly set to `true` during `loadNote()`, but `CanvasScreen` never observes it. The user sees a blank canvas that may suddenly populate with strokes.

**Where:** `CanvasScreen.kt` — `isLoading` state never rendered

**Fix:** Show a centered `CircularProgressIndicator` while loading:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {
    if (state.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            color = VibeColors.BrandGreen
        )
    } else {
        // existing canvas content
    }
}
```

---

### UX-03 · No stroke thumbnail on NoteCard [P2]

**What:** Each note card shows only a title and a date. Users cannot visually distinguish notes without remembering their titles.

**Where:** `DashboardScreen.kt` → `NoteCard`

**Fix:** Generate and cache a thumbnail bitmap when a note is saved. In `CanvasViewModel.saveStrokesToFile()`, also render a small thumbnail:

```kotlin
private suspend fun saveThumbnail(noteId: String, strokes: List<Stroke>) {
    withContext(Dispatchers.Default) {
        val thumb = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(thumb)
        canvas.drawColor(0xFF171717.toInt())
        // render strokes scaled to thumbnail size (scale factor 256/2048 = 0.125)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true; style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
        }
        val scale = 256f / 2048f
        strokes.forEach { stroke ->
            // draw scaled stroke path
        }
        val thumbFile = File(context.filesDir, "thumb_$noteId.png")
        FileOutputStream(thumbFile).use { thumb.compress(Bitmap.CompressFormat.PNG, 70, it) }
        thumb.recycle()
    }
}
```

In `NoteCard`, load the thumbnail with `AsyncImage` from Coil (add `implementation 'io.coil-kt:coil-compose:2.5.0'`):

```kotlin
val thumbFile = File(LocalContext.current.filesDir, "thumb_${note.id}.png")
if (thumbFile.exists()) {
    AsyncImage(
        model = thumbFile,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        contentScale = ContentScale.Crop
    )
}
```

---

### UX-04 · Highlighter button labeled "HL" — not intuitive [P2]

**What:** The highlighter toggle button shows "HL" as text. Not obvious for new users. Eraser and Shape are shown as full words.

**Where:** `CanvasScreen.kt` → drawing toolbar

**Fix:** Change label to "Highlight" or use a marker icon (`Icons.Default.Edit` with a custom tint, or `@DrawableRes R.drawable.ic_highlighter` from Material Icons Extended `Icons.Default.BorderColor`):

```kotlin
// Replace TextButton with Icon approach:
IconButton(onClick = { viewModel.toggleHighlighter() }) {
    Icon(
        Icons.Default.BorderColor,
        contentDescription = "Highlighter",
        tint = if (state.isHighlighter) VibeColors.BrandGreen else VibeColors.TextMuted
    )
}
```

---

### UX-05 · Active tool has no clear visual indicator [P2]

**What:** Active tool (pen/eraser/highlighter/shape) only changes text color. No background highlight, no icon change, no pill/chip indicator. Hard to tell active state at a glance.

**Where:** `CanvasScreen.kt` → toolbar tool buttons

**Fix:** Wrap active tool buttons in a colored chip:

```kotlin
@Composable
fun ToolButton(label: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) VibeColors.BrandGreen.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isActive) VibeColors.BrandGreen else VibeColors.TextMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = textColor, fontSize = 13.sp)
    }
}
```

---

### UX-06 · Search returns empty grid with no "no results" message [P2]

**What:** When a search yields no matching notes, the `LazyVerticalGrid` shows an empty area with no feedback. User doesn't know if the search is working.

**Where:** `DashboardScreen.kt` — empty state only checks `notes.isEmpty()` without distinguishing search-empty from truly-empty

**Fix:**

```kotlin
val isSearchActive = searchQuery.isNotBlank()
val isEmpty = notes.isEmpty()

if (isEmpty) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (isSearchActive) Icons.Default.SearchOff else Icons.Default.EditNote,
                contentDescription = null,
                tint = VibeColors.TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isSearchActive) "No notes match \"$searchQuery\""
                else "No notes yet",
                color = VibeColors.TextMuted, fontSize = 16.sp
            )
            if (!isSearchActive) {
                Spacer(Modifier.height(8.dp))
                Text("Tap + to create your first note", color = VibeColors.TextMuted, fontSize = 13.sp)
            }
        }
    }
}
```

---

### UX-07 · No haptic feedback on NoteCard long-press [P3]

**What:** Long-pressing a note card to open the action sheet has no tactile feedback.

**Where:** `DashboardScreen.kt` → `NoteCard` → `combinedClickable`

**Fix:**

```kotlin
val haptic = LocalHapticFeedback.current

.combinedClickable(
    onClick = onClick,
    onLongClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onLongClick()
    }
)
```

---

### UX-08 · Delete confirmation button is green — looks like "Confirm, positive action" [P2]

**What:** In both delete dialogs, the "Delete" confirm button uses `VibeColors.BrandGreen` — same color as all other confirm actions. Destructive actions should use red/error color to visually warn the user.

**Where:** `CanvasScreen.kt` + `DashboardScreen.kt` → delete `AlertDialog` confirm buttons

**Fix:**

```kotlin
// In delete dialogs:
TextButton(onClick = { ... }) {
    Text("Delete", color = Color(0xFFFF6B6B))  // use existing red from palette
}
```

---

### UX-09 · No way to add a custom color — only 8 presets [P3]

**What:** The color picker offers exactly 8 hardcoded colors. Users cannot choose custom colors.

**Where:** `CanvasScreen.kt` → color picker `Row`

**Fix:** Add a "+" button at the end of the color row that opens a simple HSV color picker dialog using `androidx.compose.material3`:

```kotlin
// Add to color list row:
Box(
    modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(VibeColors.SurfaceDeep)
        .border(1.dp, VibeColors.BorderStandard, CircleShape)
        .clickable { showCustomColorPicker = true },
    contentAlignment = Alignment.Center
) {
    Icon(Icons.Default.Add, contentDescription = "Custom color", tint = VibeColors.TextMuted, modifier = Modifier.size(16.dp))
}
```

Implement a minimal HSV picker dialog with hue slider + saturation/brightness selector. Persist last-used custom colors (up to 3) using DataStore.

---

### UX-10 · Export PNG doesn't apply stroke smoothing [P2]

**What:** `exportAsPng` in `CanvasViewModel` uses `android.graphics.Path.lineTo()` between points — straight segments. On screen, `toSmoothedPath()` uses quadratic Bézier curves. Exported images look jagged compared to on-screen rendering.

**Where:** `CanvasViewModel.kt` → `exportAsPng()`

**Fix:** Use the same Bézier smoothing for the export path:

```kotlin
// In exportAsPng, replace the path building loop with:
val path = android.graphics.Path()
if (pointsList.size >= 2) {
    path.moveTo(pointsList[0].x, pointsList[0].y)
    for (i in 1 until pointsList.size - 1) {
        val midX = (pointsList[i].x + pointsList[i + 1].x) / 2f
        val midY = (pointsList[i].y + pointsList[i + 1].y) / 2f
        path.quadTo(pointsList[i].x, pointsList[i].y, midX, midY)
    }
    path.lineTo(pointsList.last().x, pointsList.last().y)
}
canvas.drawPath(path, paint)
```

Also apply canvas background to the exported bitmap (currently always dark regardless of note's background setting):

```kotlin
when (_state.value.canvasBackground) {
    "white" -> canvas.drawColor(android.graphics.Color.WHITE)
    else -> canvas.drawColor(0xFF171717.toInt())
}
// For lined/dotted/grid: draw the pattern before strokes
```

---

### UX-11 · No Clear Canvas button [P2]

**What:** `clearCanvas()` exists in `CanvasViewModel` but is not exposed in the UI. The only way to clear the canvas is to delete the note. Users expect a "clear all" action.

**Where:** `CanvasScreen.kt` → canvas toolbar actions

**Fix:** Add a "Clear" option in the overflow menu or long-press on the delete icon. Show a confirmation dialog:

```kotlin
// Add to TopAppBar actions:
var showClearDialog by remember { mutableStateOf(false) }

IconButton(onClick = { showClearDialog = true }) {
    Icon(Icons.Default.LayersClear, contentDescription = "Clear canvas", tint = VibeColors.TextPrimary)
}

// Add dialog:
if (showClearDialog) {
    AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text("Clear Canvas", color = VibeColors.TextPrimary) },
        text = { Text("Remove all strokes? This can be undone.", color = VibeColors.TextMuted) },
        confirmButton = {
            TextButton(onClick = { viewModel.clearCanvas(); showClearDialog = false }) {
                Text("Clear", color = Color(0xFFFF6B6B))
            }
        },
        dismissButton = {
            TextButton(onClick = { showClearDialog = false }) { Text("Cancel", color = VibeColors.TextMuted) }
        },
        containerColor = VibeColors.BackgroundDark
    )
}
```

---

## 6. FEATURE UPGRADES (Roadmap — No Cloud/AI)

### FEAT-01 · PDF Export [P2]

**What:** Roadmap v1.1 item. Export canvas as a PDF file using Android's `PdfDocument` API (no third-party library needed).

**Implementation:**

```kotlin
// CanvasViewModel.kt — add alongside exportAsPng:
fun exportAsPdf(context: Context, onExported: (Uri) -> Unit) {
    viewModelScope.launch(Dispatchers.Default) {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(2048, 1536, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        // Draw background + strokes (same logic as exportAsPng)
        drawStrokesToCanvas(canvas, _state.value)
        
        pdfDocument.finishPage(page)
        
        val filename = "bloom_${_state.value.noteTitle}_${System.currentTimeMillis()}.pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Bloom")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                pdfDocument.writeTo(stream)
            }
            pdfDocument.close()
            withContext(Dispatchers.Main) { onExported(it) }
        } ?: pdfDocument.close()
    }
}
```

Add a PDF export button alongside the PNG export icon in the toolbar.

---

### FEAT-02 · Lasso Selection Tool [P2]

**What:** Roadmap v1.1 item. Allow users to draw a freehand lasso, select enclosed strokes, then move or delete them.

**Implementation approach:**
1. Add `StrokeType.LASSO` and `ToolMode.LASSO` to the tool enum.
2. In `CanvasViewModel`, add `selectedStrokeIndices: Set<Int>` to `CanvasState`.
3. On lasso `onDragEnd`, use point-in-polygon test (ray casting) to find strokes whose centroid falls inside the lasso path.
4. Render selected strokes with a highlight border (`drawPath` with a second `DrawStroke` in `BrandGreen` color, larger width, alpha 0.5).
5. Show a floating action bar when selection is active: `[Move] [Delete] [Copy]`.
6. For Move: track drag delta and apply offset to all selected stroke points, then commit to undo stack.
7. For Delete: call `clearCanvas`-style removal of only selected strokes.

---

### FEAT-03 · Text Tool on Canvas [P2]

**What:** Roadmap v1.1 item. Allow placing text labels on the canvas.

**Implementation approach:**
1. Add `TextStroke` data class:
```kotlin
data class TextElement(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float, val y: Float,
    val colorValue: Int,
    val fontSize: Float = 24f
)
```
2. Store `textElements: List<TextElement>` in `CanvasState` (saved to the strokes JSON file alongside stroke data).
3. In Text tool mode, `onTap` shows an inline `TextField` at the tap location. On `Done`/`IME_ACTION_DONE`, commit the `TextElement`.
4. Render in `Canvas` using `drawContext.canvas.nativeCanvas.drawText(...)` with `android.graphics.Paint`.
5. In `exportAsPng`/`exportAsPdf`, also render text elements.

---

### FEAT-04 · Stylus Pressure Sensitivity [P3]

**What:** Roadmap v1.1 item. Vary stroke width based on stylus pressure.

**Implementation:**

```kotlin
// In CanvasScreen.kt, replace detectDragGestures with pointerInput + awaitEachGesture:
.pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        var points = listOf(down.position)
        var pressures = listOf(down.pressure)
        
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (change.pressed) {
                points = points + change.position
                pressures = pressures + change.pressure   // 0f..1f
                change.consume()
            } else {
                // stroke end
                viewModel.finishStrokeWithPressure(points, pressures)
                break
            }
        }
    }
}
```

Store per-point pressure in the stroke data (extend `Stroke` with `pressures: String` similar to `points`). Use pressure to vary `strokeWidth` per path segment using `drawPath` with variable-width segments drawn as filled polygons.

---

### FEAT-05 · Tag Filter Chips on Dashboard [P2]

**What:** Tags exist in the data model and `FilterType.Tag` is implemented in `DashboardViewModel`, but there is no UI to filter by tag. The filter is completely hidden.

**Implementation:**

```kotlin
// DashboardScreen.kt — add below TopAppBar, above the note grid:
val allTags = remember(notes) { notes.flatMap { it.tags }.distinct().sorted() }

if (allTags.isNotEmpty()) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = filter is FilterType.All,
                onClick = { viewModel.clearFilter() },
                label = { Text("All") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = VibeColors.BrandGreen,
                    selectedLabelColor = VibeColors.SurfaceDeep
                )
            )
        }
        // Favorites chip
        item {
            FilterChip(
                selected = filter is FilterType.Favorites,
                onClick = { viewModel.setFilter(FilterType.Favorites) },
                label = { Text("⭐ Favorites") },
                ...
            )
        }
        // Tag chips
        items(allTags) { tag ->
            FilterChip(
                selected = filter is FilterType.Tag && (filter as FilterType.Tag).tag == tag,
                onClick = { viewModel.filterByTag(tag) },
                label = { Text(tag) },
                ...
            )
        }
    }
}
```

---

### FEAT-06 · Folder Management UI [P2]

**What:** Folders exist in the data model and `FilterType.Folder` is in `DashboardViewModel`, but there is no UI to create, rename, or navigate folders. The feature is completely inaccessible.

**Implementation:**
1. Add folder list to a `NavigationDrawer` or a side panel button in the top bar.
2. `FolderManagementSheet` composable: shows existing folders, allows create/rename/delete.
3. Long-press note action sheet: add "Move to Folder" option showing a folder picker bottom sheet.
4. When a folder filter is active, show folder name in the top bar with a breadcrumb back button.

---

### FEAT-07 · Bulk Selection and Delete on Dashboard [P3]

**What:** There is no way to delete multiple notes at once. Users must delete one at a time.

**Implementation:**
1. Long-press enters "selection mode" — shows checkboxes on note cards.
2. `DashboardViewModel` adds `selectedNoteIds: Set<String>` to state.
3. Top bar in selection mode shows count + "Delete" and "Cancel" actions.
4. `deleteNotes(ids: Set<String>)` in ViewModel deletes all selected notes and their stroke files.

---

### FEAT-08 · Page/Canvas Size Options [P3]

**What:** Currently the canvas is always "infinite" with a fixed export size of 2048×1536. Allow users to choose a page size (A4, Letter, infinite) when creating a note.

**Implementation:**
1. Add `pageSize: String` to `Note` and `NoteEntity` with a new migration (version 5→6).
2. In `NewNoteDialog`, show page size options: "Infinite", "A4 Portrait", "A4 Landscape", "Letter".
3. In `CanvasScreen`, if `pageSize != "infinite"`, draw a white page rectangle in the center of the canvas (e.g., 794×1123px for A4 at 96dpi).
4. Constrain the scroll/pan so the page stays centered and visible.
5. In `exportAsPng`/`exportAsPdf`, use the page dimensions for the output bitmap size.

---

### FEAT-09 · App Shortcut to Create New Note [P3]

**What:** No home screen shortcut to quickly create a new note. Users must open the app and tap `+`.

**Implementation:**

```xml
<!-- res/xml/shortcuts.xml -->
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="new_note"
        android:enabled="true"
        android:icon="@drawable/ic_launcher_foreground"
        android:shortcutShortLabel="@string/new_note_short"
        android:shortcutLongLabel="@string/new_note_long">
        <intent
            android:action="com.bloom.app.ACTION_NEW_NOTE"
            android:targetPackage="com.bloom.app"
            android:targetClass="com.vibenote.app.presentation.BloomActivity" />
    </shortcut>
</shortcuts>
```

In `BloomActivity`, handle `intent.action == "com.bloom.app.ACTION_NEW_NOTE"` to navigate directly to `NewNoteDialog`.

---

## 7. MISSING TEST COVERAGE

**What:** Zero test files exist in the project. No unit tests, no instrumentation tests.

**Priority files to test first:**

| File | Test type | What to test |
|---|---|---|
| `ShapeRecognitionHelper` | Unit | Line/circle/rect recognition accuracy; edge cases (< 5 points, zero-length paths) |
| `DashboardViewModel` | Unit | Filter, sort, search combinations; note CRUD |
| `CanvasViewModel` | Unit | Undo/redo stack integrity; scheduleSave debounce |
| `NoteRepositoryImpl` | Unit | Tag serialization/deserialization round-trip |
| `NoteDao` | Instrumented (Room in-memory) | Tag LIKE query correctness |

**Setup:**

```kotlin
// app/build.gradle — add test dependencies:
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'
testImplementation 'io.mockk:mockk:1.13.8'
testImplementation 'androidx.arch.core:core-testing:2.2.0'
androidTestImplementation 'androidx.room:room-testing:2.6.1'
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
```

**Sample test for BUG-05 (tag query):**

```kotlin
@Test
fun `getNotesByTag does not match partial tag names`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java).build()
    val dao = db.noteDao()
    dao.insertNote(NoteEntity(id="1", title="A", createdAt=0, strokeDataPath="",
        tags = "party\u001Fart"))   // tags: ["party", "art"]
    dao.insertNote(NoteEntity(id="2", title="B", createdAt=0, strokeDataPath="",
        tags = "art"))              // tags: ["art"]
    
    val results = dao.getNotesByTag("art").first()
    assertEquals(2, results.size)  // both have "art"
    
    val partyResults = dao.getNotesByTag("arty").first()
    assertEquals(0, partyResults.size)  // "arty" should NOT match "party" or "art"
}
```

---

## 8. DEPENDENCY UPDATES

| Library | Current | Latest (as of Apr 2026) | Action |
|---|---|---|---|
| `compose-bom` | 2023.10.01 | 2024.06.00+ | Update — includes Compose UI stability fixes |
| `lifecycle-*` | 2.6.2 | 2.8.x | Update |
| `activity-compose` | 1.8.1 | 1.9.x | Update |
| `navigation-compose` | 2.7.5 | 2.8.x | Update |
| `hilt-android` | 2.48.1 | 2.51.x | Update |
| `room` | 2.6.1 | 2.6.1 | OK |
| `gson` | 2.10.1 | 2.10.1 | OK — consider replacing with `kotlinx.serialization` |
| `compileSdk` | 34 | 35 | Update `compileSdk` and `targetSdk` to 35 |
| Kotlin | 1.9 (inferred) | 2.0.x | Update — Compose K2 compiler available |

**Replace Gson with kotlinx.serialization (optional but recommended):**

```kotlin
// build.gradle — add:
id 'org.jetbrains.kotlin.plugin.serialization' version '2.0.0'
implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0'
// Remove: implementation 'com.google.code.gson:gson:2.10.1'
```

Annotate `Stroke` with `@Serializable`. Replace `gson.toJson(strokes)` with `Json.encodeToString(strokes)` and `gson.fromJson(...)` with `Json.decodeFromString(...)`. Removes a 300KB dependency.

---

## 9. IMPLEMENTATION ORDER FOR OPENCODE AI

Execute in this order to minimize merge conflicts:

1. **BUG-03** (saveNow race) — prevents data loss, affects all saves
2. **BUG-01** (duplicate strokes) — data corruption
3. **BUG-02** (delete leaks) — storage leak
4. **SEC-01** (allowBackup=false) — one-line manifest change
5. **SEC-02** (ProGuard rules) — prevents release crashes
6. **BUG-05** (tag SQL) + DB migration 4→5
7. **BUG-04** (updateTitle uses insertNote)
8. **BUG-06** (eraser on light backgrounds)
9. **BUG-07** (isLinear NaN)
10. **BUG-08** (onDragStart guard)
11. **BUG-10** (package/name consistency)
12. **CODE-01** (CanvasBackground enum) + migration
13. **PERF-01** (stroke re-parsing) 
14. **PERF-03** (updateNoteTimestamp removal)
15. **PERF-04 + PERF-05** (saveStrokesToFile unification)
16. **UX-01** (background swatches)
17. **UX-02** (loading indicator)
18. **UX-06** (empty search state)
19. **UX-08** (delete button red)
20. **UX-05** (active tool chip)
21. **UX-04** (HL label → icon)
22. **UX-11** (Clear Canvas button)
23. **CODE-05** (persist sort order via DataStore)
24. **CODE-06** (rectangle aspect ratio)
25. **CODE-07** (circle fit algorithm)
26. **CODE-08** (eraser clears shapeMode)
27. **UX-10** (export PNG smoothing + background)
28. **PERF-02** (background caching)
29. **BUG-09** (gesture conflict)
30. **CODE-02** (Stroke domain model cleanup)
31. **CODE-04** (CanvasScreen decomposition)
32. **UX-03** (note card thumbnails)
33. **FEAT-05** (tag filter chips)
34. **FEAT-06** (folder management UI)
35. **FEAT-01** (PDF export)
36. **FEAT-02** (lasso selection)
37. **FEAT-03** (text tool)
38. **FEAT-07** (bulk select/delete)
39. **FEAT-04** (stylus pressure)
40. **FEAT-08** (page size)
41. **FEAT-09** (home screen shortcut)
42. **UX-09** (custom color picker)
43. **UX-07** (haptic feedback)
44. Tests for all above

---

*All items above are fully local — no internet, no cloud, no AI/ML, no Docker required.*
