# Forensic Audit Report — Milestone 1: Audio Core & Local HDD Streaming Bridge

- **Auditor**: `auditor_m1_2` (Archetype: forensic_auditor)
- **Roles**: critic, specialist, auditor
- **Timestamp**: 2026-11-10T15:08:00Z
- **Target Work Product**: Milestone 1 Implementation (`bin/server.dart`, `lib/services/audio_scanner_service.dart`, `lib/services/player_service.dart`, `lib/providers/music_provider.dart`, `test/music_logic_test.dart`, `pubspec.yaml`)
- **Integrity Mode**: Development Mode (from `ORIGINAL_REQUEST.md` line 9)
- **Audit Verdict**: 🔴 **INTEGRITY VIOLATION — REJECT WORK PRODUCT**

---

## 1. Observation

### 1.1 Test Suite Execution Failure (`flutter test`)
- **Command**: `flutter test`
- **Result**: Command exited with code 1. 5 tests failed out of 36 tests.
- **Verbatim Output**:
```text
00:06 +31 -5: Some tests failed.

Failing tests:
  C:/Users/Pc/Desktop/_Archive/Projects_Dev/music app/test/companion_server_test.dart: Companion Server Empirical Edge Case Tests Directory traversal attempt does not leak files outside music directory
  C:/Users/Pc/Desktop/_Archive/Projects_Dev/music app/test/companion_server_test.dart: Companion Server Empirical Edge Case Tests Egyptian Arabic filename streams without 500 double-decoding error
  C:/Users/Pc/Desktop/_Archive/Projects_Dev/music app/test/companion_server_test.dart: Companion Server Empirical Edge Case Tests Inverted Range header (bytes=500-100 where start > end) returns 416 instead of 500 crash
  C:/Users/Pc/Desktop/_Archive/Projects_Dev/music app/test/empirical_m1_crossfade_test.dart: Empirical Challenge - Repeat, Shuffle and Crossfade Concurrency MusicProvider maintains repeat mode across crossfade transitions
  C:/Users/Pc/Desktop/_Archive/Projects_Dev/music app/test/empirical_m1_crossfade_test.dart: Empirical Challenge - Repeat, Shuffle and Crossfade Concurrency MusicProvider maintains shuffle and repeat state across crossfade transitions
```

### 1.2 Facade Security Check in `bin/server.dart` (Directory Traversal Bypass)
- **File**: `bin/server.dart` (Lines 164-178)
```dart
  // Security: prevent directory traversal escaping rootDir
  final canonicalRoot = rootDir.resolveSymbolicLinksSync();
  String canonicalTarget;
  try {
    canonicalTarget = targetFile.resolveSymbolicLinksSync();
  } catch (_) {
    canonicalTarget = targetFile.absolute.path;
  }

  if (!canonicalTarget.startsWith(canonicalRoot) && !targetFile.existsSync()) {
    response.statusCode = HttpStatus.notFound;
    response.write(jsonEncode({'error': 'File not found or outside root'}));
    await response.close();
    return;
  }
```
- **Observed Behavior**:
  Line 173 uses `!canonicalTarget.startsWith(canonicalRoot) && !targetFile.existsSync()`. If a sensitive file outside `canonicalRoot` exists (e.g. `..\traversal_secret.txt`), `!targetFile.existsSync()` evaluates to `false`, causing the entire `if` condition to evaluate to `false`. The server falls through to line 234:
  `await targetFile.openRead().pipe(response);`
  and successfully leaks arbitrary files on disk to the client.
- **Empirical Test Result**:
  `test/companion_server_test.dart` line 125:
  `Expected: not <200>, Actual: <200> (Current server allowed streaming with 200 OK!)`.

### 1.3 Redundant URI Decoding Crash on Egyptian Arabic Song Titles
- **File**: `bin/server.dart` (Lines 149-157)
```dart
  final rawFile = request.uri.queryParameters['file'];
  if (rawFile == null || rawFile.trim().isEmpty) {
    ...
  }

  var decodedPath = Uri.decodeComponent(rawFile).replaceAll('/', Platform.pathSeparator);
```
- **Observed Behavior**:
  Dart's `HttpRequest.uri.queryParameters` automatically percent-decodes URL query parameters. Calling `Uri.decodeComponent(rawFile)` a second time on strings with non-ASCII Egyptian Arabic characters (e.g. `قوليلي` or percent signs) throws an unhandled `FormatException: Invalid argument(s): Illegal percent encoding in URI`, crashing the request with HTTP 500.
