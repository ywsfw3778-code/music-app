# Dispatch for Worker — Milestone 1 (Audio Core & Local HDD Bridge)

You are `worker_m1_audio` (Archetype: teamwork_worker).
Your working directory is: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m1_audio\`
Parent conversation ID: 82c05d99-56aa-4fc3-9440-63d76af0b05f

Read:
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_codebase\handoff.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_features\handoff.md`

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Exclusively Owned Files
- `pubspec.yaml`
- `bin/server.dart`
- `lib/services/audio_scanner_service.dart`
- `lib/services/player_service.dart`
- `lib/providers/music_provider.dart`
- `test/music_logic_test.dart`

## Implementation Tasks
1. Update `pubspec.yaml` with required dependencies:
   - `http: ^1.2.0`
   - `flutter_svg: ^2.0.17`
   - `palette_generator_plus: ^1.2.0`
   - `youtube_explode_dart: ^3.1.0`
   Run `flutter pub get`.
2. Create `bin/server.dart`:
   - Lightweight companion HTTP server running on port `8081` using standard `dart:io`.
   - `GET /api/songs`: Scans `E:\music` (recursively for audio files .mp3, .m4a, .wav, .flac), reads file metadata, sorts by modification time descending (newest-first), returns JSON list with `uri: "http://localhost:8081/api/stream?file=..."`.
   - `GET /api/stream?file=...`: Streams requested audio file with HTTP 206 Partial Content (handling Byte Range requests for smooth web seeking) and CORS header `Access-Control-Allow-Origin: *`.
   - `GET /api/health`: Returns `{"status": "ok"}`.
3. Update `lib/services/audio_scanner_service.dart`:
   - For web (`kIsWeb`), queries `http://localhost:8081/api/songs`.
   - For Windows desktop, reads `E:\music` directly.
   - Preserves newest-first ordering.
   - Graceful fallback if companion server isn't started yet.
4. Update `lib/services/player_service.dart` and `lib/providers/music_provider.dart`:
   - Ensure songs from the companion server stream cleanly via `just_audio`.
   - Ensure queue order places newest songs at the top.
5. STRICT CONSTRAINT: Absolutely NO equalizer feature, sliders, filters, or UI controls.
6. Verification:
   - Run `flutter analyze` (must be 0 errors/warnings).
   - Run `flutter test` (all tests must pass; update/add unit tests for local scanning logic).
7. Write your handoff report to `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m1_audio\handoff.md` and notify parent via `send_message`.


## 2026-11-10T14:31:12Z
Resume work at c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m1_audio\. Read c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md, and DISPATCH.md for instructions. Implement Milestone 1: Audio Core & Local HDD (E:\music) streaming bridge and companion server. Run flutter analyze and flutter test. Write your handoff report to handoff.md in your working directory and notify me via send_message when done.


## 2026-11-10T14:39:13Z
**Context**: New Feature Requirement (Crossfade) & Status Check
**Content**: 
1. The user has added a new requirement: **Crossfade (كروس فيد)**:
   - Smooth audio crossfading between tracks (e.g. 3-8 seconds fade out / fade in) without gaps.
   - Please implement crossfade in `PlayerService` (support smooth volume ramp / fade out before track end, or dual player crossfade, configurable duration e.g. 3-8 seconds).
2. Note: Check `lib/services/player_service.dart` around line 49 for any stray brace.
3. Finish `lib/services/audio_scanner_service.dart` to query `http://localhost:8081/api/songs` on web and scan `E:\music` on desktop.
4. Verify `flutter analyze` has zero issues and `flutter test` passes.
5. Write your complete handoff report to `handoff.md` and report back.
**Action**: Implement crossfade and complete Milestone 1 handoff.
