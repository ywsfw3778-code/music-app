# Handoff Report: Milestone 2 — YouTube Search, MP3 Download & Synced Lyrics

## 1. Observation

### 1.1 Tool Commands and Verification Outputs
- **Static Analysis**:
  - Command: `flutter analyze`
  - Output:
    ```
    Analyzing music app...
    No issues found! (ran in 8.5s)
    ```
- **Automated Test Suite**:
  - Command: `flutter test`
  - Output:
    ```
    00:07 +58: All tests passed!
    ```
    Total: 58 tests passed (36 previous tests + 22 new tests covering Milestone 2 components).
- **Strict Constraint: Zero Equalizer Audit**:
  - Command: `Get-ChildItem -Path lib, bin, test -Recurse -File | Select-String -Pattern 'equalizer|equaliz'`
  - Output: Matches occur exclusively inside automated test assertions verifying the total absence of equalizer code/controls. Zero equalizer sliders, DSP biquad filters, EQ presets, or equalizer UI exist in the codebase.

### 1.2 Created and Modified Files
- **Created**:
  - `lib/models/lyric_line.dart` (Lines 1-89): Defines `LyricLine` with `Duration timestamp`, `String text`, `compareTo`, `operator ==`, `hashCode`, and `LrcParser.parse(String?)`.
  - `lib/services/lyrics_service.dart` (Lines 1-125): LRCLIB API client implementing exact `/api/get` match with `/api/search` query fallback, track/artist title sanitization (`cleanTrackTitle`), and in-memory caching.
  - `lib/services/youtube_service.dart` (Lines 1-137): Client for companion server endpoints `GET /api/youtube/search?q=<query>` and `POST /api/youtube/download`, returning `SongModel` and calling `musicProvider.prependSong(song)` to immediately position newly downloaded MP3s at index 0.
  - `lib/ui/views/youtube_search_view.dart` (Lines 1-473): Pure AMOLED `#000000` search interface with search input bar, debounced queries (600ms), video thumbnail/author/duration cards, per-item download spinners, checkmark indicators, and play-now SnackBar actions.
  - `lib/ui/widgets/synced_lyrics_widget.dart` (Lines 1-278): Real-time auto-scrolling synced lyrics viewer listening to `MusicProvider.currentPosition`, centering active line via `ScrollController.animateTo`, applying `1.08x` scale transition and primary color glow, dimming inactive lines to 35% opacity, and supporting tap-to-seek.
  - `test/lyrics_service_test.dart` (Lines 1-235): 11 tests covering `LrcParser` (standard `[mm:ss.xx]`, 3-digit ms `[mm:ss.xxx]`, 1-digit fractions, multi-timestamp lines, unordered sorting, metadata tag skipping, empty input), `LyricsService` (exact match, search fallback, error tolerance, caching), and `SyncedLyricsWidget` (empty state, tap-to-seek).
  - `test/youtube_service_test.dart` (Lines 1-269): 11 tests covering `YouTubeSearchResult` serialization, `YouTubeService` search, HTTP errors, download and `prependSong` invariant verification, `YouTubeSearchView` rendering and download flow, and zero-equalizer constraint verification.
- **Modified**:
  - `lib/ui/views/home_view.dart`: Added YouTube search icon button in SliverAppBar actions.
  - `lib/ui/widgets/now_playing_sheet.dart`: Added synced lyrics sheet modal trigger in the top bar.

---

## 2. Logic Chain

1. **LRC Parsing Architecture**:
   - *Premise*: Lyrics on LRCLIB and in `.lrc` files use various timestamp precision formats (`[01:23.45]`, `[01:23.456]`, `[01:23.4]`) and often duplicate timestamps across repeating chorus lines.
   - *Inference*: `LrcParser.parse` regex `r'\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]'` captures minutes, seconds, and centiseconds/milliseconds, normalizes the fractional part to milliseconds, duplicates lines for multiple timestamps on one row, skips non-timestamp metadata (`[ti:...]`, `[ar:...]`), and performs chronological sorting by `Duration`.
   - *Proof*: Verified by tests in `test/lyrics_service_test.dart` lines 18-97.

