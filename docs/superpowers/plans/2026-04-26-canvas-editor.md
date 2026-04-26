# Bloom Canvas Editor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine editor toolbar, add shape recognition haptics, and implement smooth momentum-based pan/zoom.

**Architecture:** Use `HapticFeedback` for shape detection. Update gesture handling in `CanvasScreen` for momentum/smoothness.

**Tech Stack:** Jetpack Compose Gestures, HapticFeedback.

---

### Task 1: Shape Recognition Haptics

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/canvas/CanvasScreen.kt`

- [ ] **Step 1: Trigger haptics when shape recognized**

```kotlin
// Inside pointerInput gesture block
if (state.isShapeMode && points.size >= 5) {
    val shapeStroke = viewModel.applyShapeRecognition(points)
    if (shapeStroke != null) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // or specific haptic
        viewModel.startStroke(shapeStroke)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git commit -am "feat(editor): add haptic feedback for shape recognition"
```

---

### Task 2: Smooth Pan/Zoom & Double-Tap

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/canvas/CanvasScreen.kt`

- [ ] **Step 1: Implement double-tap to reset zoom**

```kotlin
// Add detectTapGestures to pointerInput or similar
.pointerInput(Unit) {
    detectTapGestures(
        onDoubleTap = {
            canvasScale = 1f
            canvasOffsetX = 0f
            canvasOffsetY = 0f
        }
    )
}
```

- [ ] **Step 2: Add momentum/smoothing to zoom/pan**

```kotlin
// Use animateTo for transitions if possible or higher-level transformable
```

- [ ] **Step 3: Verify on device**

Expected: Smooth interactions, haptics on shapes, double-tap resets view.

- [ ] **Step 4: Commit**

```bash
git commit -am "feat(editor): improve zoom/pan smoothness and add double-tap reset"
```

---

### Task 3: Toolbar Polish

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/canvas/CanvasScreen.kt`

- [ ] **Step 1: Refine toolbar layout and active states**

```kotlin
// Add better background/padding for active tools
// Group related tools (Pen, Eraser, Highlighter, Shape)
```

- [ ] **Step 2: Commit**

```bash
git commit -am "style(editor): polish toolbar layout and tool states"
```
