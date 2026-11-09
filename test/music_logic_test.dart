import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:music_player/models/song_model.dart';
import 'package:music_player/models/playlist_model.dart';
import 'package:music_player/services/storage_service.dart';

void main() {
  group('SongModel Tests', () {
    test('Correctly serializes and deserializes from JSON', () {
      final song = SongModel(
        id: 101,
        title: 'Midnight Beats',
        artist: 'Wave Artist',
        album: 'Cyber Album',
        uri: '/storage/music/song.mp3',
        duration: 180000,
        playCount: 12,
        isFavorite: true,
      );

      final json = song.toJson();
      expect(json['id'], 101);
      expect(json['playCount'], 12);
      expect(json['isFavorite'], true);

      final fromJson = SongModel.fromJson(json);
      expect(fromJson.id, song.id);
      expect(fromJson.title, song.title);
      expect(fromJson.duration, song.duration);
      expect(fromJson.playCount, song.playCount);
      expect(fromJson.isFavorite, song.isFavorite);
    });
  });

  group('PlaylistModel Tests', () {
    test('Creates playlist and serializes correctly', () {
      final playlist = PlaylistModel(
        id: 'pl_1',
        name: 'Gym Workout',
        songIds: [101, 102, 103],
        createdAt: 1700000000,
      );

      final json = playlist.toJson();
      expect(json['name'], 'Gym Workout');
      expect((json['songIds'] as List).length, 3);

      final fromJson = PlaylistModel.fromJson(json);
      expect(fromJson.name, 'Gym Workout');
      expect(fromJson.songIds, [101, 102, 103]);
    });
  });

  group('StorageService Persistent Tests', () {
    late StorageService storage;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      storage = await StorageService.init();
    });

    test('Increments and retrieves play counts correctly', () async {
      expect(storage.getPlayCounts(), isEmpty);

      await storage.incrementPlayCount(1001);
      await storage.incrementPlayCount(1001);
      await storage.incrementPlayCount(1002);

      final counts = storage.getPlayCounts();
      expect(counts[1001], 2);
      expect(counts[1002], 1);
    });

    test('Toggles favorites correctly', () async {
      expect(storage.getFavorites(), isEmpty);

      await storage.toggleFavorite(505);
      expect(storage.getFavorites().contains(505), true);

      await storage.toggleFavorite(505);
      expect(storage.getFavorites().contains(505), false);
    });

    test('Saves and loads user playlists', () async {
      final p1 = PlaylistModel(
        id: '1',
        name: 'Chill Evening',
        songIds: [1, 2],
        createdAt: 123456,
      );
      await storage.savePlaylists([p1]);

      final loaded = storage.getPlaylists();
      expect(loaded.length, 1);
      expect(loaded.first.name, 'Chill Evening');
      expect(loaded.first.songIds, [1, 2]);
    });
  });
}