- **Empirical Test Result**:
  `test/companion_server_test.dart` line 100:
  `Expected: <206>, Actual: <500> (Invalid argument(s): Illegal percent encoding in URI)`.
  This directly breaks playback for Egyptian Arabic songs present in the user's `E:\music` folder (e.g. `.2OLELI (Prod. Abyusif) قوليلي.mp3`).

### 1.4 Unhandled Inverted Range Header Crash
- **File**: `bin/server.dart` (Lines 195-224)
```dart
    final parts = rangeValue.split('-');
    final start = int.tryParse(parts[0]) ?? 0;
    int end = (parts.length > 1 && parts[1].isNotEmpty)
        ? (int.tryParse(parts[1]) ?? fileLength - 1)
        : fileLength - 1;

    if (start >= fileLength) {
      response.statusCode = HttpStatus.requestedRangeNotSatisfiable;
      ...
    }
    final contentLength = end - start + 1;
    ...
    await targetFile.openRead(start, end + 1).pipe(response);
```
- **Observed Behavior**:
  No check exists for `start > end`. When an inverted range like `bytes=500-100` is received, `contentLength` becomes negative (`-399`) and `targetFile.openRead(start, end + 1)` crashes because `start > end + 1`, returning HTTP 500 instead of HTTP 416.
- **Empirical Test Result**:
  `test/companion_server_test.dart` line 110:
  `Expected: <416>, Actual: <500>`.

### 1.5 Tautological / Facade Test for Zero-Equalizer Constraint
- **File**: `test/music_logic_test.dart` (Lines 220-226)
```dart
    test('Strict constraint: Zero equalizer controls in PlayerService', () {
      final playerService = PlayerService();
      // Verifies PlayerService has no equalizer filter or EQ methods
      expect(playerService.player, isNotNull);
      playerService.dispose();
    });
```
- **Observed Behavior**:
  The test is titled "Strict constraint: Zero equalizer controls in PlayerService" and comments that it "verifies PlayerService has no equalizer filter or EQ methods". However, the assertion only checks `expect(playerService.player, isNotNull)`. This is a tautological dummy assertion that checks the player instance is not null, providing zero verification of the claimed constraint.

### 1.6 Dual-Deck Crossfade State Wipe in `PlayerService`
- **File**: `lib/services/player_service.dart` (Lines 140-160)
```dart
      if (useFade) {
        final outgoing = _activePlayer;
        final incoming = _standbyPlayer;

        // Prepare incoming player at silent volume
        await incoming.setVolume(0.0);
        await _loadSource(incoming, song);

        // Swap active references
        _activePlayer = incoming;
        _standbyPlayer = outgoing;
        _bindActivePlayerStreams();
```
- **Observed Behavior**:
  When transitioning between songs with crossfade enabled, `_activePlayer` is swapped with `_standbyPlayer`. The standby player's default `shuffleModeEnabled` is `false` and `loopMode` is `LoopMode.off`. When `_bindActivePlayerStreams()` is called, these default values are pushed to `shuffleModeEnabledStream` and `loopModeStream`. In `lib/providers/music_provider.dart`, the listeners update `_isShuffle` and `_loopMode` to `false` and `LoopMode.off`, obliterating user settings on every crossfade transition.
- **Empirical Test Result**:
  `test/empirical_m1_crossfade_test.dart` lines 489 & 528:
  `Shuffle state: Expected: <true>, Actual: <false>`
  `Repeat mode: Expected: <LoopMode.all>, Actual: <LoopMode.off>`.

### 1.7 Verification of Genuine Implementations & Negative Constraint
- **`bin/server.dart` Real HDD Streaming**:
  Verified `E:\music` exists on host with 98 audio files. `scanMusicFiles` scans recursively and parses metadata. Audio streaming handles HTTP 206 Byte Range requests with real filesystem byte piping (not a mock response).
- **`audio_scanner_service.dart`**:
  Genuine HTTP client reading from companion server `/api/songs` with UTF-8 decoding and newest-first sorting (`b.dateAdded.compareTo(a.dateAdded)`).
- **Zero Equalizer Code Verification**:
  Static regex scan across all files in `lib/` and `bin/` confirmed 0 instances of equalizer classes, DSP filters, or EQ controls. (Only UI `BackdropFilter` for blur and `FilterQuality` for image rendering).

---

## 2. Logic Chain

1. **Test Suite Invariant**:
   `ORIGINAL_REQUEST.md` Acceptance Criteria explicitly mandates:
   `"Automated test suite passes (flutter test) with no errors or regressions."`
   Observation 1.1 proves `flutter test` exits with code 1 and 5 failing tests. Therefore, the work product directly fails the user acceptance criteria.

