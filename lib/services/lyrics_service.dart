import 'dart:async';
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../models/lyric_line.dart';
import '../models/song_model.dart';

class LyricsService {
  final http.Client _client;
  final Map<String, List<LyricLine>> _cache = {};

  LyricsService({http.Client? client}) : _client = client ?? http.Client();

  /// Cleans titles for better search hit rates on LRCLIB (e.g. removing "(Prod. ...)", ".mp3", etc.)
  static String cleanTrackTitle(String raw) {
    var title = raw;
    // Remove file extension
    title = title.replaceAll(RegExp(r'\.(mp3|m4a|wav|flac|aac|ogg)$', caseSensitive: false), '');
    // Remove track numbering like "01 - " or "1. "
    title = title.replaceAll(RegExp(r'^\d+\s*[-.]\s*'), '');
    // Remove bracketed or parenthesized additions like [Prod. ...], (Official Audio), etc.
    title = title.replaceAll(RegExp(r'\[.*?\]'), '');
    title = title.replaceAll(RegExp(r'\(.*?(official|prod|feat|audio|video|lyric).*?\)', caseSensitive: false), '');
    // Replace underscores with spaces
    title = title.replaceAll('_', ' ').trim();
    return title.isEmpty ? raw.trim() : title;
  }

  static String cleanArtistName(String raw) {
    if (raw.toLowerCase() == 'unknown artist' || raw.toLowerCase() == 'local artist') {
      return '';
    }
    return raw.trim();
  }

  /// Fetches synced lyrics for [song].
  /// Returns an empty list if not found or on network timeout.
  Future<List<LyricLine>> getLyricsForSong(SongModel song) async {
    final cacheKey = '${song.id}_${song.title}_${song.artist}';
    if (_cache.containsKey(cacheKey)) {
      return _cache[cacheKey]!;
    }

    final cleanedTitle = cleanTrackTitle(song.title);
    final cleanedArtist = cleanArtistName(song.artist);

    final durationSec = (song.duration > 0) ? (song.duration / 1000).round() : 0;

    final lyrics = await fetchSyncedLyrics(
      title: cleanedTitle,
      artist: cleanedArtist,
      durationSeconds: durationSec > 0 ? durationSec : null,
    );

    _cache[cacheKey] = lyrics;
    return lyrics;
  }

  /// Fetches synced lyrics from LRCLIB using exact match (/api/get) with fallback to (/api/search).
  Future<List<LyricLine>> fetchSyncedLyrics({
    required String title,
    String? artist,
    int? durationSeconds,
  }) async {
    if (title.trim().isEmpty) return const [];

    final headers = {
      'User-Agent': 'EchoMusicApp/1.0 (https://github.com/echomusic)',
      'Accept': 'application/json',
    };

    // 1. Try exact match /api/get if title and artist are present
    if (artist != null && artist.isNotEmpty) {
      try {
        final queryParams = <String, String>{
          'track_name': title,
          'artist_name': artist,
        };
        if (durationSeconds != null && durationSeconds > 0) {
          queryParams['duration'] = durationSeconds.toString();
        }

        final uri = Uri.https('lrclib.net', '/api/get', queryParams);
        final response = await _client.get(uri, headers: headers).timeout(
              const Duration(seconds: 5),
            );

        if (response.statusCode == 200) {
          final data = jsonDecode(response.body) as Map<String, dynamic>;
          final syncedLrc = data['syncedLyrics'] as String?;
          if (syncedLrc != null && syncedLrc.trim().isNotEmpty) {
            return LrcParser.parse(syncedLrc);
          }
        }
      } catch (e) {
        debugPrint('LRCLIB exact get error: $e');
      }
    }

    // 2. Fallback to /api/search with query string
    try {
      final query = (artist != null && artist.isNotEmpty)
          ? '$title $artist'
          : title;

      final uri = Uri.https('lrclib.net', '/api/search', {'q': query});
      final response = await _client.get(uri, headers: headers).timeout(
            const Duration(seconds: 6),
          );

      if (response.statusCode == 200) {
        final list = jsonDecode(response.body) as List<dynamic>;
        for (final item in list) {
          if (item is Map<String, dynamic>) {
            final syncedLrc = item['syncedLyrics'] as String?;
            if (syncedLrc != null && syncedLrc.trim().isNotEmpty) {
              return LrcParser.parse(syncedLrc);
            }
          }
        }
      }
    } catch (e) {
      debugPrint('LRCLIB search error: $e');
    }

    return const [];
  }

  void clearCache() {
    _cache.clear();
  }
}
