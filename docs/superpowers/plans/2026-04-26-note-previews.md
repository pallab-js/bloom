# Bloom Note Previews Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render a miniature full-canvas preview of the note content on each `NoteCard` in the dashboard.

**Architecture:** Create a `CanvasPreview` composable that draws a list of `Stroke` objects scaled to fit the card. Update `NoteCard` to include this preview.

**Tech Stack:** Jetpack Compose Canvas.

---

### Task 1: Canvas Preview Composable

**Files:**
- Create: `app/src/main/java/com/vibenote/app/presentation/dashboard/components/CanvasPreview.kt`

- [ ] **Step 1: Implement CanvasPreview logic**

```kotlin
@Composable
fun CanvasPreview(
    strokes: List<Stroke>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Calculate scale based on canvas size vs preview size
        // Draw each stroke scaled down
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/vibenote/app/presentation/dashboard/components/CanvasPreview.kt
git commit -m "feat(ui): add CanvasPreview component"
```

---

### Task 2: Update NoteCard

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Integrate CanvasPreview into NoteCard**

```kotlin
// Add CanvasPreview before title in NoteCard Column
CanvasPreview(
    strokes = note.strokes,
    modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
        .padding(bottom = 8.dp)
)
```

- [ ] **Step 2: Verify on device**

Expected: Notes show a mini version of their drawings.

- [ ] **Step 3: Commit**

```bash
git commit -am "feat(ui): show canvas preview on note cards"
```