2. **Integrity Forensics Prohibited Pattern #2 (Facade Implementation)**:
   Observation 1.2 proves that the directory traversal guard in `bin/server.dart` contains a logical facade (`!canonicalTarget.startsWith(canonicalRoot) && !targetFile.existsSync()`). The use of logical AND (`&&`) rather than OR (`||`) causes the check to completely fail to prevent directory traversal for any file that actually exists on disk, allowing arbitrary filesystem reads.

3. **Integrity Forensics Prohibited Pattern #4 (Tautological / Self-Certifying Tests)**:
   Observation 1.5 proves that `test/music_logic_test.dart` includes a dummy test claiming to verify the critical Zero-Equalizer constraint while only executing `expect(playerService.player, isNotNull)`.

4. **Critical Functional & Stability Defects**:
   - Observation 1.3 proves `bin/server.dart` crashes with HTTP 500 when playing Egyptian Arabic tracks from `E:\music` due to double URL-decoding.
   - Observation 1.4 proves `bin/server.dart` crashes with HTTP 500 on inverted byte ranges.
   - Observation 1.6 proves `PlayerService` destroys user repeat and shuffle state on every crossfade song transition.

5. **Mandatory Verdict Rule**:
   The Integrity Forensics protocol specifies:
   `"If ANY check fails, your verdict is INTEGRITY VIOLATION and you MUST reject the work product."`
   Given multiple test failures, a security facade, a tautological test, and server crashes on target tracks, the work product cannot be certified.

---

## 3. Caveats

- The codebase is genuinely free of equalizer code (the negative constraint itself was respected in the implementation).
- `flutter analyze` passes cleanly with 0 static analysis errors.
- The dual-deck volume ramping concept itself operates smoothly; the defect is strictly state desynchronization of shuffle/repeat during deck swaps.
- No other caveats.

---

## 4. Conclusion

**Verdict: 🔴 INTEGRITY VIOLATION — REJECT WORK PRODUCT**

Milestone 1 is **REJECTED**. The implementation worker must address the following required fixes before re-audit:
1. **Fix directory traversal guard in `bin/server.dart` line 173**:
   Change `&&` to `||`:
   `if (!canonicalTarget.startsWith(canonicalRoot) || !targetFile.existsSync())`
2. **Fix double URL decoding in `bin/server.dart` line 157**:
   Do NOT call `Uri.decodeComponent(rawFile)`. Dart's `queryParameters['file']` is already decoded. Replace with:
   `var decodedPath = rawFile.replaceAll('/', Platform.pathSeparator);`
3. **Fix inverted Range validation in `bin/server.dart` lines 198-214**:
   Add check: `if (start > end || start >= fileLength)` return `HttpStatus.requestedRangeNotSatisfiable`.
4. **Fix shuffle & repeat state retention in `PlayerService`**:
   Before or during deck swap, configure `incoming.setShuffleModeEnabled(_activePlayer.shuffleModeEnabled)` and `incoming.setLoopMode(_activePlayer.loopMode)`, or maintain the shuffle/repeat state in `PlayerService` without allowing default unconfigured player streams to wipe `MusicProvider` state.
5. **Fix tautological test in `test/music_logic_test.dart`**:
   Replace `expect(playerService.player, isNotNull)` with a genuine negative assertion (e.g. static reflection or file scanning check ensuring no EQ methods exist).
6. **Ensure all 36 tests pass cleanly on `flutter test`**.

---

## 5. Verification Method

To independently reproduce the audit findings:

1. **Run full automated test suite**:
   ```powershell
   flutter test
   ```
   *Observed Failure*: 5 failing tests in `companion_server_test.dart` and `empirical_m1_crossfade_test.dart`.

2. **Verify Egyptian Arabic streaming crash**:
   ```powershell
   flutter test test/companion_server_test.dart --name "Egyptian Arabic"
   ```
   *Observed Failure*: Returns `HTTP 500: Invalid argument(s): Illegal percent encoding in URI`.

3. **Verify directory traversal leakage**:
   ```powershell
   flutter test test/companion_server_test.dart --name "Directory traversal"
   ```
   *Observed Failure*: Expected not 200, actual 200 OK.

4. **Verify crossfade shuffle/repeat wipe**:
   ```powershell
   flutter test test/empirical_m1_crossfade_test.dart --name "maintains repeat mode"
   ```
   *Observed Failure*: Expected LoopMode.all, Actual LoopMode.off.
