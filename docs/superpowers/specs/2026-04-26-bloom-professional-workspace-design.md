# Design Spec - Bloom Professional Workspace

## 1. Overview
Bloom is evolving from a simple canvas into a high-powered "Digital Research Lab" workspace. This design focuses on a dual-pane research environment, layered note-taking (text + ink), and a robust hybrid organizational system (folders + tags) to support students, researchers, and technical professionals.

## 2. Information Architecture
### 2.1 Hybrid Organization
- **Folders**: High-level hierarchical containers for "Projects" or "Courses." Notes must belong to exactly one folder (defaults to "General").
- **Tags**: Multi-select labels for cross-project context (e.g., `#research`, `#priority-1`, `#final-draft`).
- **Workspace Navigation**: A collapsible navigation drawer or sidebar providing quick access to Folders, Tags, and Favorites.

### 2.2 Data Model Extensions
- **NoteEntity**:
    - `folderId`: String (UUID of the parent folder)
    - `tags`: List<String> (serialized for Room)
    - `sourceUri`: String (Optional URI for the linked reference file)
    - `contentJson`: String (Structured text content, e.g., Markdown or JSON)
- **FolderEntity**:
    - `id`: String (UUID)
    - `name`: String
    - `parentId`: String (Optional, for nested folders)

## 3. User Experience & Components
### 3.1 The Dashboard (Library View)
- **Multi-select Mode**: Long-press to select multiple notes for bulk moving (to folders) or tagging.
- **Filter Chips**: Dynamic chips at the top of the grid for active filtering by tag or status.
- **Enhanced Search**: Global search indexing titles, tags, and text content.

### 3.2 The Research Editor (Split-View)
- **Split-Pane Layout**: A dual-pane interface with a resizable divider.
- **Reference Pane (Left)**: A dedicated viewer for images (PNG/JPG) and eventually PDFs. Supports independent pan and zoom.
- **Workspace Pane (Right)**: The active note editor.
- **Layered Input System**:
    - **Base Layer**: A structured text editor (Markdown-capable) for high-speed typing.
    - **Overlay Layer**: A transparent ink canvas for freehand drawing, circling text, and sketching.

### 3.3 Productivity Features
- **Command Palette**: `Ctrl/Cmd + K` style quick-jump for researchers to find notes or tags without leaving the keyboard.
- **Local Attachment Manager**: Logic to import/copy reference files into the app's internal storage to ensure persistent links.

## 4. Technical Implementation
- **Room Migrations**: Version 5->6 to add folder/tag support and new content fields.
- **Jetpack Compose**: Use `HorizontalPager` or custom `Layout` for the split-view divider.
- **Serialization**: Use `kotlinx.serialization` for the complex `contentJson` field.

## 5. Security & Privacy
- **100% Local**: No cloud sync. All data, including imported reference documents, remains in the app's internal `filesDir`.
- **Encrypted Export**: (Future) Option to password-protect library backups.
