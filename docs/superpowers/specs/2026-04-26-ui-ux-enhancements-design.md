# Design Spec - Bloom UI/UX Enhancements

## 1. Dashboard Enhancements
- **Empty State**: Replace current "No notes yet" text/icon with a custom "scribble" illustration and a prominent "Start blooming" CTA.
- **Note Previews**: Render a miniature version of the entire canvas on each `NoteCard`.
- **Layout & Polish**: Refine the standard 2-column grid with improved spacing and subtle card elevations.
- **Search Experience**: Add smooth transition animations for search bar entry/exit.

## 2. Canvas Editor Enhancements
- **Toolbar Refinement**: Polished docked top bar with improved icon states and alignment.
- **Feedback**: Integrate haptic feedback for successful shape recognition.
- **Navigation**: Implement momentum-based pan/zoom and a double-tap-to-reset transformation.

## 3. General Features
- **Onboarding**: A pre-loaded "Welcome" note demonstrating shape recognition and tools.
- **Theming**: Add support for a Light Mode toggle in the application settings.

## 4. Architecture & Components
- **DashboardScreen**: Update `NoteCard` to include a canvas preview composable.
- **CanvasScreen**: Optimize toolbar layout and gesture handling.
- **Theme**: Expand `VibeColors` to support dynamic light/dark palettes.

## 5. Testing Strategy
- **Visual Regression**: Verify preview rendering across different note contents.
- **Interaction**: Test gesture responsiveness and haptic triggers.
- **Theme**: Validate color contrast in both Light and Dark modes.
