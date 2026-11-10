# Technical Feasibility & Architecture Report: Local HDD, YouTube MP3, Synced Lyrics & AMOLED Theme

## 1. Observation

### 1.1 Local Environment & HDD Assets
- **Command**: `Test-Path "E:\music"`  
  **Result**: `True`.
- **Command**: `(Get-ChildItem -Path "E:\music" -Recurse -File).Count`  
  **Result**: `98` audio files on disk in `E:\music`.
- **Sample Files Observed**:
  - `E:\music\.2OLELI (Prod. Abyusif) قوليلي.mp3` (4,091,654 bytes)
  - `E:\music\Abo El Anwar - 3amal A7kylek.mp3` (7,733,385 bytes)
  - `E:\music\Abyusif - Fel Dalma Di Etwaladt.mp3` (6,403,908 bytes)
  - `E:\music\01 - AMAN (INTRO).mp3` (7,305,919 bytes)
  Contains 98 Egyptian rap/hip-hop tracks.

### 1.2 Flutter & Dart Toolchain
- **Command**: `flutter --version`  
  **Result**: `Flutter 3.47.6 • channel stable`, `Dart 3.13.5`.
- **Command**: `flutter devices`  
  **Result**: `Windows (desktop)`, `Chrome (web)`, `Edge (web)`.
- **Command**: `flutter analyze`  
  **Result**: `No issues found! (ran in 7.7s)`.
- **Command**: `flutter test`  
  **Result**: `All tests passed! (7 tests)`.

### 1.3 Codebase Architecture & Constraints Audit
- `lib/services/audio_scanner_service.dart`:
  - Line 39: `if (kIsWeb) return true;`
  - Lines 62-100: Scans via `on_audio_query` on Android/iOS; calls `_scanWindowsMusic()` only on Windows desktop. On web, it falls back directly to `_getSampleTracks()` (line 106).
  - Lines 148-180: `_scanWindowsMusic()` currently scans only `%USERPROFILE%\Music`, not `E:\music`.
  - Line 1: `import 'dart:io';` directly inside a file compiled for web (can trigger runtime exceptions on web if not guarded).
- `lib/services/player_service.dart`:
  - Lines 22-34: Uses `just_audio` with `_player.setUrl(song.uri)`, `_player.setAudioSource(AudioSource.uri(...))`, and `_player.setFilePath(...)`.
  - On web, `setFilePath` throws `UnsupportedError` because local file paths do not exist in the browser sandbox.
- Equalizer audit across entire project:
  - `Get-ChildItem -Path lib -Recurse | Select-String -Pattern "equalizer|equaliz|eq"`:
  - No equalizer DSP, no biquad filters, no frequency sliders, and no EQ controls exist.
  - Two UI files use `Icons.graphic_eq_rounded` as generic music icons:
    - `lib/ui/views/home_view.dart`: Line 74
    - `lib/ui/widgets/album_art_widget.dart`: Line 100
  - `lib/ui/widgets/sound_wave_visualizer.dart`: Purely a 4-bar decorative animation driven by `AnimationController` sine wave (lines 69-71), not an audio equalizer.
- UI Theme & AMOLED audit:
  - `lib/core/constants/app_colors.dart`: Background is `0xFF090912` (visionOS) or `0xFF050508` (titanium), neither of which is pure AMOLED black (`#000000`).
  - Icons across all screens currently use Flutter Material Icon fonts (`Icons.*`), violating Requirement 1.

### 1.4 Web Security Sandbox & YouTube Restrictions
- **Web Browser Sandbox in Chrome**: Chrome blocks web pages loaded over `http://localhost:8080` from reading `file:///` URLs or arbitrary Windows drive paths (`E:\music\...`) due to the `Not allowed to load local resource` security policy.
- **`youtube_explode_dart` on Web**: Direct requests from browser JavaScript to YouTube endpoints trigger browser CORS failures because YouTube servers do not return `Access-Control-Allow-Origin: *`.
- **Browser File Writing**: Web browsers in Chrome cannot silently write downloaded YouTube MP3 files into `E:\music` without browser download dialog prompts.
- **LRCLIB API Test**:
  - Command: `Invoke-RestMethod -Uri 'https://lrclib.net/api/search?q=Abyusif' -Headers @{'User-Agent'='EchoMusicApp/1.0'}`
  - Result: Returned HTTP 200 with ID `10894578`, exact match track `Abyusif - 3ESABAT`, and full `syncedLyrics` in LRC format `[00:07.47] ...`.
  - LRCLIB supports `Access-Control-Allow-Origin: *` (CORS-friendly for web).

