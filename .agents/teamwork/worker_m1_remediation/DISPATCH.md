# Dispatch for Remediation Worker — Milestone 1 (Iteration 2)

You are `worker_m1_remediation` (Archetype: teamwork_worker).
Your working directory is: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m1_remediation\`
Parent conversation ID: 82c05d99-56aa-4fc3-9440-63d76af0b05f

Read:
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md`
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_m1_r2\handoff.md` (CONTAINS EXACT DIFFS)

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Exclusively Owned Files
- `bin/server.dart`
- `lib/services/player_service.dart`
- `lib/providers/music_provider.dart`
- `test/music_logic_test.dart`

## Instructions
Apply the exact patches detailed in `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_m1_r2\handoff.md`:
1. `bin/server.dart`:
   - Line 157: Remove redundant `Uri.decodeComponent`. Change to `var decodedPath = rawFile.replaceAll('/', Platform.pathSeparator);`
   - Lines 164-185: Fix path traversal guard. Verify `!targetFile.existsSync()` returns 404, and `canonicalTarget.startsWith(rootPrefix) || canonicalTarget == canonicalRoot`. If not inside root, return 404.
   - Lines 195-218: Handle suffix ranges `bytes=-50` (last 50 bytes) and inverted ranges (`start < 0 || start >= fileLength || start > end` returns 416).
2. `lib/services/player_service.dart`:
   - Add `bool _isTransitioning = false;` and `double _userVolume = 1.0;`. Expose getters.
   - In `playSong`: set `_isTransitioning = true` during crossfades. Before deck swap, copy `_activePlayer.shuffleModeEnabled` and `_activePlayer.loopMode` to `incoming`.
   - In `_runCrossfadeRamp`: scale volume by `_userVolume`, and in `finally` reset `_isTransitioning = false`.
   - In `toggleShuffle`, `cycleRepeatMode`, and `setVolume`: update BOTH `_activePlayer` and `_standbyPlayer`.
3. `lib/providers/music_provider.dart`:
   - In `_onSongCompleted`: guard with `if (_playerService.isTransitioning) return;`.
4. `test/music_logic_test.dart`:
   - Replace tautological test with authentic negative constraint verification (scanning `lib/` and `bin/` for forbidden keywords).

## Verification
1. Run `flutter analyze` — must be 0 errors, 0 warnings.
2. Run `flutter test` — must pass 36/36 tests with 0 failures!
3. Write your handoff report to `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m1_remediation\handoff.md` and notify parent via `send_message`.


## 2026-11-10T15:15:35Z
Resume work at c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m1_remediation\. Read c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\explorer_m1_r2\handoff.md, and DISPATCH.md. Apply all specified remediation patches to bin/server.dart, player_service.dart, music_provider.dart, and music_logic_test.dart. Run flutter test and flutter analyze. Ensure all 36 tests pass. Write handoff.md and notify me via send_message when done.
