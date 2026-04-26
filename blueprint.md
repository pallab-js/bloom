# **Technical Blueprint: VibeNote (Goodnotes-Inspired Android App)**

## **1\. Executive Summary**

**Project Name:** VibeNote  
**Platform:** Android (Kotlin \+ Jetpack Compose)  
**Target Environment:** Apple MacBook Air M1 (8GB RAM) \-\> ADB \-\> Real Android Device  
**Development Methodology:** AI-Assisted "Vibecoding" via OpenCode AI.  
**Design Language:** Strict adherence to the Supabase Dark-Mode-Native Design System (Terminal-inspired, HSL layers, Border-based depth).  
**AI Token & Memory Strategy:** Developing on an 8GB M1 machine requires strict token economy. OpenCode AI will be guided through heavily isolated, micro-scoped prompts (Phases). We will never ask the AI to "build the app." Instead, we will ask it to "build the NoteEntity data class," then "build the CanvasUI composable." This modularity eliminates hallucinations and prevents context window overflow.

## **2\. Product Requirements Document (PRD) & MVP Scope**

### **2.1. MVP Features**

1. **Dashboard/Library:** A grid view of existing notebooks/notes.  
2. **Infinite Canvas:** A drawing surface that supports freehand strokes.  
3. **Ink System:** Basic pen tool (variable colors, fixed widths), Eraser tool, and Undo/Redo mechanism.  
4. **Local Persistence:** Saving note metadata to SQLite and stroke data to local JSON/binary files.  
5. **Strict UI Theming:** 100% adherence to the Supabase dark theme (no shadows, border depth, \#171717 backgrounds).

### **2.2. Out of Scope for MVP (To save AI Context)**

* Cloud Sync / Firebase  
* PDF Import/Export  
* OCR / Handwriting Recognition  
* Complex shapes or ruler tools.

## **3\. UI/UX Wireframe & Design System Integration**

*Mapped directly from the provided DESIGN.md to Jetpack Compose tokens.*

### **3.1. Color Tokens (Compose Colors)**

* BackgroundDark: \#171717 (App background, Canvas base)  
* SurfaceDeep: \#0f0f0f (Toolbars, Primary Buttons)  
* BrandGreen: \#3ecf8e (Active tool indicator, brand logo)  
* ActionGreen: \#00c573 (Text links)  
* TextPrimary: \#fafafa  
* TextMuted: \#898989  
* BorderSubtle: \#242424  
* BorderStandard: \#2e2e2e (Card borders, Toolbar borders)  
* BorderHighlight: rgba(62, 207, 142, 0.3) (Active tool border)

### **3.2. Typography (Typography.kt)**

* **Primary Font:** Circular (fallback: standard Sans-Serif).  
* **Technical Font:** Source Code Pro (fallback: Monospace).  
* **Hero/Title:** Weight 400, Line-height 1.0.  
* **Code Label:** 12px, Monospace, Uppercase, 1.2px tracking, \#898989.

### **3.3. Wireframe Topology**

**Screen 1: Dashboard (\#171717 background)**

* **Top Bar:** \#171717 bg. Left: Monospace title "VIBENOTE". Right: Pill Button "New Note" (\#0f0f0f bg, \#fafafa text, 1px solid \#fafafa, 9999px radius).  
* **Grid:** 2-column grid of Note Cards.  
* **Card:** \#171717 bg, 16px radius, 1px solid \#2e2e2e border. Text: \#fafafa title, \#898989 date. No shadows.

**Screen 2: Canvas Workspace (\#171717 background)**

* **Top Toolbar:** \#0f0f0f bg, 1px solid \#2e2e2e bottom border.  
* **Tools:** Ghost buttons (6px radius, transparent bg). Active tool gets a rgba(62, 207, 142, 0.3) border highlight.  
* **Canvas Area:** Full screen. Strokes default to \#fafafa or \#3ecf8e.

## **4\. Technical Requirements Document (TRD)**

* **Language:** Kotlin  
* **UI Framework:** Jetpack Compose (Declarative, perfect for modular AI generation).  
* **Architecture:** MVVM (Model-View-ViewModel).  
* **Local Storage:** \* Room Database (Note metadata: ID, Title, CreatedAt).  
  * File System (GSON/Moshi to serialize drawn paths as SVG path strings or a list of Float coordinate points (X, Y) into JSON files, avoiding SQLite blob limits).  
* **Drawing Engine:** Jetpack Compose Canvas with androidx.compose.ui.graphics.Path.  
* **Dependency Injection:** Hilt (Standardizes module generation for AI).

## **5\. Software Development Architecture (SDA)**

### **5.1. Directory Structure (Clean Architecture)**

com.vibenote.app  
├── core  
│   ├── theme (Color.kt, Type.kt, Theme.kt \- Supabase specs)  
│   └── util (UndoRedoManager.kt)  
├── data  
│   ├── local (NoteDatabase.kt, NoteDao.kt)  
│   └── repository (NoteRepositoryImpl.kt)  
├── domain  
│   ├── model (Note.kt, Stroke.kt)  
│   └── repository (NoteRepository.kt)  
└── presentation  
    ├── dashboard (DashboardScreen.kt, DashboardViewModel.kt)  
    └── canvas (CanvasScreen.kt, CanvasViewModel.kt)

## **6\. OpenCode AI "Vibecoding" Strategy (M1 8GB Optimization)**

**The Core Rule:** Never paste this entire document into OpenCode AI at once during coding. Use it as *your* map. Feed the AI exactly one phase at a time.  
**Context Refresh:** Every time you start a new phase, save the generated code into the exact directories defined in Section 5.1 before clearing the chat. Then, start fresh, pasting only the dependencies it needs.

### **How to prompt OpenCode AI to avoid Hallucinations:**

1. **Define Role:** "You are an expert Android Kotlin developer."  
2. **Define Output:** "Output ONLY the code for![][image1]  
   . Do not explain."  
3. **Provide Constraints:** "Use Jetpack Compose. No XML."  
4. **Inject Design context (when doing UI):** "Strict UI constraint: Background must be \#171717, borders \#2e2e2e, no shadows."

## **7\. Step-by-Step AI Prompting Guide (The Execution Phases)**

### **Phase 1: Foundation & Design System (Tokens: Low)**

**Prompt to AI:**  
"Initialize Jetpack Compose Theme files based on a Supabase dark-mode design system.  
Create Color.kt with: BackgroundDark(0xFF171717), SurfaceDeep(0xFF0F0F0F), BrandGreen(0xFF3ECF8E), ActionGreen(0xFF00C573), TextPrimary(0xFFFAFAFA), TextMuted(0xFF898989), BorderStandard(0xFF2E2E2E).  
Create a PillButton composable: 9999px radius (CircleShape), SurfaceDeep background, TextPrimary text, 1dp solid BorderStandard border. No elevation/shadows."

### **Phase 2: Domain & Data Layer (Tokens: Medium)**

*Clear AI Context.*  
**Prompt to AI:**  
"I am building a note-taking app. Create the Room Database setup in Kotlin.

1. Create a NoteEntity data class with id (UUID), title (String), timestamp (Long), and strokeDataPath (String).  
2. Create NoteDao interface with Insert, Delete, and Get-All (Flow) methods.  
3. Create the Room NoteDatabase abstract class."

### **Phase 3: The Drawing Engine State (Tokens: High \- Critical Phase)**

*Clear AI Context.*  
**Prompt to AI:**  
"Create a CanvasViewModel in Kotlin for an Android drawing app.  
Requirements:

1. State class holding a List\<Stroke\> where Stroke is a data class containing a Compose Path, Color, strokeWidth, and a blend mode (e.g., BlendMode.Clear or BlendMode.SrcOver).  
2. Implement an Undo/Redo stack using two Lists.  
3. Functions: addPath(path: Path), undo(), redo(), clearCanvas().  
   Do not generate the UI yet, just the ViewModel and State holding the path logic."

### **Phase 4: The Canvas UI Composables (Tokens: High)**

*Clear AI Context. Paste the CanvasViewModel generated in Phase 3\.*  
**Prompt to AI:**  
"Using the provided CanvasViewModel, create a Jetpack Compose CanvasScreen.

1. The root background MUST be Color(0xFF171717).  
2. Add a Canvas composable that fills max size.  
3. Use pointerInput with detectDragGestures to capture user touch points and build a Path. On drag end, send the path to the ViewModel.  
4. Iterate over the ViewModel's stroke list and drawPath for each.  
5. Add a Top App Bar with a \#0F0F0F background and a 1dp \#2E2E2E bottom border containing Undo/Redo icon buttons."

### **Phase 5: Dashboard UI (Tokens: Medium)**

*Clear AI Context.*  
**Prompt to AI:**  
"Create a DashboardScreen in Jetpack Compose.

1. Background: \#171717.  
2. LazyVerticalGrid with 2 columns.  
3. Grid items are note cards: \#171717 bg, 16dp RoundedCornerShape, 1dp solid \#2E2E2E border, no elevation/shadow. Title text \#FAFAFA, date text \#898989.  
4. A 'New Note' floating action button using the BrandGreen (\#3ECF8E) as the background."

### **Phase 6: ADB Deployment**

Once the code is assembled using a lightweight IDE like VS Code or Fleet, connect your Android device via USB.

1. Enable Developer Options \-\> USB Debugging on your phone.  
2. Open terminal on Mac: adb devices to verify connection.  
3. Build and install: ./gradlew installDebug.

*End of Blueprint. Follow these phases sequentially to ensure your M1 8GB machine handles the AI context gracefully while strictly adhering to the Supabase UI/UX guidelines.*

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAjsAAABACAYAAAD8rLZwAAAEd0lEQVR4Xu3dW6htUxgH8OEauT0QRXLklhSKN+q4hsiDFxI6yKUUHpS7ECLKpSTKPRTJAyGilIgHlySUy0FScs39FuM7cwx7rLHmXvucNs5++P3qa83xH3POtfZ+WV9zzLVWSgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD/i3Vz7daHAABrwzO5/lrN6i3L9VGXxXi+/VfHXbm+T3PnuGdidvBnmnxdB09OAwBMm9WgPJzr9z7Mvkrjx3ySxvM1MavJCs/n2rkPAQDGrJeGpuLVfqLYLNcLfThDnOulPlwD6+R6KtdjaTjXMZPTq8zXBAEATDkvDc3D4V2+UXncIteV7cQC4lyH9eEaOCfXvmV7vqs7f/QBAMB8vkvTDcVFaW6ZaINcmzRz4bpcK7osrEjT52pF43R9ritybdjNVbE8Vn2bhvNt3mS7puH5Zzki1525dugnijtyndqMozl7INdeTbZfrvtyXdxkY45Mw1Wo5f0EALA09FdPdu/GvV/KY+xzYDuRrSx5Lz6h9VOuJ9PQ5NSlszFtHg1XjN9rsgdzbdqMe7H/uWU7lubiKtDnc9Pp4zQslcV+V+f6NdcuTXZSrs9ynVj2/zrXj2W7dUma/B9cm+vmuWkAYCmoTcdYjbk9DY1LiH0ObeZq9mKXhcjfHMl60XA83mX96xk7roq5D0ay85vxO03en6tm2zTZrSVr1Uan/i/Clmlx9yoBAP+BaALiTfvoJtsz1yPNuHVpebwxTTcAIbL+o+D3l7yKZahoSK5psqq9X6fN4vhY/gq/NXOtsde0U8liKS7EMtqysh35IWW7iuy5Lvum5K0Yv12249ynlwwAWGLqd9q0TkvDUtYsccwbXRbLP/25QmRRt+S6MNcBE7OTYsloTD3HHrmu6uaquk/r7pEsHJfG88iWj2T3NuO4RyeyR9PQsJ2Zhqs6AMASNNYgLGSrNByzdZd/WPJeZNFUrY6x48OzaZiL59i4m6tivr03p2Y/dFl4N00/18kj2T4lq1eGwmUlAwCWuLhRON60X+knFvB6mnuzj2Ww7cp2ZPWelbYZiO23mnHVf8Jr/TS9hFTFvTFxnllNRsz1n5yK7PKyvXIuXpW/3IzDpyVvxXJbzeIG5Gjw9m+y3gl9AACsPTel4U372H5iAXHM02X7iy6/INeO5bE6u8y1omGIe2GquFH6yzS78YpPc/3ch41YAnu/GccVnXje+Dj58bn2buYiP6gZ1yx+PqPPnijb8TMVbX5KMw5xD89ivl8IAPiXPJSGZaVoNuI7beK7bOLj5Le1O80QNyDHm33/xX5nlHxll4d6I3RUPG80O1X8HEV81099LW1T0Yqm5aw+7MQyVjxHXc66oYxf+2ePlLYtWS+y7bvsqJL3TVZcaYr/Wf2b5rsiBQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIv1N5YMDnJM3fatAAAAAElFTkSuQmCC>