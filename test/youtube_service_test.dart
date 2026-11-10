import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:music_player/providers/music_provider.dart';
import 'package:music_player/services/storage_service.dart';
import 'package:music_player/services/youtube_service.dart';
import 'package:music_player/ui/views/youtube_search_view.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  group('YouTubeSearchResult Model Tests', () {
    test('serializes and deserializes correctly', () {
      final json = {
        'id': 'dQw4w9WgXcQ',
        'title': 'Never Gonna Give You Up',
        'author': 'Rick Astley',
        'duration': '03:33',
        'thumbnail': 'https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg',
      };

      final result = YouTubeSearchResult.fromJson(json);
      expect(result.id, equals('dQw4w9WgXcQ'));
      expect(result.title, equals('Never Gonna Give You Up'));
      expect(result.author, equals('Rick Astley'));
      expect(result.duration, equals('03:33'));
      expect(result.thumbnail, equals('https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg'));

      expect(result.toJson(), equals(json));
    });
  });

  group('YouTubeService Unit Tests', () {
    test('search returns empty list on empty query without network call', () async {
      int calls = 0;
      final mockClient = MockClient((request) async {
        calls++;
        return http.Response('[]', 200);
      });

      final service = YouTubeService(client: mockClient);
      final results = await service.search('   ');

      expect(results, isEmpty);
      expect(calls, equals(0));
    });

    test('search calls companion server and parses results', () async {
      final mockClient = MockClient((request) async {
        expect(request.url.path, equals('/api/youtube/search'));
        expect(request.url.queryParameters['q'], equals('Abyusif'));

        final sampleResults = [
          {
            'id': 'v123',
            'title': 'Abyusif - 3ESABAT',
            'author': 'Abyusif',
            'duration': '03:15',
            'thumbnail': 'https://img/v123.jpg',
          },
          {
            'id': 'v456',
            'title': 'Abyusif - Basha E3temed',
            'author': 'Abyusif',
            'duration': '04:02',
            'thumbnail': 'https://img/v456.jpg',
          },
        ];

        return http.Response(
          jsonEncode(sampleResults),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final service = YouTubeService(client: mockClient);
      final results = await service.search('Abyusif');

      expect(results.length, equals(2));
      expect(results[0].id, equals('v123'));
      expect(results[0].title, equals('Abyusif - 3ESABAT'));
      expect(results[1].id, equals('v456'));
    });

    test('search throws YouTubeServiceException on HTTP error', () async {
      final mockClient = MockClient((request) async {
        return http.Response('Internal server error', 500);
      });

      final service = YouTubeService(client: mockClient);
      expect(
        () => service.search('error query'),
        throwsA(isA<YouTubeServiceException>()),
      );
    });

    test('downloadSong calls POST /api/youtube/download and prepends to MusicProvider index 0', () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final storageService = StorageService(prefs);
      final musicProvider = MusicProvider(storageService);

      final mockClient = MockClient((request) async {
        expect(request.url.path, equals('/api/youtube/download'));
        expect(request.method, equals('POST'));

        final body = jsonDecode(request.body) as Map<String, dynamic>;
        expect(body['id'], equals('yt_789'));
        expect(body['title'], equals('Downloaded Rap'));

        final createdSongJson = {
          'id': 99991,
          'title': 'Downloaded Rap',
          'artist': 'Local Artist',
          'album': r'E:\music',
          'uri': 'http://localhost:8081/api/stream?file=Downloaded%20Rap.mp3',
          'duration': 210000,
          'dateAdded': DateTime.now().millisecondsSinceEpoch ~/ 1000,
        };

        return http.Response(
          jsonEncode(createdSongJson),
          200,
          headers: {'content-type': 'application/json'},
        );
      });

      final service = YouTubeService(client: mockClient);

      final song = await service.downloadSong(
        videoId: 'yt_789',
        title: 'Downloaded Rap',
        musicProvider: musicProvider,
      );

      expect(song.id, equals(99991));
      expect(song.title, equals('Downloaded Rap'));

      // Invariant: track must immediately be at index 0 of both lists!
      expect(musicProvider.allSongs.isNotEmpty, isTrue);
      expect(musicProvider.allSongs.first.id, equals(99991));
      expect(musicProvider.currentQueue.first.id, equals(99991));
    });

    test('downloadSong throws YouTubeServiceException on failure', () async {
      final mockClient = MockClient((request) async {
        return http.Response('Download failed', 500);
      });

      final service = YouTubeService(client: mockClient);

      expect(
        () => service.downloadSong(videoId: 'fail_id', title: 'Fail Title'),
        throwsA(isA<YouTubeServiceException>()),
      );
    });
  });

  group('YouTubeSearchView Widget Tests', () {
    late StorageService storageService;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      storageService = StorageService(prefs);
    });

    testWidgets('renders search input, performs search, and triggers download', (tester) async {
      final musicProvider = MusicProvider(storageService);

      final mockClient = MockClient((request) async {
        if (request.url.path == '/api/youtube/search') {
          final results = [
            {
              'id': 'vid_1',
              'title': 'Mock Video Title 1',
              'author': 'Channel 1',
              'duration': '03:40',
              'thumbnail': '',
            }
          ];
          return http.Response(jsonEncode(results), 200, headers: {'content-type': 'application/json'});
        }

        if (request.url.path == '/api/youtube/download') {
          final createdSong = {
            'id': 12345,
            'title': 'Mock Video Title 1',
            'artist': 'Channel 1',
            'album': r'E:\music',
            'uri': 'http://localhost:8081/api/stream?file=test.mp3',
            'duration': 220000,
            'dateAdded': DateTime.now().millisecondsSinceEpoch ~/ 1000,
          };
          return http.Response(jsonEncode(createdSong), 200, headers: {'content-type': 'application/json'});
        }

        return http.Response('Not found', 404);
      });

      final youtubeService = YouTubeService(client: mockClient);

      await tester.pumpWidget(
        MaterialApp(
          home: ChangeNotifierProvider<MusicProvider>.value(
            value: musicProvider,
            child: YouTubeSearchView(youtubeService: youtubeService),
          ),
        ),
      );

      await tester.pumpAndSettle();

      // Verify title and input bar
      expect(find.text('بحث وتحميل YouTube'), findsOneWidget);
      expect(find.byType(TextField), findsOneWidget);

      // Enter query
      await tester.enterText(find.byType(TextField), 'Mock Video');
      await tester.testTextInput.receiveAction(TextInputAction.search);
      await tester.pump(); // Start search
      await tester.pump(const Duration(milliseconds: 700)); // Debounce & response
      await tester.pumpAndSettle();

      // Result should be visible
      expect(find.text('Mock Video Title 1'), findsOneWidget);
      expect(find.text('Channel 1'), findsOneWidget);

      // Download icon should be present
      final downloadIcon = find.byIcon(Icons.download_rounded);
      expect(downloadIcon, findsOneWidget);

      // Tap download
      await tester.tap(downloadIcon);
      await tester.pump(); // Starts download
      await tester.pumpAndSettle(); // Completes download

      // SnackBar with confirmation should appear
      expect(find.textContaining('تم تحميل "Mock Video Title 1"'), findsOneWidget);

      // Invariant: MusicProvider index 0 should now be the downloaded track
      expect(musicProvider.allSongs.first.title, equals('Mock Video Title 1'));
    });

    testWidgets('STRICT CONSTRAINT: Zero equalizer controls in YouTubeSearchView', (tester) async {
      final mockClient = MockClient((request) async => http.Response('[]', 200));
      final youtubeService = YouTubeService(client: mockClient);

      await tester.pumpWidget(
        MaterialApp(
          home: YouTubeSearchView(youtubeService: youtubeService),
        ),
      );
      await tester.pumpAndSettle();

      // Ensure no sliders or EQ text
      expect(find.byType(Slider), findsNothing);
      expect(find.textContaining(RegExp(r'equalizer', caseSensitive: false)), findsNothing);
      expect(find.textContaining('إيكولايزر'), findsNothing);
      expect(find.textContaining('معادل'), findsNothing);
    });
  });
}
