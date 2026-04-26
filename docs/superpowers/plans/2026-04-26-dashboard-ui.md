# Bloom Dashboard UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enhance Dashboard with interactive empty state, smooth search animation, and refined grid layout.

**Architecture:** Use Compose animations for search bar. Update `DashboardScreen` to handle new UI states.

**Tech Stack:** Jetpack Compose, Kotlin, Hilt.

---

### Task 1: Search Bar Animation

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Implement animated search visibility**

```kotlin
// Wrap TopAppBar content in AnimatedContent or use visibility animation
// Transition from title text to OutlinedTextField
AnimatedVisibility(
    visible = showSearch,
    enter = fadeIn() + slideInHorizontally(),
    exit = fadeOut() + slideOutHorizontally()
) {
    // Search TextField
}
```

- [ ] **Step 2: Verify in device**

Run: `adb shell am start -n com.bloom.app/com.vibenote.app.presentation.BloomActivity`
Action: Tap Search icon.
Expected: Search bar slides in smoothly.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/vibenote/app/presentation/dashboard/DashboardScreen.kt
git commit -m "feat(ui): add search bar animation"
```

---

### Task 2: Playful Empty State

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Replace icon with scribble illustration mockup**

```kotlin
// Create ScribbleIllustration composable or use specific icon/painter
Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("✎✨", fontSize = 64.sp) // Temporary scribble emoji/icon
    Text("Start blooming!", color = VibeColors.TextMuted)
    Button(onClick = onNewNote) { Text("Create Note") }
}
```

- [ ] **Step 2: Verify on device (delete all notes first)**

Expected: Playful illustration and CTA visible.

- [ ] **Step 3: Commit**

```bash
git commit -am "feat(ui): update empty state with playful illustration"
```

---

### Task 3: Grid Polish

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/presentation/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Update NoteCard elevation and border**

```kotlin
Card(
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.5.dp, VibeColors.BorderStandard.copy(alpha = 0.5f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
)
```

- [ ] **Step 2: Verify layout**

Expected: Cards look more defined.

- [ ] **Step 3: Commit**

```bash
git commit -am "style(ui): refine dashboard grid and cards"
```
