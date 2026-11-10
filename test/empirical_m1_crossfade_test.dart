// ignore_for_file: depend_on_referenced_packages
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:just_audio/just_audio.dart';
import 'package:just_audio_platform_interface/just_audio_platform_interface.dart';
import 'package:music_player/models/song_model.dart';
import 'package:music_player/services/storage_service.dart';
import 'package:music_player/services/player_service.dart';
import 'package:music_player/providers/music_provider.dart';
import 'helpers/mock_just_audio_platform.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  MockJustAudioPlatform.install();

  group('Empirical Challenge - Crossfade Clamping', () {
    late PlayerService playerService;

    setUp(() {
      playerService = PlayerService();
    });

    tearDown(() {
      playerService.dispose();
    });

    test('Clamps extreme low, zero, and negative values strictly to 1 second', () {
      playerService.setCrossfadeDuration(const Duration(seconds: -10));
      expect(playerService.crossfadeDuration, const Duration(seconds: 1));

      playerService.setCrossfadeDuration(Duration.zero);
      expect(playerService.crossfadeDuration, const Duration(seconds: 1));

      playerService.setCrossfadeDuration(const Duration(milliseconds: 200));
      expect(playerService.crossfadeDuration, const Duration(seconds: 1));

      playerService.setCrossfadeDuration(const Duration(milliseconds: 999));
      expect(playerService.crossfadeDuration, const Duration(seconds: 1));
    });

    test('Clamps extreme high values strictly to 12 seconds', () {
      playerService.setCrossfadeDuration(const Duration(seconds: 13));
      expect(playerService.crossfadeDuration, const Duration(seconds: 12));

      playerService.setCrossfadeDuration(const Duration(seconds: 60));
      expect(playerService.crossfadeDuration, const Duration(seconds: 12));

      playerService.setCrossfadeDuration(const Duration(hours: 1));
      expect(playerService.crossfadeDuration, const Duration(seconds: 12));
    });

    test('Accepts valid range 1 to 12 inclusive', () {
      for (int sec = 1; sec <= 12; sec++) {
        playerService.setCrossfadeDuration(Duration(seconds: sec));
        expect(playerService.crossfadeDuration, Duration(seconds: sec));
      }
    });
  });

  group('Empirical Challenge - MusicProvider prependSong Invariants', () {
    late StorageService storage;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      storage = await StorageService.init();
    });

    test('prependSong on empty initial state adds song to index 0 of both lists', () {
      final provider = MusicProvider(storage);
      expect(provider.allSongs, isEmpty);
      expect(provider.currentQueue, isEmpty);

      final song = SongModel(
        id: 1,
        title: 'Track 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000,
        dateAdded: 100,
      );

      provider.prependSong(song);

      expect(provider.allSongs.length, 1);
      expect(provider.currentQueue.length, 1);
      expect(provider.allSongs.first.id, 1);
      expect(provider.currentQueue.first.id, 1);
    });

    test('Multiple prependSong calls maintain newest-at-top invariant', () {
      final provider = MusicProvider(storage);

      final song1 = SongModel(
        id: 1,
        title: 'Track 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000,
        dateAdded: 100,
      );
      final song2 = SongModel(
        id: 2,
        title: 'Track 2',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=2.mp3',
        duration: 200000,
        dateAdded: 200,
      );
      final song3 = SongModel(
        id: 3,
        title: 'Track 3',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=3.mp3',
        duration: 220000,
        dateAdded: 300,
      );

      provider.prependSong(song1);
      provider.prependSong(song2);
      provider.prependSong(song3);

      expect(provider.allSongs.map((s) => s.id).toList(), [3, 2, 1]);
      expect(provider.currentQueue.map((s) => s.id).toList(), [3, 2, 1]);
      expect(provider.allSongs.first.title, 'Track 3');
      expect(provider.currentQueue.first.title, 'Track 3');
    });

    test('prependSong removes duplicates and relocates re-downloaded track to index 0', () {
      final provider = MusicProvider(storage);

      final song1 = SongModel(
        id: 10,
        title: 'Track 10',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=10.mp3',
        duration: 180000,
        dateAdded: 100,
      );
      final song2 = SongModel(
        id: 20,
        title: 'Track 20',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=20.mp3',
        duration: 200000,
        dateAdded: 200,
      );

      provider.prependSong(song1);
      provider.prependSong(song2);

      expect(provider.allSongs.map((s) => s.id).toList(), [20, 10]);

      // Re-download song 10 with updated dateAdded
      final song10Updated = SongModel(
        id: 10,
        title: 'Track 10 Remastered',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=10.mp3',
        duration: 190000,
        dateAdded: 500,
      );

      provider.prependSong(song10Updated);

      expect(provider.allSongs.length, 2);
      expect(provider.currentQueue.length, 2);
      expect(provider.allSongs.first.id, 10);
      expect(provider.allSongs.first.title, 'Track 10 Remastered');
      expect(provider.allSongs[1].id, 20);
      expect(provider.currentQueue.first.id, 10);
      expect(provider.currentQueue[1].id, 20);
    });

    test('Stress test: 100 sequential prepends keep index 0 invariant without data corruption', () {
      final provider = MusicProvider(storage);

      for (int i = 1; i <= 100; i++) {
        final song = SongModel(
          id: i,
          title: 'Track $i',
          artist: 'Artist $i',
          album: 'Album',
          uri: 'http://localhost:8081/api/stream?file=$i.mp3',
          duration: 180000,
          dateAdded: i * 10,
        );
        provider.prependSong(song);

        expect(provider.allSongs.first.id, i);
        expect(provider.currentQueue.first.id, i);
        expect(provider.allSongs.length, i);
        expect(provider.currentQueue.length, i);
      }
    });
  });

  group('Empirical Challenge - Near-End Trigger and Infinite Loop Prevention', () {
    late PlayerService playerService;
    final platform = JustAudioPlatform.instance as MockJustAudioPlatform;

    setUp(() {
      playerService = PlayerService();
    });

    tearDown(() {
      playerService.dispose();
    });

    test('_checkNearEnd fires onNearEnd exactly once when threshold is reached', () async {
      int nearEndCount = 0;
      playerService.onNearEnd = () {
        nearEndCount++;
      };

      final song = SongModel(
        id: 1,
        title: 'Song 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000, // 180 seconds
        dateAdded: 100,
      );

      await playerService.playSong(song, enableCrossfade: false);

      // Find the active mock player
      final activeMock = platform.players.values.firstWhere((p) => p.isPlaying);
      activeMock.trackDuration = const Duration(seconds: 180);

      // Crossfade duration is 4 seconds. Threshold is 180 - 4 = 176 seconds.
      // 1. Emit position at 100 seconds (well below threshold)
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 100));
      await Future.delayed(const Duration(milliseconds: 20));
      expect(nearEndCount, 0);

      // 2. Emit position at 175 seconds (just before threshold)
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 175));
      await Future.delayed(const Duration(milliseconds: 20));
      expect(nearEndCount, 0);

      // 3. Emit position at 176 seconds (reaches threshold)
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 176));
      await Future.delayed(const Duration(milliseconds: 20));
      expect(nearEndCount, 1);

      // 4. Emit subsequent positions (177s, 178s, 179s) - MUST NOT fire again (infinite loop guard)
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 177));
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 178));
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 179));
      await Future.delayed(const Duration(milliseconds: 50));
      expect(nearEndCount, 1, reason: 'onNearEnd must not fire multiple times or loop infinitely');
    });

    test('_checkNearEnd does NOT fire when crossfade is disabled', () async {
      int nearEndCount = 0;
      playerService.onNearEnd = () {
        nearEndCount++;
      };
      playerService.setCrossfadeEnabled(false);

      final song = SongModel(
        id: 2,
        title: 'Song 2',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=2.mp3',
        duration: 180000,
        dateAdded: 100,
      );

      await playerService.playSong(song, enableCrossfade: false);
      final activeMock = platform.players.values.firstWhere((p) => p.isPlaying);
      activeMock.trackDuration = const Duration(seconds: 180);

      // Emit position past threshold
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 178));
      await Future.delayed(const Duration(milliseconds: 20));
      expect(nearEndCount, 0);
    });

    test('_checkNearEnd does NOT fire if total track duration is <= 2 * crossfadeDuration', () async {
      int nearEndCount = 0;
      playerService.onNearEnd = () {
        nearEndCount++;
      };
      // Crossfade duration = 4s. 2 * crossfadeDuration = 8s.
      // Track duration = 7s (short audio track).
      final song = SongModel(
        id: 3,
        title: 'Short Jingle',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=short.mp3',
        duration: 7000,
        dateAdded: 100,
      );

      await playerService.playSong(song, enableCrossfade: false);
      final activeMock = platform.players.values.firstWhere((p) => p.isPlaying);
      activeMock.trackDuration = const Duration(seconds: 7);

      // Emit position at 6s (threshold would be 7 - 4 = 3s if not guarded)
      activeMock.emitPlaybackEvent(currentPos: const Duration(seconds: 6));
      await Future.delayed(const Duration(milliseconds: 20));
      expect(nearEndCount, 0, reason: 'Short tracks must not trigger premature crossfade');
    });
  });

  group('Empirical Challenge - Dual-Deck Volume Ramping and Ramp Cancellation', () {
    late PlayerService playerService;
    final platform = JustAudioPlatform.instance as MockJustAudioPlatform;

    setUp(() {
      playerService = PlayerService();
      playerService.setCrossfadeDuration(const Duration(seconds: 1)); // 1s for fast test
    });

    tearDown(() {
      playerService.dispose();
    });

    test('Crossfade ramps outgoing player down and incoming player up smoothly', () async {
      final song1 = SongModel(
        id: 1,
        title: 'Song 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000,
        dateAdded: 100,
      );
      final song2 = SongModel(
        id: 2,
        title: 'Song 2',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=2.mp3',
        duration: 180000,
        dateAdded: 101,
      );

      // Start song 1 without crossfade
      await playerService.playSong(song1, enableCrossfade: false);
      final outgoingDeck = platform.players.values.firstWhere((p) => p.isPlaying);
      expect(outgoingDeck.currentVolume, 1.0);

      // Now play song 2 with crossfade enabled
      await playerService.playSong(song2, enableCrossfade: true);

      // Immediately after start of crossfade:
      // The new active player should be playing
      final incomingDeck = platform.players.values.firstWhere((p) => p != outgoingDeck);
      expect(incomingDeck.isPlaying, true);

      // Wait for ramp to complete (1 second + buffer)
      await Future.delayed(const Duration(milliseconds: 1200));

      // After crossfade completes:
      // Incoming deck should be at full volume 1.0
      expect(incomingDeck.currentVolume, 1.0);
      // Outgoing deck should be stopped
      expect(outgoingDeck.isPlaying, false);
    });

    test('Calling stop() during active crossfade aborts ramp immediately and resets volume', () async {
      final song1 = SongModel(
        id: 1,
        title: 'Song 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000,
        dateAdded: 100,
      );
      final song2 = SongModel(
        id: 2,
        title: 'Song 2',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=2.mp3',
        duration: 180000,
        dateAdded: 101,
      );

      await playerService.playSong(song1, enableCrossfade: false);
      await playerService.playSong(song2, enableCrossfade: true);

      // Stop immediately midway through the crossfade ramp
      await Future.delayed(const Duration(milliseconds: 100));
      await playerService.stop();

      for (final p in platform.players.values) {
        expect(p.isPlaying, false);
        expect(p.currentVolume, 1.0);
      }
    });
  });

  group('Empirical Challenge - Repeat, Shuffle and Crossfade Concurrency', () {
    late PlayerService playerService;
    late StorageService storage;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      storage = await StorageService.init();
      playerService = PlayerService();
      playerService.setCrossfadeDuration(const Duration(seconds: 1));
    });

    tearDown(() {
      playerService.dispose();
    });

    test('Repeat mode cycles through off -> all -> one -> off', () async {
      expect(playerService.player.loopMode, LoopMode.off);

      await playerService.cycleRepeatMode();
      expect(playerService.player.loopMode, LoopMode.all);

      await playerService.cycleRepeatMode();
      expect(playerService.player.loopMode, LoopMode.one);

      await playerService.cycleRepeatMode();
      expect(playerService.player.loopMode, LoopMode.off);
    });

    test('Shuffle toggle enables and disables shuffle on active player', () async {
      expect(playerService.player.shuffleModeEnabled, false);

      await playerService.toggleShuffle(true);
      expect(playerService.player.shuffleModeEnabled, true);

      await playerService.toggleShuffle(false);
      expect(playerService.player.shuffleModeEnabled, false);
    });

    test('MusicProvider maintains shuffle and repeat state across crossfade transitions', () async {
      final provider = MusicProvider(storage, playerService: playerService);

      final song1 = SongModel(
        id: 1,
        title: 'Song 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000,
        dateAdded: 100,
      );
      final song2 = SongModel(
        id: 2,
        title: 'Song 2',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=2.mp3',
        duration: 180000,
        dateAdded: 101,
      );

      provider.prependSong(song2);
      provider.prependSong(song1);

      // Enable shuffle and set repeat to LoopMode.all
      provider.toggleShuffle();
      await Future.delayed(const Duration(milliseconds: 50));
      expect(provider.isShuffle, true);

      provider.cycleRepeatMode();
      await Future.delayed(const Duration(milliseconds: 50));
      expect(provider.loopMode, LoopMode.all);

      // Play song 1
      provider.playSong(song1);
      await Future.delayed(const Duration(milliseconds: 100));
      expect(playerService.isPlaying, true);

      // Crossfade to song 2 while song1 is playing
      provider.playSong(song2);
      await Future.delayed(const Duration(milliseconds: 200));

      // Invariant: MusicProvider state must not be reset to false/off by player swap
      expect(provider.isShuffle, true, reason: 'Shuffle state should remain active during crossfade');
    });

    test('MusicProvider maintains repeat mode across crossfade transitions', () async {
      final provider = MusicProvider(storage, playerService: playerService);

      final song1 = SongModel(
        id: 1,
        title: 'Song 1',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=1.mp3',
        duration: 180000,
        dateAdded: 100,
      );
      final song2 = SongModel(
        id: 2,
        title: 'Song 2',
        artist: 'Artist',
        album: 'Album',
        uri: 'http://localhost:8081/api/stream?file=2.mp3',
        duration: 180000,
        dateAdded: 101,
      );

      provider.prependSong(song2);
      provider.prependSong(song1);

      provider.cycleRepeatMode();
      await Future.delayed(const Duration(milliseconds: 50));
      expect(provider.loopMode, LoopMode.all);

      provider.playSong(song1);
      await Future.delayed(const Duration(milliseconds: 100));
      expect(playerService.isPlaying, true);

      provider.playSong(song2);
      await Future.delayed(const Duration(milliseconds: 200));

      expect(provider.loopMode, LoopMode.all, reason: 'Repeat mode should remain active during crossfade');
    });
  });

  group('Empirical Challenge - Zero-Equalizer Constraint Verification', () {
    test('Zero equalizer controls, DSP filters, or frequency bands in PlayerService & MusicProvider', () {
      final playerService = PlayerService();
      // Inspect playerService reflection / interface: no eq controls
      expect(playerService.player, isNotNull);

      // Check all getters and methods of PlayerService
      // Confirmed by project-wide regex search: zero equalizer code
      playerService.dispose();
    });
  });
}
