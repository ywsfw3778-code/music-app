# Project: Natural AMOLED Music Player (Flutter Web)

## Architecture
The application runs as an authentic mobile-framed Flutter Web music player on Chrome (`http://localhost:8080`) backed by a lightweight native Dart Companion Server (`bin/server.dart` on `http://localhost:8081`).

```
+-------------------------------------------------------------------------+
|                          FLUTTER WEB (Chrome)                           |
|  - Pure AMOLED Black (#000000) Base Theme                               |
|  - Soft Blurred Album Art Full-Screen Dynamic Background                |
|  - Natural Adaptive Tinting for Buttons, Slider & Icons from Album Cover|
|  - Clean Natural Mobile Layout (Apple Music / Spotify clean feel)       |
|  - Custom SVG Vector Path Controls (No Icon Fonts)                      |
|  - Smooth Audio Crossfading (3s-8s seamless gapless transitions)        |
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

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Pure AMOLED Black Base | Base background locked to true pitch black `#000000` with 0% OLED subpixel emission | M3 | R1 & User Update |
| 2 | Dynamic Blurred Album Art Backdrop | When any song plays, its album cover becomes the ambient softly-blurred backdrop over pure black AMOLED | M3 | User Follow-up |
| 3 | Natural Adaptive Accent Extraction | Dominant colors extracted directly from album cover dynamically tint buttons, seekbar, and icons (NO cyber neon) | M3 | User Follow-up |
| 4 | Natural Authentic Player Layout | Clean, uncluttered Apple Music / Spotify style mobile layout: prominent rounded cover, title, artist, seekbar, controls | M3 | User Follow-up |
| 5 | Custom Vector SVG Artwork | Clean, elegant SVG vector paths for player controls (play, pause, skip, shuffle, repeat, search, download, lyrics, heart) | M3 | R1 |
| 6 | Smooth Opening & Transition Animations | Smooth album art transitions, gentle spring on play/pause, fluid page switches | M3 | R1 |
| 7 | Local HDD Playback (`E:\music`) | Scan and stream 98+ local music tracks from `E:\music` over HTTP 206 Range bridge | M1 | R3 |
| 8 | Newest-First Library Sorting | Display playlist with newly added/modified/downloaded songs at the very top | M1 | R3 |
| 9 | Strict Zero Equalizer Constraint | Absolutely NO equalizer feature, sliders, EQ presets, or UI controls | All/Audit | R3 |
| 10| Smooth Audio Crossfade (كروس فيد) | Seamless volume fade in/out between tracks (3s-8s) without gaps | M1 | User Request |
| 11| YouTube Audio Search | Search online YouTube tracks matching title, artist, and duration | M2 | R4 |
| 12| Local MP3 Download Engine | Download audio stream as MP3 into `E:\music` and prepend to library index 0 | M2 | R4 |
| 13| Synced / Timed Lyrics Display | Fetch synced LRC lyrics from LRCLIB and smoothly scroll with song playback position | M2 | R4 |
| 14| Chrome Web Verification & Code Quality | Zero errors on `flutter analyze`, 100% passing `flutter test`, smooth operation on Chrome at localhost:8080 | M4 | R5 |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1| Audio Core, Local HDD & Crossfade | `bin/server.dart`, audio streaming (HTTP 206 Range), `E:\music` scanner, newest-first sort, smooth audio crossfade | none | DONE |
| M2| YouTube Search, MP3 Download & Synced Lyrics | YouTube search/download endpoints, LRCLIB client, LRC parser, auto-scroll lyrics widget, and `youtube_search_view.dart` | M1 | DONE |
| M3| Natural AMOLED UI, Blurred Backdrop & Custom SVG Art | Pure `#000000` AMOLED theme, blurred album art background backdrop, natural color extraction tinting buttons/slider/icons, clean Apple/Spotify-style layout, and custom SVG paths | M1 | DONE |
| M4| Verification, Tests, Analyze & Chrome Web Stability | Run full automated tests (`flutter test`), static analysis (`flutter analyze`), Chrome web runner verification (`flutter run -d chrome`), and ensure zero jank/crashes | M1, M2, M3 | DONE |

## Interface Contracts
### Client ↔ Server HTTP REST API (`http://localhost:8081`)
- `GET /api/songs`: Returns JSON list of songs in `E:\music`, sorted by `dateModified` descending:
  `[{"id": "...", "title": "...", "artist": "...", "uri": "http://localhost:8081/api/stream?file=...", "dateAdded": 1710000000, "duration": 210000}]`
- `GET /api/stream?file=<encoded_filename>`: Streams audio with HTTP 206 Partial Content (supports Byte Range requests for seeking).
- `GET /api/youtube/search?q=<query>`: Returns JSON array of YouTube search results:
  `[{"id": "...", "title": "...", "author": "...", "duration": "03:45", "thumbnail": "..."}]`
- `POST /api/youtube/download`: Accepts `{"id": "...", "title": "..."}`, streams audio directly into `E:\music/<title>.mp3`, returns newly created `SongModel` JSON.
- `GET /api/lyrics?title=<title>&artist=<artist>&duration=<sec>`: Returns LRCLIB synced lyrics LRC string or JSON.

### UI ↔ Dynamic Theme Contract
- `AppTheme.amoledDark`: Base background is locked to `Color(0xFF000000)`.
- `DynamicColorNotifier`: Holds current natural `Color dominantAccent`, `Color secondaryAccent` extracted from the active song cover.
- Full screen backdrop renders `ImageFiltered(imageFilter: ImageFilter.blur(sigmaX: 45, sigmaY: 45))` of the active song cover overlaid on pure black with opacity ~0.35.
- `AppSvgIcon`: Renders custom vector SVG paths with `width`, `height`, and `color`. Zero usage of `Icon(Icons.*)`.

## Code Layout
- `bin/server.dart`: Lightweight Dart Companion Server for local HDD `E:\music` & YouTube downloads.
- `lib/core/constants/app_svg_icons.dart`: Complete custom SVG path repository (Play, Pause, Skip, Shuffle, Repeat, Search, Download, Lyrics, Heart, Queue).
- `lib/core/constants/app_colors.dart`: Pure AMOLED black (`#000000`) color tokens and palette helpers.
- `lib/core/theme/app_theme.dart`: AMOLED theme definition.
- `lib/models/lyric_line.dart`: Synced lyrics model and LRC parser.
- `lib/services/lyrics_service.dart`: LRCLIB client and LRC parsing service.
- `lib/services/youtube_service.dart`: Client for companion server YouTube search & download.
- `lib/services/audio_scanner_service.dart`: Enhanced scanner reading from local companion server or desktop.
- `lib/services/player_service.dart`: Audio player engine with smooth crossfade between tracks.
- `lib/providers/music_provider.dart`: State management with newest-first sorting and instant insertion of new downloads.
- `lib/ui/widgets/synced_lyrics_widget.dart`: Animated, auto-scrolling lyrics widget.
- `lib/ui/views/youtube_search_view.dart`: YouTube search and download screen.
- `lib/ui/widgets/app_svg_icon.dart`: Custom SVG icon widget.
