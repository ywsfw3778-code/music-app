import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import '../bin/server.dart' as server;

void main() {
  group('Companion Server Empirical Edge Case Tests', () {
    late Directory tempRoot;
    late Directory outsideDir;
    late File arabicSong;
    late File englishSong;
    late File outsideSecretFile;
    late HttpServer testServer;
    late int port;

    setUpAll(() async {
      // Create isolated temporary directories for empirical testing
      tempRoot = await Directory.systemTemp.createTemp('music_server_test_');
      outsideDir = await Directory.systemTemp.createTemp('outside_test_');

      // Create dummy audio files
      englishSong = File('${tempRoot.path}${Platform.pathSeparator}DIVA.mp3');
      await englishSong.writeAsBytes(List.generate(1024, (i) => i % 256));

      // Egyptian Arabic named song with spaces, parentheses and dashes
      arabicSong = File('${tempRoot.path}${Platform.pathSeparator}قوليلي (Abyusif) - تراك.mp3');
      await arabicSong.writeAsBytes(List.generate(2048, (i) => (i * 3) % 256));

      // File outside root to test directory traversal vulnerability
      outsideSecretFile = File('${outsideDir.path}${Platform.pathSeparator}secret.key');
      await outsideSecretFile.writeAsString('SUPER_SECRET_TOKEN');

      // Start test server on ephemeral port (port 0)
      testServer = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      port = testServer.port;

      testServer.listen((request) async {
        try {
          server.applyCorsHeaders(request.response);
          if (request.uri.path == '/api/stream') {
            await server.handleStreamRequest(request, tempRoot);
          } else {
            request.response.statusCode = HttpStatus.notFound;
            await request.response.close();
          }
        } catch (e) {
          try {
            request.response.statusCode = HttpStatus.internalServerError;
            request.response.write(e.toString());
            await request.response.close();
          } catch (_) {}
        }
      });
    });

    tearDownAll(() async {
      await testServer.close(force: true);
      if (await tempRoot.exists()) {
        await tempRoot.delete(recursive: true);
      }
      if (await outsideDir.exists()) {
        await outsideDir.delete(recursive: true);
      }
    });

    test('Missing file parameter returns 400 Bad Request', () async {
      final res = await http.get(Uri.parse('http://localhost:$port/api/stream'));
      expect(res.statusCode, HttpStatus.badRequest);
      expect(res.body, contains('Missing'));
    });

    test('Non-existent file returns 404 Not Found', () async {
      final res = await http.get(Uri.parse('http://localhost:$port/api/stream?file=does_not_exist.mp3'));
      expect(res.statusCode, HttpStatus.notFound);
    });

    test('Valid English track streaming with Range header returns 206 Partial Content', () async {
      final uri = Uri.parse('http://localhost:$port/api/stream?file=DIVA.mp3');
      final res = await http.get(uri, headers: {'Range': 'bytes=0-99'});
      expect(res.statusCode, HttpStatus.partialContent);
      expect(res.headers['content-range'], 'bytes 0-99/1024');
      expect(res.bodyBytes.length, 100);
      expect(res.headers['access-control-allow-origin'], '*');
    });

    test('Out of bounds Range header (start >= fileLength) returns 416 Range Not Satisfiable', () async {
      final uri = Uri.parse('http://localhost:$port/api/stream?file=DIVA.mp3');
      final res = await http.get(uri, headers: {'Range': 'bytes=9999999-9999999'});
      expect(res.statusCode, HttpStatus.requestedRangeNotSatisfiable);
      expect(res.headers['content-range'], 'bytes */1024');
    });

    test('Egyptian Arabic filename streams without 500 double-decoding error', () async {
      // In companion server, the URI is built with Uri.encodeComponent:
      final encodedName = Uri.encodeComponent('قوليلي (Abyusif) - تراك.mp3');
      final url = 'http://localhost:$port/api/stream?file=$encodedName';
      final res = await http.get(Uri.parse(url), headers: {'Range': 'bytes=0-99'});

      // BUG CHECK: In buggy server.dart, this crashes with 500 due to Uri.decodeComponent on already decoded string
      expect(res.statusCode, HttpStatus.partialContent,
          reason: 'Server must not return 500 on Egyptian Arabic song titles. Returned ${res.statusCode}: ${res.body}');
      expect(res.bodyBytes.length, 100);
    });

    test('Inverted Range header (bytes=500-100 where start > end) returns 416 instead of 500 crash', () async {
      final uri = Uri.parse('http://localhost:$port/api/stream?file=DIVA.mp3');
      final res = await http.get(uri, headers: {'Range': 'bytes=500-100'});

      // BUG CHECK: In buggy server.dart, start > end calculates negative Content-Length (-399) and crashes with 500
      expect(res.statusCode, HttpStatus.requestedRangeNotSatisfiable,
          reason: 'Inverted range (start > end) must return 416, not 500 crash. Returned ${res.statusCode}');
    });

    test('Directory traversal attempt does not leak files outside music directory', () async {
      // Place a sensitive file in parent directory of tempRoot
      final parentFile = File('${tempRoot.parent.path}${Platform.pathSeparator}traversal_secret.txt');
      await parentFile.writeAsString('TOP_SECRET_CREDENTIALS');
      try {
        final uri = Uri.parse('http://localhost:$port/api/stream?file=..%2Ftraversal_secret.txt');
        final res = await http.get(uri);

        // BUG CHECK: In buggy server.dart line 173:
        // (!canonicalTarget.startsWith(canonicalRoot) && !targetFile.existsSync())
        // If file exists outside root, !targetFile.existsSync() is false, so it does not block the request!
        expect(res.statusCode, isNot(HttpStatus.ok),
            reason: 'Directory traversal must be blocked (403 or 404). Current server allowed streaming with 200 OK!');
        expect(res.body, isNot(contains('TOP_SECRET_CREDENTIALS')));
      } finally {
        if (await parentFile.exists()) {
          await parentFile.delete();
        }
      }
    });
  });

  group('Audio Core Strict Negative Constraint Audit', () {
    test('Zero equalizer code or UI controls in lib/ and bin/', () {
      final projectDir = Directory.current;
      final dirsToScan = [
        Directory('${projectDir.path}${Platform.pathSeparator}lib'),
        Directory('${projectDir.path}${Platform.pathSeparator}bin'),
      ];

      final forbiddenKeywords = [
        'equalizer',
        'equalise',
        'equalize',
        'audiofilter',
        'soundpreset',
      ];

      final violations = <String>[];

      for (final dir in dirsToScan) {
        if (!dir.existsSync()) continue;
        final files = dir.listSync(recursive: true).whereType<File>();
        for (final file in files) {
          if (!file.path.endsWith('.dart')) continue;
          final content = file.readAsStringSync().toLowerCase();
          for (final kw in forbiddenKeywords) {
            if (content.contains(kw)) {
              violations.add('${file.path} contains forbidden keyword "$kw"');
            }
          }
        }
      }

      expect(violations, isEmpty, reason: 'Strict constraint: Absolutely zero equalizer code permitted');
    });
  });
}
