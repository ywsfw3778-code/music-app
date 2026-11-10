# BRIEFING — 2026-11-10T14:28:00Z

## Mission
Investigate technical feasibility and architecture for local HDD audio (E:\music) under Flutter web/Chrome, YouTube search & MP3 download, synced lyrics (LRC), and dynamic color extraction without equalizers.

## 🔒 My Identity
- Archetype: explorer
- Roles: technical feasibility explorer, architecture investigation, problem analysis, synthesis
- Working directory: c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_features\
- Original parent: 82c05d99-56aa-4fc3-9440-63d76af0b05f
- Milestone: technical-feasibility-investigation

## 🔒 Key Constraints
- Read-only investigation — do NOT implement
- Target runtime: Flutter Web / Chrome (e.g. http://localhost:8080)
- Local HDD playback from E:\music under Flutter Web browser sandbox constraints
- Strict constraint: Absolutely NO EQUALIZER feature, sliders, or UI controls (explicitly forbidden by user)
- YouTube search & MP3 download to local library (newest-first library sorting)
- Synced / timed lyrics (LRC format) synchronized with playback position
- Dynamic color extraction from album art for AMOLED pure black theme
- All controls/icons must use custom SVG vector paths (no standard icon fonts)

## Current Parent
- Conversation ID: 82c05d99-56aa-4fc3-9440-63d76af0b05f
- Updated: 2026-11-10T14:28:00Z

## Investigation State
- **Explored paths**:
  - `pubspec.yaml`, `lib/main.dart`, `lib/services/*`, `lib/providers/*`, `lib/ui/*`, `test/*`
  - Local filesystem `E:\music` (verified 98 real MP3 tracks)
  - Browser sandbox & CORS behavior in Chrome for local HDD access and YouTube streaming
  - `youtube_explode_dart` compatibility with Flutter Web vs Dart native server
  - `LRCLIB` API endpoints and live verification with Arabic rap tracks (Abyusif)
  - `palette_generator_plus` vs deprecated `palette_generator` on Flutter Web
  - `flutter_svg` for custom vector path icons replacing standard icon fonts
- **Key findings**:
  1. Browser security forbids direct `file:///` access and silent background writes to `E:\music` from Chrome web.
  2. Client-side `youtube_explode_dart` is blocked on Flutter Web by YouTube's lack of CORS headers.
  3. Solution: Embedded Dart Companion Server (`bin/server.dart` or `server.dart` on port 8081) using `dart:io` provides direct `E:\music` scanning (sorted newest-first), HTTP 206 range audio streaming, and CORS-free YouTube searching/downloading directly into `E:\music`.
  4. LRCLIB is fully operational, free, requires no API key, supports CORS, and returns synced LRC for local tracks.
  5. AMOLED Pure Black (#000000) base with `palette_generator_plus` ambient radial glow satisfies both true OLED black and dynamic album art adaptation.
  6. Strict constraint verified: Zero equalizer sliders/DSP; need to replace `Icons.graphic_eq_rounded` with custom SVG icons.
- **Unexplored areas**: None for technical feasibility; ready for handoff report.

## Key Decisions Made
- Architecture recommendation: Dual-mode with a Dart Companion Server on `http://localhost:8081` as primary backend for Web + direct filesystem for Windows, with client-side fallback.
- Lyrics source: LRCLIB (`https://lrclib.net/api/get` and `/api/search`) with custom LRC timestamp parser and real-time position stream synchronization.
- Vector icons: Custom SVG path strings rendered via `flutter_svg`, completely replacing Material & Cupertino icon fonts.

## Artifact Index
- .agents/teamwork/explorer_features/BRIEFING.md — persistent working memory
- .agents/teamwork/explorer_features/DISPATCH.md — dispatch log
- .agents/teamwork/explorer_features/progress.md — liveness heartbeat
- .agents/teamwork/explorer_features/handoff.md — 5-component technical feasibility report
