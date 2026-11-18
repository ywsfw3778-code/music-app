import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:music_player/models/lyric_line.dart';
import 'package:music_player/models/song_model.dart';
import 'package:music_player/services/lyrics_service.dart';
import 'package:music_player/ui/widgets/synced_lyrics_widget.dart';
import 'package:provider/provider.dart';
import 'package:music_player/providers/music_provider.dart';
import 'package:music_player/services/storage_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('LrcParser Tests', () {
    test('parses standard [mm:ss.xx] timestamp', () {
      const lrc = '[01:23.45]Hello world';
      final lines = LrcParser.parse(lrc);

      expect(lines.length, equals(1));
      expect(lines.first.timestamp, equals(const Duration(minutes: 1, seconds: 23, milliseconds: 450)));
      expect(lines.first.text, equals('Hello world'));
    });

    test('parses 3-digit millisecond [mm:ss.xxx] timestamp', () {
      const lrc = '[00:15.345]Precise milliseconds line';
      final lines = LrcParser.parse(lrc);

      expect(lines.length, equals(1));
      expect(lines.first.timestamp, equals(const Duration(seconds: 15, milliseconds: 345)));
      expect(lines.first.text, equals('Precise milliseconds line'));
    });

    test('parses single-digit fraction [mm:ss.x]', () {
      const lrc = '[00:04.8]Eight tenths';
      final lines = LrcParser.parse(lrc);

      expect(lines.length, equals(1));
      expect(lines.first.timestamp, equals(const Duration(seconds: 4, milliseconds: 800)));
      expect(lines.first.text, equals('Eight tenths'));
    });

    test('parses multiple timestamps on a single line', () {
      const lrc = '[00:02.00][00:10.50]Chorus line repeated';
      final lines = LrcParser.parse(lrc);

      expect(lines.length, equals(2));
      expect(lines[0].timestamp, equals(const Duration(seconds: 2)));
      expect(lines[0].text, equals('Chorus line repeated'));
      expect(lines[1].timestamp, equals(const Duration(seconds: 10, milliseconds: 500)));
      expect(lines[1].text, equals('Chorus line repeated'));
    });

    test('sorts lines chronologically even if input is unordered', () {
      const lrc = '''
[01:00.00]Third line
[00:10.00]First line
[00:30.00]Second line
''';
      final lines = LrcParser.parse(lrc);

      expect(lines.length, equals(3));
      expect(lines[0].text, equals('First line'));
      expect(lines[1].text, equals('Second line'));
      expect(lines[2].text, equals('Third line'));
    });

    test('skips metadata header tags like [ti:], [ar:], [al:]', () {
      const lrc = '''
[ti:3ESABAT]
[ar:Abyusif]
[al:Album Name]
[length:03:20]
[00:05.00]Real lyric starts here
''';
      final lines = LrcParser.parse(lrc);

      expect(lines.length, equals(1));
      expect(lines.first.text, equals('Real lyric starts here'));
      expect(lines.first.timestamp, equals(const Duration(seconds: 5)));
    });

    test('returns empty list for empty, null, or whitespace-only content', () {
      expect(LrcParser.parse(null), isEmpty);
      expect(LrcParser.parse(''), isEmpty);
      expect(LrcParser.parse('   \n\t  '), isEmpty);
      expect(LrcParser.parse('[ar:Artist Only]'), isEmpty);
    });
  });

  group('LyricsService Tests', () {
    test('cleanTrackTitle strips extension, track numbers, and parenthesized junk', () {
      expect(
        LyricsService.cleanTrackTitle('01 - Track Title (Official Audio).mp3'),
        equals('Track Title'),
      );
      expect(
        LyricsService.cleanTrackTitle('.2OLELI (Prod. Abyusif) قوليلي.mp3'),
        contains('قوليلي'),
      );
      expect(
        LyricsService.cleanTrackTitle('Abyusif - Fel Dalma Di Etwaladt.flac'),
        equals('Abyusif - Fel Dalma Di Etwaladt'),
      );
    });

    test('fetches synced lyrics via exact match /api/get', () async {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/get') {
          return http.Response(
            jsonEncode({
              'id': 101,
              'trackName': 'Song A',
              'artistName': 'Artist A',
              'syncedLyrics': '[00:03.00]Line 1\n[00:08.50]Line 2',
            }),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response('Not found', 404);
      });

      final service = LyricsService(client: mockClient);
      final song = SongModel(
        id: 1,
        title: 'Song A',
        artist: 'Artist A',
        album: 'Album',
        uri: 'http://test',
        duration: 180000,
      );

      final lyrics = await service.getLyricsForSong(song);
      expect(lyrics.length, equals(2));
      expect(lyrics[0].text, equals('Line 1'));
      expect(lyrics[0].timestamp, equals(const Duration(seconds: 3)));
      expect(lyrics[1].text, equals('Line 2'));
      expect(lyrics[1].timestamp, equals(const Duration(seconds: 8, milliseconds: 500)));
    });

    test('falls back to /api/search when /api/get returns 404', () async {
      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/get') {
          return http.Response('Not found', 404);
        }
        if (request.url.path == '/api/search') {
          return http.Response(
            jsonEncode([
              {
                'id': 202,
                'trackName': 'Song B',
                'artistName': 'Artist B',
                'syncedLyrics': '[00:01.20]Search match lyric line',
              }
            ]),
            200,
            headers: {'content-type': 'application/json'},
          );
        }
        return http.Response('Not found', 404);
      });

      final service = LyricsService(client: mockClient);
      final lyrics = await service.fetchSyncedLyrics(title: 'Song B', artist: 'Artist B');

      expect(lyrics.length, equals(1));
      expect(lyrics.first.text, equals('Search match lyric line'));
      expect(lyrics.first.timestamp, equals(const Duration(seconds: 1, milliseconds: 200)));
    });

    test('returns empty list gracefully on server error or 404', () async {
      final mockClient = MockClient((request) async {
        return http.Response('Internal Error', 500);
      });

      final service = LyricsService(client: mockClient);
      final lyrics = await service.fetchSyncedLyrics(title: 'NonExistent');

      expect(lyrics, isEmpty);
    });

    test('uses memory cache on repeated calls for same song', () async {
      int requestCount = 0;
      final mockClient = MockClient((request) async {
        requestCount++;
        return http.Response(
          jsonEncode({
            'id': 303,
            'syncedLyrics': '[00:05.00]Cached line',
          }),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final service = LyricsService(client: mockClient);
      final song = SongModel(
        id: 77,
        title: 'Cached Song',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://test',
        duration: 120000,
      );

      final first = await service.getLyricsForSong(song);
      final second = await service.getLyricsForSong(song);

      expect(requestCount, equals(1));
      expect(first, equals(second));
    });
  });

  group('SyncedLyricsWidget Widget Tests', () {
    late StorageService storageService;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      storageService = StorageService(prefs);
    });

    testWidgets('renders empty state when no lyrics are available', (tester) async {
      final provider = MusicProvider(storageService);

      await tester.pumpWidget(
        MaterialApp(
          home: ChangeNotifierProvider<MusicProvider>.value(
            value: provider,
            child: const Scaffold(
              body: SyncedLyricsWidget(
                initialLyrics: [],
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();
      expect(find.text('لا توجد كلمات متزامنة متاحة لهذه الأغنية'), findsOneWidget);
    });

    testWidgets('renders lyric lines and supports tap-to-seek', (tester) async {
      final provider = MusicProvider(storageService);
      Duration? seekTarget;

      final testLyrics = [
        const LyricLine(timestamp: Duration(seconds: 0), text: 'Intro verse'),
        const LyricLine(timestamp: Duration(seconds: 5), text: 'Second line here'),
        const LyricLine(timestamp: Duration(seconds: 12), text: 'Third line chorus'),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: ChangeNotifierProvider<MusicProvider>.value(
            value: provider,
            child: Scaffold(
              body: SizedBox(
                height: 400,
                child: SyncedLyricsWidget(
                  initialLyrics: testLyrics,
                  onSeek: (dur) {
                    seekTarget = dur;
                  },
                ),
              ),
            ),
          ),
        ),
      );

      await tester.pumpAndSettle();

      expect(find.text('Intro verse'), findsOneWidget);
      expect(find.text('Second line here'), findsOneWidget);
      expect(find.text('Third line chorus'), findsOneWidget);

      // Tap on second line
      await tester.tap(find.text('Second line here'));
      await tester.pumpAndSettle();

      expect(seekTarget, equals(const Duration(seconds: 5)));
    });
  });
}
