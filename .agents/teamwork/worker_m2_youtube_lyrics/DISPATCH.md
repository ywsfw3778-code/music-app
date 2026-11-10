# Dispatch for Worker — Milestone 2 (YouTube Search, MP3 Download & Synced Lyrics)

You are `worker_m2_youtube_lyrics` (Archetype: teamwork_worker).
Your working directory is: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m2_youtube_lyrics\`
Parent conversation ID: 82c05d99-56aa-4fc3-9440-63d76af0b05f

Read:
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_features\handoff.md`

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Exclusively Owned Files
- `lib/services/youtube_service.dart` (New)
- `lib/models/lyric_line.dart` (New)
- `lib/services/lyrics_service.dart` (New)
- `lib/ui/views/youtube_search_view.dart` (New)
- `lib/ui/widgets/synced_lyrics_widget.dart` (New)
- `test/lyrics_service_test.dart` (New)
- `test/youtube_service_test.dart` (New)

## Requirements to Implement:
1. `lib/models/lyric_line.dart`:
   - `LyricLine` model with `Duration timestamp`, `String text`.
   - `LrcParser.parse(String lrcContent)` method parsing `[mm:ss.xx] text` and sorting by timestamp.
2. `lib/services/lyrics_service.dart`:
   - Fetches synced lyrics from `https://lrclib.net/api/get` (or `/api/search` with title and artist) with CORS-friendly headers.
   - Parses the `syncedLyrics` LRC string into `List<LyricLine>`.
   - Returns empty list if no lyrics found or on network timeout.
3. `lib/services/youtube_service.dart`:
   - Calls companion server `http://localhost:8081/api/youtube/search?q=<query>` for search results.
   - Calls companion server `POST http://localhost:8081/api/youtube/download` with `{id: videoId, title: videoTitle}`.
   - Returns the created `SongModel` and calls `musicProvider.prependSong(song)` so the track immediately appears at index 0 of the library.
4. `lib/ui/views/youtube_search_view.dart`:
   - Clean, dark AMOLED search view.
   - Search input bar, debounced search query, candidate list with title, author/channel, duration, and thumbnail.
   - Download button with downloading spinner/progress indicator.
   - Upon download completion, notifies user and adds song to top of playlist.
5. `lib/ui/widgets/synced_lyrics_widget.dart`:
   - Smoothly auto-scrolling synced lyrics view.
   - Listens to active position stream.
   - Highlights the active lyric line with bold text and scale transition.
   - Dimmed opacity for upcoming and past lines.
6. STRICT NEGATIVE CONSTRAINT: Absolutely NO equalizer controls, sliders, or filters anywhere.
7. Verification:
   - Run `flutter analyze` — 0 errors, 0 warnings.
   - Run `flutter test` — all existing and new tests must pass cleanly.

Write your handoff report to `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m2_youtube_lyrics\handoff.md` and notify parent via `send_message`.


## 2026-11-10T15:30:44Z
Resume work at c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m2_youtube_lyrics\. Read c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_features\handoff.md, and DISPATCH.md. Implement Milestone 2: YouTube Search, MP3 Download to E:\music (newest-first prepend to index 0), and Synced Lyrics (LRCLIB client, LRC parser, auto-scroll lyrics widget). Verify with flutter analyze and flutter test. Write your handoff report to handoff.md in your working directory and notify me via send_message when done.