2. **Synced Lyrics Real-Time Scrolling & UX**:
   - *Premise*: Requirement R4 requires displaying synced/timed lyrics that scroll and animate synchronously with song playback.
   - *Inference*: `SyncedLyricsWidget` listens to `provider.currentPosition`, computes the last index where `lyric.timestamp <= currentPosition`, scales and highlights the active line with text shadows, dims past/future lines, and computes viewport-centered scroll offsets. If the user touches or drags the list, a 4-second auto-scroll suspension timer activates so the user's manual inspection is not abruptly overridden.
   - *Proof*: Verified by widget tests in `test/lyrics_service_test.dart` lines 185-233.

3. **YouTube Search & Local MP3 Download Workflow**:
   - *Premise*: Under Chrome web sandbox, direct YouTube scraping triggers CORS blocks, and direct filesystem writes to `E:\music` are prohibited without user download prompts.
   - *Inference*: `YouTubeService` delegates to native companion server `http://localhost:8081` (`/api/youtube/search` and `POST /api/youtube/download`). The companion server downloads the highest bitrate audio directly into `E:\music/<title>.mp3`, generates deterministic metadata and stream URL, and returns the created `SongModel`.
   - *Inference*: `YouTubeService.downloadSong` immediately calls `musicProvider.prependSong(song)`, guaranteeing that the new MP3 file is placed at index 0 of `allSongs` and `currentQueue` (newest-first).
   - *Proof*: Verified by unit and widget tests in `test/youtube_service_test.dart` lines 86-138 and 188-248.

4. **Negative Constraint Enforcement**:
   - *Premise*: Strict user constraint requires absolutely NO equalizer controls, sliders, or filters anywhere in the app.
   - *Inference*: No equalizer packages, biquad filters, or frequency controls were added. Automated tests in `test/youtube_service_test.dart` and `test/music_logic_test.dart` assert that no sliders or equalizer keywords exist in UI or business logic.

---

## 3. Caveats

- **Companion Server Dependency for YouTube Operations**:
  - Live downloading and searching of YouTube videos requires the local companion server to be running (`dart run bin/server.dart`).
  - If the server is offline or unreachable, `YouTubeService` throws a typed `YouTubeServiceException` which `YouTubeSearchView` catches to display a friendly retry interface without crashing.
- **LRCLIB Availability**:
  - LRCLIB is a public free API supporting CORS. If an obscure or unlisted track has no synced lyrics on LRCLIB, `LyricsService` falls back from `/api/get` to `/api/search`, and if still not found, gracefully returns an empty list triggering the friendly UI empty state ("لا توجد كلمات متزامنة متاحة لهذه الأغنية").

---

## 4. Conclusion

Milestone 2 (YouTube Search, MP3 Download to `E:\music` with newest-first prepend to index 0, and Synced Lyrics with LRCLIB client and auto-scrolling widget) is complete, robust, and verified:
- `LyricLine` and `LrcParser` handle all timestamp variations and chronological ordering.
- `LyricsService` cleanly interfaces with LRCLIB with exact and fuzzy search plus caching.
- `YouTubeService` searches, downloads MP3s via companion server, and prepends songs to index 0.
- `YouTubeSearchView` provides an AMOLED dark UI with debounced search, candidate cards, and download indicators.
- `SyncedLyricsWidget` provides real-time position tracking, active line scaling, dimmed inactive lines, and tap-to-seek.
- Static analysis: 0 errors, 0 warnings (`flutter analyze`).
- Tests: 58/58 tests passing (`flutter test`).
- Strict negative constraint: Zero equalizer code verified.

---

## 5. Verification Method

To independently verify the implementation:

1. **Run Static Analysis**:
   ```powershell
   flutter analyze
   ```
   *Expected result*: `No issues found!`

2. **Run All Unit & Widget Tests**:
   ```powershell
   flutter test
   ```
   *Expected result*: `All tests passed! (58 tests)`.

3. **Run Milestone 2 Targeted Tests**:
   ```powershell
   flutter test test/lyrics_service_test.dart test/youtube_service_test.dart
   ```
   *Expected result*: `All tests passed! (22 tests)`.

4. **Verify Zero Equalizer Constraint**:
   ```powershell
   powershell -Command "Get-ChildItem -Path lib, bin, test -Recurse -File | Select-String -Pattern 'equalizer|equaliz'"
   ```
   *Expected result*: Only matches are inside automated test assertions verifying zero equalizer presence.
