# Bloom

<p align="center">
  <img src="https://raw.githubusercontent.com/pallab-js/bloom/master/.github/icon.png" width="120" alt="Bloom Logo"/>
</p>

<p align="center">
  <strong>A premium handwritten note-taking app for Android</strong><br/>
  Infinite canvas. Shape recognition. Professional organization.
</p>

---

## Features

### Digital Research Lab Workspace
- **Workspace Sidebar** - Collapsible navigation for Folders, Tags, and Favorites.
- **Split-View Reference Pane** - Open images or documents on the left while taking notes on the right.
- **Layered Input System** - Foundations of structured text with a transparent ink overlay for annotations.
- **Folder Management** - Organize research into project-based folders.
- **Advanced Metadata** - Hybrid organization using both Folders and Tags.

### Canvas Experience
- **Infinite Canvas** - Draw freely without boundaries
- **Shape Recognition** - Automatically converts freehand strokes to shapes (lines, rectangles, circles)
- **Pinch-to-Zoom** - Two-finger pinch to zoom in/out (0.5x - 3x)
- **Two-Finger Pan** - Navigate large canvases with two fingers
- **Highlighter Tool** - Semi-transparent strokes for highlighting (40% opacity)
- **Stroke Smoothing** - Bézier curve interpolation for smooth lines
- **Canvas Backgrounds** - Choose from dark, white, lined, dotted, or grid backgrounds

### Drawing Tools
- **Pen Tool** - Multiple colors with adjustable stroke width (2-20px)
- **Eraser** - Remove mistakes easily
- **Colors** - 8 preset colors: White, Brand Green, Red, Teal, Yellow, Mint, Coral, Lavender
- **Undo/Redo** - Full history support (capped at 50 entries)

### Organization
- **Persistent Preferences** - Remembers your sort order across app restarts
- **Sort Options** - Sort by newest, oldest, last modified, A-Z, or Z-A
- **Search** - Find notes by title instantly
- **Tags/Labels** - Organize notes with custom tags (pipe-safe serialization)
- **Folders** - Group notes into folders
- **Favorites** - Mark important notes with favorites
- **Note Actions** - Long-press for quick actions (favorite, duplicate, delete)

### Export & Share
- **PNG Export** - Save notes as high-quality images to Pictures/Bloom
- **Share Sheet** - Share via Android share intent
- **Clear Canvas** - Quickly reset a canvas with one tap (supports Undo)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose (Material 3) |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Database | Room |
| Persistence | Jetpack DataStore |
| Async | Kotlin Coroutines + Flow |
| Testing | JUnit 4, MockK, Turbine |
| Build | Gradle |

---

## Project Structure

```
app/src/main/java/com/vibenote/app/
├── core/theme/           # Design tokens (Colors, Typography, Shapes)
├── data/
│   ├── local/       # Room database (NoteEntity, NoteDao, Converters, StrokeDto)
│   └── repository/  # Repository implementations
├── di/              # Hilt dependency injection
├── domain/
│   ├── model/       # Domain models (Note, Stroke, CanvasBackground)
│   └── repository/  # Repository interfaces
└── presentation/
    ├── canvas/      # Canvas screen + ViewModel + Shape recognition
    ├── dashboard/   # Dashboard screen + ViewModel
    └── BloomActivity.kt
```

---

## Testing

The project includes a suite of unit tests for core logic.

```bash
# Run unit tests
./gradlew testDebugUnitTest
```

---

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

---

## Security

- **Local Storage Only** - All data stored locally on device
- **No Network Permissions** - App does not require internet
- **Encapsulated Backups** - Device backups are disabled (`allowBackup=false`) to ensure notes stay on-device
- **Room Migrations** - Proper schema versioning for data integrity
- **ProGuard Ready** - Optimized and obfuscated release builds with pre-configured rules
- **Input Validation** - Comprehensive protection against malicious data:
  - Stroke point count limited to 5,000 per stroke
  - String length validation (max 100KB per stroke)
  - Total stroke count limited to 10,000 per note
  - File size validation (max 10MB per note file)
  - Malformed data gracefully handled without crashes

---

## License

MIT License - See LICENSE file

---

<p align="center">
  <sub>Built with ❤️ using Jetpack Compose</sub>
</p>