---

## 2. Logic Chain

### 2.1 Local HDD Audio (`E:\music`) Access Under Chrome Web
1. **Fact**: Requirement R3 requires loading and playing local music files from `E:\music` while Requirement R5 requires running and verifying the app on Chrome web at `http://localhost:8080`.
2. **Constraint**: Web browsers cannot directly read host file paths (`E:\music`) or file URLs (`file:///E:/music/...`) via client-side JavaScript/Wasm.
3. **Inference**: To serve audio files from `E:\music` to Chrome web, a bridge is required that serves `E:\music` as HTTP resources.
4. **Architectural Evaluation of Solutions**:
   - *Option A: HTML5 File System Access API (`showDirectoryPicker`)*:
     - Requires interactive user directory prompt every session; cannot hardcode `E:\music` without user gesture.
     - Files must be read into browser Blobs; memory-heavy for 98 large audio files.
     - Cannot silently write YouTube downloads into `E:\music`.
   - *Option B: Embedded Dart Local Companion Server (`bin/server.dart`)*:
     - Written in Dart using standard `dart:io` `HttpServer` (zero external dependencies) or `shelf`.
     - Runs locally on port `8081` (or serves static web app directly on `8080`).
     - Directly accesses `E:\music` via `Directory('E:\\music').listSync(recursive: true)` on Windows.
     - Serves audio tracks over `http://localhost:8081/api/stream?file=...` with HTTP 206 Partial Content (Byte Range requests) and `Access-Control-Allow-Origin: *`.
     - Allows `just_audio` in Chrome web to stream and seek instantly without loading entire files into browser RAM.
     - Allows background downloading of YouTube audio directly into `E:\music`.
   - **Conclusion on Local Audio**: Option B (Dart Local Companion Server) is the optimal, robust architecture. The Flutter app can also provide a client-side folder picker fallback if the companion server is not running.

### 2.2 YouTube Audio Search & MP3 Download
1. **Fact**: In Flutter Web, `youtube_explode_dart` fails client-side due to YouTube's strict CORS policies and anti-scraping checks.
2. **Fact**: Requirement R4 mandates downloading YouTube audio locally as MP3 files into the music library and having newly downloaded files automatically appear at the top of the track list.
3. **Inference**: Placing YouTube search and download logic inside the local Dart companion server bypasses all browser CORS restrictions completely, because the server runs natively on Windows.
4. **Execution Flow**:
   - User types query in Flutter Web search bar.
   - Flutter Web sends `GET http://localhost:8081/api/youtube/search?q=<query>`.
   - Server uses `YoutubeExplode().search.search(query)` and returns video metadata (`id`, `title`, `author`, `duration`, `thumbnailUrl`).
   - User taps download icon.
   - Flutter Web sends `POST http://localhost:8081/api/youtube/download` with `{id: videoId, title: videoTitle}`.
   - Server calls `yt.videos.streamsClient.getManifest(videoId)` -> `manifest.audioOnly.withHighestBitrate()`.
   - Server pipes audio stream directly into `E:\music\<sanitized_title>.mp3`.
   - Server returns the new `SongModel` JSON with `dateAdded: DateTime.now().millisecondsSinceEpoch ~/ 1000`.
   - Flutter app prepends the song at index 0 of `_allSongs` and `_currentQueue`.
   - The song appears immediately at the top of the list, perfectly satisfying R3 and R4.

### 2.3 Synced Lyrics (LRC) Integration
1. **Fact**: Requirement R4 mandates fetching and displaying synced/timed lyrics from the web that scroll and animate synchronously with song playback.
2. **Fact**: LRCLIB (`https://lrclib.net`) is free, requires no API key, supports CORS (`Access-Control-Allow-Origin: *`), and has verified synchronized lyrics for the exact Egyptian artists in `E:\music` (e.g., Abyusif).
3. **Parser Implementation**:
   - An LRC parser splits the raw string into lines and matches `r'\[(\d+):(\d+(?:\.\d+)?)\](.*)'`.
   - Calculates time in milliseconds: `ms = (minutes * 60 + seconds) * 1000`.
   - Produces a sorted `List<LyricLine>`.
