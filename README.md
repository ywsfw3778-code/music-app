# سُكون - Nocturne (Kotlin + Jetpack Compose)

A native Android music player built with Kotlin, Jetpack Compose, Material 3, Media3 (`ExoPlayer` / `MediaSessionService`), and a Super AMOLED pure black (`#000000`) & translucent water-drop glass UI.

## Features

- **Local Music Library**: Scans device audio via `MediaStore.Audio.Media` with fast local caching, search, and sorting (Recent, Title, Artist, Duration).
- **Playlists & Favorites**: Create, edit, search-pick, delete, and drag-reorder playlists, plus one-tap favorites.
- **Artists & Albums**: Automatic artist normalization (`ArtistNormalizer`), junk-album filtering, and online cover/photo fetching via Deezer & iTunes (`OnlineMetadataResolver`) with local disk caching.
- **Background Playback & Dual-Deck Crossfade**: `PlaybackService` (`MediaSessionService`) paired with `PlayerViewModel` for equal-power crossfade transitions, queue management, repeat modes, shuffle, and sleep timer.
- **Synced & Plain Lyrics**: Automatic and manual search via LRCLIB (`LyricsRepository`), Franco-Arabic normalization, local `.lrc` file import, tap-to-seek lines, and timing offset adjustments (`-0.5s` / `+0.5s`).
- **Smart Filtering & Folders**: Auto-exclude recordings, minimum duration threshold, custom keyword filters, quarantine restore page, and per-folder exclusion controls.
- **Bilingual & Adaptive Appearance**: Full Arabic (RTL) and English support, dynamic cover-extracted accent colors, and configurable artwork/blur backdrop modes.

## Project Structure

- `app/src/main/java/com/example/` — Application entry (`MainActivity`), `PlayerViewModel`, `PlaybackService`, `data/`, `model/`, `ui/` (`components/`, `screens/`, `theme/`)
- `app/src/main/res/` — Strings, adaptive launcher icons, fonts, and themes
- `app/src/test/` — Unit and Robolectric tests

## Build & Test

- `applicationId`: `com.aistudio.nocturne.kxmpzq`
- `minSdk`: `24`, `targetSdk`: `36`, `compileSdk`: `36`
- Run unit tests: `gradle :app:testDebugUnitTest`
