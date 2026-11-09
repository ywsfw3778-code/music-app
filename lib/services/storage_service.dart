import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/playlist_model.dart';

class StorageService {
  static const String _keyPlayCounts = 'play_counts';
  static const String _keyFavorites = 'favorite_songs';
  static const String _keyPlaylists = 'user_playlists';
  static const String _keyLastSongId = 'last_played_song_id';

  final SharedPreferences _prefs;

  StorageService(this._prefs);

  static Future<StorageService> init() async {
    final prefs = await SharedPreferences.getInstance();
    return StorageService(prefs);
  }

  // Play Counts
  Map<int, int> getPlayCounts() {
    final raw = _prefs.getString(_keyPlayCounts);
    if (raw == null) return {};
    try {
      final decoded = jsonDecode(raw) as Map<String, dynamic>;
      return decoded.map((k, v) => MapEntry(int.parse(k), v as int));
    } catch (_) {
      return {};
    }
  }

  Future<void> incrementPlayCount(int songId) async {
    final counts = getPlayCounts();
    counts[songId] = (counts[songId] ?? 0) + 1;
    final encoded = jsonEncode(counts.map((k, v) => MapEntry(k.toString(), v)));
    await _prefs.setString(_keyPlayCounts, encoded);
  }

  // Favorites
  Set<int> getFavorites() {
    final list = _prefs.getStringList(_keyFavorites) ?? [];
    return list.map((e) => int.tryParse(e) ?? 0).where((id) => id != 0).toSet();
  }

  Future<void> toggleFavorite(int songId) async {
    final favs = getFavorites();
    if (favs.contains(songId)) {
      favs.remove(songId);
    } else {
      favs.add(songId);
    }
    await _prefs.setStringList(_keyFavorites, favs.map((e) => e.toString()).toList());
  }

  // Playlists
  List<PlaylistModel> getPlaylists() {
    final raw = _prefs.getStringList(_keyPlaylists) ?? [];
    return raw.map((e) {
      try {
        return PlaylistModel.fromJson(jsonDecode(e) as Map<String, dynamic>);
      } catch (_) {
        return null;
      }
    }).whereType<PlaylistModel>().toList();
  }

  Future<void> savePlaylists(List<PlaylistModel> playlists) async {
    final list = playlists.map((p) => jsonEncode(p.toJson())).toList();
    await _prefs.setStringList(_keyPlaylists, list);
  }

  // Last Played
  int? getLastSongId() {
    return _prefs.getInt(_keyLastSongId);
  }

  Future<void> saveLastSongId(int songId) async {
    await _prefs.setInt(_keyLastSongId, songId);
  }
}
