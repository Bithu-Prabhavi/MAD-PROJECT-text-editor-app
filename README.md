# MAD Mini-Project — Mobile Text Editor

IS2205: Mobile Application Design & Development — Mini-Project

A native Android text editor built with Kotlin and Jetpack Compose. It supports syntax
highlighting for Kotlin and Markdown, local file management, crash recovery, and a
version history system that stores real diff patches instead of full file copies.

## Features

- **Editor** — create, open, save and "save as" text/Kotlin/Markdown files, with undo/redo
  (up to 25 steps) and word wrap toggle.
- **Search & Replace** — find all matches, jump to next/previous match, replace one match
  or replace all.
- **Syntax Highlighting** — live regex-based highlighting for Kotlin (keywords,
  annotations, strings, comments) and Markdown (headings, bold, italic, inline code),
  plus highlighted search matches.
- **Version History** — every save is stored as a version. The first save stores the full
  file; every save after that stores only a diff patch of what changed, computed with
  `java-diff-utils`.
- **Diff Viewer** — browse past versions of a file and see the stored unified diff
  (colour-coded additions/removals), with the option to restore any version.
- **Crash Recovery & Auto-Save** — the current buffer is auto-saved to a temp file every
  10 seconds and restored automatically if the app is reopened after being closed
  unexpectedly.
- **Read-Only Mode** — toggle the currently open document to read-only for the session.
- **Recent Files** — quick access to recently opened files, backed by Room.

## Tech Stack

| Layer            | Technology                                              |
|-------------------|----------------------------------------------------------|
| Language / UI      | Kotlin, Jetpack Compose (Material 3)                     |
| Architecture       | MVVM (`AndroidViewModel`)                                 |
| Local database     | Room (`AppDatabase`, `EditorDao`)                          |
| Diff engine         | java-diff-utils (`DiffUtils`, `UnifiedDiffUtils`)           |
| File access         | Kotlin File I/O (app-internal storage)                     |
| Concurrency         | Kotlin Coroutines                                          |

## Project Structure

```
app/src/main/java/com/example/mad_mini_project/
├── MainActivity.kt          # Entry point, creates the ViewModel and sets content
├── ui/
│   ├── EditorScreen.kt      # Main editor UI, toolbar, dialogs
│   ├── EditorViewModel.kt   # App state: text, files, undo/redo, search, versions
│   └── DiffViewerDialog.kt  # Version history / diff viewer UI
├── data/
│   ├── AppDatabase.kt       # Room database setup
│   ├── EditorDao.kt         # DB queries for recent files & versions
│   ├── RecentFile.kt        # Entity: recently opened files
│   └── DocumentVersion.kt   # Entity: stored version/diff patches
└── util/
    ├── SyntaxHighlighter.kt # Kotlin/Markdown syntax highlighting (VisualTransformation)
    └── DiffUtilsHelper.kt   # Wraps java-diff-utils to create/apply diff patches
```

## How Version History Works

1. On the first save of a file, the full text is stored as the baseline version.
2. On every save after that, `DiffUtilsHelper.createDiff()` computes a unified diff
   between the last saved state and the new text, and only that patch is stored.
3. To reconstruct any version (or restore one), the app starts from the baseline and
   applies each stored patch in order with `DiffUtilsHelper.applyDiff()`.

## Requirements

- Android Studio (recent stable version)
- Min SDK 24, Target/Compile SDK 36
- Kotlin, Gradle (wrapper included — no separate install needed)

## Getting Started

1. Clone the repository:
   ```
   git clone <repo-url>
   ```
2. Open the project in Android Studio.
3. Let Gradle sync and download dependencies.
4. Run the app on an emulator or physical device (API 24+).
   
   APK Build / Release:  https://drive.google.com/file/d/1dGsGvaGoEz2vuFnzSU-jVl8BEXG5O3Dq/view?usp=sharing
                         https://github.com/Bithu-Prabhavi/MAD-PROJECT-text-editor-app/releases/tag/release-apk-v1

## Team

- T.W Abeysekera - 24020036
- B.P Kandanage - 24020559
- M.L Basnayaka - 24020141

## Course

IS2205 — Mobile Application Design & Development, University of Colombo School of Computing (UCSC).