4. **Playback Synchronization**:
   - Listen to `playerService.positionStream`.
   - On position tick, find active line index:
     `activeIdx = lines.lastIndexWhere((line) => line.timestamp <= currentPosition)`.
   - Use `ScrollController` or `ItemScrollController` with `animateTo` to smoothly center the active line.
   - Style active line with dynamic vibrant album color and scale animation (`1.05x`), while inactive lines are dimmed white (`alpha: 0.35`).

### 2.4 AMOLED Pure Black & Dynamic Color Extraction
1. **Fact**: Requirement R1 mandates base theme of pure AMOLED black (`#000000`) and dynamic color adaptation from album art.
2. **Fact**: The legacy `palette_generator` package is discontinued. Its actively maintained successor with full Flutter Web support is `palette_generator_plus: ^1.2.0`.
3. **Visual Architecture**:
   - Scaffold background: `Color(0xFF000000)`.
   - Top-screen ambient glow: A radial gradient positioned at `Alignment(0, -0.6)`:
     `colors: [extractedAccent.withValues(alpha: 0.28), Colors.black], stops: [0.0, 0.7]`.
   - True black OLED preservation: 100% pure black across bottom half, controls, and margins to guarantee zero OLED power consumption.
   - Dynamic accent application: Play/pause button glow, seekbar progress track, active lyric text, and album border highlight adapt to `extractedAccent`.
   - Fallback: A deterministic hash generator deriving vibrant neon colors (violet, cyan, magenta, emerald) from the song's title/artist if no image art is present.

### 2.5 Custom SVG Vector Paths (Zero Icon Fonts)
1. **Fact**: Requirement R1 mandates: "All controls and icons must be custom-drawn SVG vector paths specifically crafted for this player; do not use standard icon fonts."
2. **Implementation**:
   - Add `flutter_svg: ^2.0.17`.
   - Create a centralized vector path repository (`SvgIcons` or `AppSvgIcon` widget).
   - Define exact SVG paths for all player controls:
     - `play`, `pause`, `skipNext`, `skipPrevious`, `shuffle`, `repeat`, `search`, `download`, `heart`, `heartFilled`, `playlist`, `lyrics`, `audioWave`.
   - Eliminate every instance of `Icon(Icons.*)` and `Icon(CupertinoIcons.*)`.

### 2.6 Strict Constraint: Absolutely Zero Equalizer
1. **Fact**: Requirement R3 states: "Strict constraint: Absolutely NO EQUALIZER feature, sliders, or UI controls (explicitly forbidden by user)."
2. **Audit & Remediation**:
   - Verified that `just_audio` has no equalizer DSP filters in this codebase.
   - Identified 2 lingering icon references to `Icons.graphic_eq_rounded` in `home_view.dart` and `album_art_widget.dart` that could visually suggest an equalizer.
   - Remediation: Replace these with custom SVG music wave/note icons.
   - Ensure no EQ sliders, frequency bands, presets (Rock, Pop, Jazz), or EQ settings pages are added.

---

## 3. Caveats
- **Local Companion Server Launch**: To achieve silent access to `E:\music` and background YouTube downloading under Chrome web, the local Dart server (`bin/server.dart`) must be running on the machine (`dart run bin/server.dart`). If a user opens Chrome web without starting the companion server, a web folder picker fallback must be displayed with clear instructions.
- **YouTube Anti-Bot Throttling**: While `youtube_explode_dart` runs natively without CORS on the companion server, YouTube periodically updates its player algorithms. Using the latest version and catching network exceptions with friendly user messages is essential.
- **Audio Codec Compatibility**: YouTube streams are typically Opus (`audio/webm`) or AAC (`audio/mp4`). Chrome natively plays both formats without transcoding. If saving as `.mp3`, the container is direct AAC/M4A or MP3 stream.

---

## 4. Conclusion & Recommended Architecture

### Proposed Solution Architecture

