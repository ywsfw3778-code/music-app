# Handoff Report — Milestone 1: Audio Core & Streaming Bridge Empirical Challenge

- **Agent**: `challenger_m1_1` (Archetype: teamwork_challenger)
- **Role**: critic, specialist
- **Timestamp**: 2026-11-10T15:00:00Z
- **Working Directory**: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\challenger_m1_1\`
- **Milestone**: Milestone 1 (Audio Core & Local HDD Bridge)
- **Verdict**: **REQUEST_CHANGES**

---

## 1. Observation

Direct empirical observations, commands, exact file paths, line numbers, and tool outputs:

### 1.1 Blocker Bug: Egyptian Arabic Song Titles Return HTTP 500
- **Location**: `bin/server.dart` (Line 157)
- **Code**:
  ```dart
  final rawFile = request.uri.queryParameters['file'];
  ...
  var decodedPath = Uri.decodeComponent(rawFile).replaceAll('/', Platform.pathSeparator);
  ```
- **Observed Execution**:
  Ran:
  ```powershell
  curl.exe -i "http://localhost:8081/api/stream?file=.2OLELI%20(Prod.%20Abyusif)%20%D9%82%D9%88%D9%84%D9%8A%D9%84%D9%8A.mp3" -H "Range: bytes=0-100"
  ```
  Output:
  ```
  HTTP/1.1 500 Internal Server Error
  ```
  Server Log:
  ```
  Unhandled request error: Invalid argument(s): Illegal percent encoding in URI
  #0      _Uri._uriDecode (dart:core/uri.dart:3662:11)
  #1      Uri.decodeComponent (dart:core/uri.dart:1299:17)
  #2      handleStreamRequest (bin/server.dart:157:25)
  ```
- **Same 500 error reproduced across Egyptian Arabic titles in `E:\music`**:
  - `Marwan Moussa - البوصلة ضاعت.mp3` -> HTTP 500
  - `Telegram/Wegz - تايه.mp3` -> HTTP 500
  - `Wegz - ساليني.mp3` -> HTTP 500
  - `AFROTO - FAR2 KHEBRA _ (عفروتو - فرق خبرة...` -> HTTP 500

### 1.2 Critical Vulnerability: Directory Traversal Arbitrary File Read (CWE-22)
- **Location**: `bin/server.dart` (Line 173)
- **Code**:
  ```dart
  if (!canonicalTarget.startsWith(canonicalRoot) && !targetFile.existsSync()) {
    response.statusCode = HttpStatus.notFound;
    response.write(jsonEncode({'error': 'File not found or outside root'}));
    await response.close();
    return;
  }
  ```
- **Observed Execution**:
  Ran:
  ```powershell
  curl.exe -I "http://localhost:8081/api/stream?file=../PdaNetA5232b.exe"
  ```
  Output:
  ```
  HTTP/1.1 200 OK
  content-length: 4038192
  content-type: application/octet-stream
  ```
  Because `targetFile.existsSync()` is `true` for existing files outside `rootDir`, `!targetFile.existsSync()` is `false`. The `&&` condition evaluates to `false`, completely bypassing the root containment check and streaming arbitrary files outside `E:\music` to any client.

### 1.3 Bug: Inverted Byte Ranges Crash Server with HTTP 500
- **Location**: `bin/server.dart` (Lines 198-203, 214, 217)
- **Code**:
  ```dart
  final contentLength = end - start + 1;
  response.statusCode = HttpStatus.partialContent;
  response.headers.set(HttpHeaders.contentLengthHeader, contentLength.toString());
  ```
- **Observed Execution**:
  Ran:
  ```powershell
  curl.exe -i -H "Range: bytes=500-100" "http://localhost:8081/api/stream?file=ABYUSIF%20x%20MARWAN%20PABLO%20-%20DIVA%20(Audio)(MP3_160K).mp3"
  ```
  Output:
  ```
  HTTP/1.1 500 Internal Server Error
  ```
  Server Log:
  ```
  Unhandled request error: HttpException: Content-Length must contain only digits
  #0      _HttpHeaders._addContentLength (dart:_http/http_headers.dart:391:9)
  #4      handleStreamRequest (bin/server.dart:217:22)
  ```
  `contentLength` evaluates to `-399`. RFC 7233 section 2.1 requires returning `416 Requested Range Not Satisfiable` when `start > end`. Passing `"-399"` into `Content-Length` crashes `dart:_http`.

### 1.4 Non-Compliance: Suffix Byte Ranges (`bytes=-50`) Return Prefix
- **Location**: `bin/server.dart` (Lines 198-201)
- **Observed Execution**:
  Ran:
  ```powershell
  curl.exe -i -H "Range: bytes=-50" "http://localhost:8081/api/stream?file=ABYUSIF%20x%20MARWAN%20PABLO%20-%20DIVA%20(Audio)(MP3_160K).mp3"
  ```
  Output:
  ```
  content-range: bytes 0-50/3790443
  ```
  `parts[0]` is `""`, parsing as `0`. RFC 7233 section 2.1 specifies `bytes=-50` means the final 50 bytes of representation data (`fileLength - 50` to `fileLength - 1`), not bytes `0-50`.

### 1.5 Positive Verifications
- **Missing `file` parameter**: Returns `400 Bad Request` with `{"error":"Missing \"file\" query parameter"}`. (PASS)
- **Non-existent file**: Returns `404 Not Found` with `{"error":"File not found"}`. (PASS)
- **Range out of bounds (`bytes=99999999-99999999`)**: Returns `416 Requested range not satisfiable` with `content-range: bytes */3790443`. (PASS)
- **Strict Negative Constraint (Zero Equalizer)**: Exhaustive regex/keyword scan across all files in `lib/`, `bin/`, and `test/` for `equalizer`, `equalize`, `equalise`, `audiofilter`, and `soundpreset` confirmed 0 instances. Constraint is strictly satisfied. (PASS)
- **Existing Tests**: `flutter test test/music_logic_test.dart test/widget_test.dart` passed 11/11 tests. (PASS)
- **Automated Regression Suite**: Created `test/companion_server_test.dart` codifying all edge cases.

---

## 2. Logic Chain

1. **Arabic Filename Crash**: Dart's standard `HttpRequest.uri.queryParameters` automatically parses and decodes query parameter values. When a request for `.../api/stream?file=Abyusif%20-%20%D9%82%D9%88%D9%84%D9%8A%D9%84%D9%8A.mp3` arrives, `request.uri.queryParameters['file']` produces the already-decoded string `Abyusif - قوليلي.mp3`. At line 157, `bin/server.dart` calls `Uri.decodeComponent` a second time. Dart's `Uri.decodeComponent` requires percent-encoded ASCII bytes and throws `Invalid argument(s): Illegal percent encoding in URI` when fed raw UTF-8 characters. This unhandled exception triggers HTTP 500, making every Egyptian Arabic track unplayable on Chrome Web.
2. **Directory Traversal**: In `bin/server.dart:173`, the guard condition is `if (!canonicalTarget.startsWith(canonicalRoot) && !targetFile.existsSync())`. In boolean logic, `(!A && !B)` is true only if both `!A` and `!B` are true. If an attacker requests a file that *does* exist outside the root directory, `targetFile.existsSync()` is true, so `!targetFile.existsSync()` is false, causing the entire block to evaluate to false. The server then executes line 224/234 and streams the sensitive file outside `E:\music` with `HTTP 200 OK`.
3. **Inverted Range Crash**: When a client requests `Range: bytes=500-100`, `start` (500) is greater than `end` (100). The current server only checks `if (start >= fileLength)`. Because `start < fileLength`, it proceeds to compute `contentLength = end - start + 1 = -399`. Attempting to set `Content-Length: -399` triggers `HttpException: Content-Length must contain only digits` in Dart's HTTP header parser, crashing with HTTP 500 instead of returning HTTP 416.

---

## 3. Caveats

- We observed `challenger_m1_2` working concurrently on crossfade tests (`test/empirical_m1_crossfade_test.dart` and `test/helpers/mock_just_audio_platform.dart`). Those helper files introduce temporary analyzer notices about missing pubspec dependencies (`plugin_platform_interface`, `just_audio_platform_interface`), which are owned by `challenger_m1_2`.
- No other caveats.

---

## 4. Conclusion

**Verdict: REQUEST_CHANGES**

Milestone 1 CANNOT be approved in its current state because:
1. **Egyptian Arabic songs (critical requirement for user's library) fail with HTTP 500 on Chrome Web**.
2. **Directory traversal allows arbitrary file disclosure outside `E:\music`**.
3. **Inverted HTTP Range headers crash the server with HTTP 500 instead of returning 416**.

### Actionable Required Fixes for `worker_m1_audio` in `bin/server.dart`:

1. **Fix Egyptian Arabic Decoding (Line 157)**:
   Do not call `Uri.decodeComponent` again on `rawFile`:
   ```dart
   var decodedPath = rawFile.replaceAll('/', Platform.pathSeparator);
   ```

2. **Fix Directory Traversal Vulnerability (Lines 173-178)**:
   Change condition to block any target that does not start with `canonicalRoot`:
   ```dart
   if (!canonicalTarget.startsWith(canonicalRoot)) {
     response.statusCode = HttpStatus.forbidden;
     response.write(jsonEncode({'error': 'Access denied: outside root directory'}));
     await response.close();
     return;
   }
   ```

3. **Fix Range Validation & Suffix Ranges (Lines 195-212)**:
   ```dart
   if (rangeHeader != null && rangeHeader.startsWith('bytes=')) {
     final rangeValue = rangeHeader.substring(6).trim();
     final parts = rangeValue.split('-');
     int start;
     int end;

     if (parts[0].isEmpty) {
       // Suffix byte range: bytes=-50 (last 50 bytes)
       final suffixLength = int.tryParse(parts.length > 1 ? parts[1] : '') ?? 0;
       if (suffixLength <= 0) {
         response.statusCode = HttpStatus.requestedRangeNotSatisfiable;
         response.headers.set(HttpHeaders.contentRangeHeader, 'bytes */$fileLength');
         await response.close();
         return;
       }
       start = (fileLength - suffixLength).clamp(0, fileLength - 1);
       end = fileLength - 1;
     } else {
       start = int.tryParse(parts[0]) ?? 0;
       end = (parts.length > 1 && parts[1].isNotEmpty)
           ? (int.tryParse(parts[1]) ?? fileLength - 1)
           : fileLength - 1;
     }

     if (start < 0 || start >= fileLength || start > end) {
       response.statusCode = HttpStatus.requestedRangeNotSatisfiable;
       response.headers.set(HttpHeaders.contentRangeHeader, 'bytes */$fileLength');
       await response.close();
       return;
     }

     if (end >= fileLength) {
       end = fileLength - 1;
     }
   ```

---

## 5. Verification Method

To independently verify the bugs and validate fixes:

1. **Run Companion Server Test Suite**:
   ```powershell
   flutter test test/companion_server_test.dart
   ```
   - *Current Buggy State*: Fails on `Egyptian Arabic filename streams without 500`, `Inverted Range header returns 416`, and `Directory traversal attempt does not leak files`.
   - *Fixed State*: All tests in `test/companion_server_test.dart` will pass cleanly.

2. **Run Manual HTTP Reproduction**:
   ```powershell
   # Start server:
   dart run bin/server.dart

   # Test Arabic streaming:
   curl.exe -i "http://localhost:8081/api/stream?file=.2OLELI%20(Prod.%20Abyusif)%20%D9%82%D9%88%D9%84%D9%8A%D9%84%D9%8A.mp3" -H "Range: bytes=0-100"

   # Test Directory Traversal:
   curl.exe -I "http://localhost:8081/api/stream?file=../PdaNetA5232b.exe"

   # Test Inverted Range:
   curl.exe -i -H "Range: bytes=500-100" "http://localhost:8081/api/stream?file=ABYUSIF%20x%20MARWAN%20PABLO%20-%20DIVA%20(Audio)(MP3_160K).mp3"
   ```
