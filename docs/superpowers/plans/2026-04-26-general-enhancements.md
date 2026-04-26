# Bloom General Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a pre-loaded "Welcome" note and add a Light Mode toggle in settings.

**Architecture:** Use `NoteDatabase` to seed the welcome note on first launch. Update `Theme.kt` to handle dynamic dark/light colors.

**Tech Stack:** Room (Database), Jetpack Compose Theme.

---

### Task 1: Seed Welcome Note

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/data/local/NoteDatabase.kt`

- [ ] **Step 1: Add Room callback for initial seeding**

```kotlin
// Add RoomDatabase.Callback() to insert a welcome note with sample strokes
```

- [ ] **Step 2: Commit**

```bash
git commit -am "feat(onboarding): seed welcome note on first launch"
```

---

### Task 2: Light Mode Support

**Files:**
- Modify: `app/src/main/java/com/vibenote/app/core/theme/Color.kt`
- Modify: `app/src/main/java/com/vibenote/app/core/theme/Theme.kt`

- [ ] **Step 1: Define Light Mode color palette**

```kotlin
val LightBackground = Color(0xFFF8F9FA)
// etc.
```

- [ ] **Step 2: Update BloomTheme to support darkTheme parameter**

```kotlin
@Composable
fun BloomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Select colors based on darkTheme
}
```

- [ ] **Step 3: Add toggle in Dashboard (temporary settings location)**

- [ ] **Step 4: Verify on device**

Expected: "Welcome" note appears on first run. App colors change on toggle.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(theme): add support for light mode"
```