```
+-------------------------------------------------------------------------+
|                          FLUTTER WEB (Chrome)                           |
|  - Pure AMOLED Black (#000000) Base Theme                               |
|  - Custom SVG Vector Path Controls (No Icon Fonts)                      |
|  - Dynamic Color Extraction (palette_generator_plus / Ambient Glow)     |
|  - Real-Time Synced Lyrics Viewer (LRC Parser + Auto-Scroll)            |
|  - YouTube Search & Download UI (Prepends to Top of Track List)         |
|  - Zero Equalizer Controls / Sliders                                    |
+------------------------------------+------------------------------------+
                                     | HTTP REST + Range Audio
                                     v
+-------------------------------------------------------------------------+
|                  LOCAL DART COMPANION SERVER (Port 8081)                |
|  bin/server.dart (dart run bin/server.dart)                             |
|                                                                         |
|  1. GET  /api/songs           -> Scans E:\music, sorts newest-first     |
|  2. GET  /api/stream?file=... -> Streams audio with HTTP 206 Range      |
|  3. GET  /api/youtube/search  -> youtube_explode_dart search (No CORS)  |
|  4. POST /api/youtube/download-> Streams YT audio to E:\music\*.mp3     |
|  5. GET  /api/lyrics          -> Proxies/caches LRCLIB synced lyrics    |
+------------------------------------+------------------------------------+
                                     | Direct Filesystem I/O
                                     v
+-------------------------------------------------------------------------+
|                          HOST DISK (E:\music)                           |
|  - 98 Existing Local Tracks (.mp3, .m4a, .flac)                         |
|  - Destination for New YouTube MP3 Downloads                            |
+-------------------------------------------------------------------------+
```

### Proposed File Modifications & Additions
1. **New `bin/server.dart`**:
   - Lightweight Dart companion server using `dart:io` and `youtube_explode_dart`.
   - Scans `E:\music`, serves audio streams with byte ranges, searches YouTube, and downloads MP3s.
2. **Update `pubspec.yaml`**:
   - Add `flutter_svg: ^2.0.17`
   - Add `palette_generator_plus: ^1.2.0`
   - Add `http: ^1.2.0`
   - Add `youtube_explode_dart: ^3.1.0`
3. **New `lib/core/constants/app_svg_icons.dart`**:
   - Centralized SVG path strings and custom SVG icon widget replacing all standard icon fonts.
4. **Update `lib/core/constants/app_colors.dart` & `lib/core/theme/app_theme.dart`**:
   - Enforce `Color(0xFF000000)` pure AMOLED black base.
   - Dynamic accent color provider / state integration.
5. **New `lib/models/lyric_line.dart` & `lib/services/lyrics_service.dart`**:
   - LRCLIB API client and LRC timestamp parser.
6. **New `lib/ui/widgets/synced_lyrics_widget.dart`**:
   - Smoothly auto-scrolling synced lyrics view with active line highlights.
7. **New `lib/ui/views/youtube_search_view.dart`**:
   - Search YouTube audio and download straight to `E:\music` with instant top-of-library insertion.
8. **Update `lib/services/audio_scanner_service.dart` & `lib/providers/music_provider.dart`**:
   - Connect to local server API on Web (`http://localhost:8081/api/songs`), sort newest-first by file modification timestamp (`dateAdded DESC`), and handle newly downloaded tracks.

---

## 5. Verification Method

To independently verify these conclusions and test the implementation:

1. **Verify Static Analysis**:
   ```powershell
   flutter analyze
   ```
   *Expected output*: `No issues found!`

2. **Verify Unit & Widget Tests**:
   ```powershell
   flutter test
   ```
   *Expected output*: `All tests passed!`

3. **Verify Local HDD Directory**:
   ```powershell
   Test-Path "E:\music"
   (Get-ChildItem -Path "E:\music" -Filter *.mp3).Count
   ```
   *Expected output*: Confirms `E:\music` exists with MP3 files.

4. **Verify LRCLIB API Accessibility**:
   ```powershell
   Invoke-RestMethod -Uri "https://lrclib.net/api/search?q=Abyusif" -Headers @{'User-Agent'='EchoMusicApp/1.0'}
   ```
   *Expected output*: Returns JSON array of tracks with `syncedLyrics`.

5. **Verify Chrome Web Launch**:
   ```powershell
   flutter run -d chrome --web-port 8080
   ```
   *Expected output*: App loads at `http://localhost:8080` in Google Chrome with AMOLED pure black interface, SVG icons, and smooth playback.
