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
- **Sort Options** - Sort by newest, oldest, last modified, A-Z, or Z-A
- **Search** - Find notes by title instantly
- **Tags/Labels** - Organize notes with custom tags (comma-safe serialization)
- **Folders** - Group notes into folders
- **Favorites** - Mark important notes with favorites
- **Note Actions** - Long-press for quick actions (favorite, duplicate, delete)

### Export & Share
- **PNG Export** - Save notes as images to Pictures/Bloom
- **Share Sheet** - Share via Android share intent

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose (Material 3) |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Database | Room |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle |

---

## Project Structure

```
app/src/main/java/com/vibenote/app/
├── core/theme/           # Design tokens (Colors, Typography, Shapes)
├── data/
│   ├── local/       # Room database (NoteEntity, NoteDao)
│   └── repository/  # Repository implementations
├── di/              # Hilt dependency injection
├── domain/
│   ├── model/       # Domain models (Note, Stroke)
│   └── repository/  # Repository interfaces
└── presentation/
    ├── canvas/      # Canvas screen + ViewModel + Shape recognition
    ├── dashboard/   # Dashboard screen + ViewModel
    └── BloomActivity.kt
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

## Design System

### Colors

| Token | Hex | Usage |
|-------|-----|-------|
| BackgroundDark | #171717 | Main background |
| SurfaceDeep | #0F0F0F | Cards, toolbar |
| BrandGreen | #3ECF8E | Primary accent |
| ActionGreen | #00C573 | Buttons, actions |
| TextPrimary | #FAFAFA | Primary text |
| TextMuted | #898989 | Secondary text |
| BorderStandard | #2E2E2E | Standard borders |

### Typography

- **Display/Hero**: Font Family Default, Weight 400
- **Code/Labels**: Font Family Monospace, Weight Medium

---

## Security

- **Local Storage Only** - All data stored locally on device
- **No Network Permissions** - App does not require internet
- **Room Migrations** - Proper schema versioning for data integrity
- **ProGuard Enabled** - Code obfuscation in release builds

---

## Roadmap

### v1.1 (Planned)
- [ ] PDF Export
- [ ] Pressure sensitivity (stylus support)
- [ ] Lasso selection
- [ ] Text tool on canvas

### v1.2 (Planned)
- [ ] Cloud sync
- [ ] Multiple notebooks
- [ ] Page templates

---

## License

MIT License - See LICENSE file

---

<p align="center">
  <sub>Built with ❤️ using Jetpack Compose</sub>
</p>