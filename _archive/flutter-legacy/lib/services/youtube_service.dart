import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/song_model.dart';
import '../providers/music_provider.dart';

class YouTubeSearchResult {
  final String id;
  final String title;
  final String author;
  final String duration;
  final String thumbnail;

  const YouTubeSearchResult({
    required this.id,
    required this.title,
    required this.author,
    required this.duration,
    required this.thumbnail,
  });

  factory YouTubeSearchResult.fromJson(Map<String, dynamic> json) {
    return YouTubeSearchResult(
      id: json['id'] as String? ?? '',
      title: json['title'] as String? ?? 'Untitled',
      author: json['author'] as String? ?? 'Unknown Artist',
      duration: json['duration'] as String? ?? '00:00',
      thumbnail: json['thumbnail'] as String? ?? '',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'author': author,
      'duration': duration,
      'thumbnail': thumbnail,
    };
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is YouTubeSearchResult &&
          runtimeType == other.runtimeType &&
          id == other.id;

  @override
  int get hashCode => id.hashCode;
}

class YouTubeServiceException implements Exception {
  final String message;
  final int? statusCode;

  const YouTubeServiceException(this.message, {this.statusCode});

  @override
  String toString() => 'YouTubeServiceException: $message (status: $statusCode)';
}

class YouTubeService {
  final String baseUrl;
  final http.Client _client;

  YouTubeService({
    this.baseUrl = 'http://localhost:8081',
    http.Client? client,
  }) : _client = client ?? http.Client();

  /// Searches YouTube audio tracks via the companion server.
  Future<List<YouTubeSearchResult>> search(String query) async {
    final cleanQuery = query.trim();
    if (cleanQuery.isEmpty) return const [];

    final encoded = Uri.encodeComponent(cleanQuery);
    final url = Uri.parse('$baseUrl/api/youtube/search?q=$encoded');

    try {
      final response = await _client.get(
        url,
        headers: {'Accept': 'application/json'},
      ).timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final decoded = jsonDecode(response.body);
        if (decoded is List) {
          return decoded
              .map((item) => YouTubeSearchResult.fromJson(item as Map<String, dynamic>))
              .toList();
        }
        return const [];
      } else {
        throw YouTubeServiceException(
          'Failed to search YouTube: ${response.body}',
          statusCode: response.statusCode,
        );
      }
    } on TimeoutException {
      throw const YouTubeServiceException('YouTube search timed out. Check companion server.');
    } catch (e) {
      if (e is YouTubeServiceException) rethrow;
      throw YouTubeServiceException('Cannot connect to companion server: $e');
    }
  }

  /// Downloads audio from YouTube as MP3 into E:\music and prepends to the library.
  Future<SongModel> downloadSong({
    required String videoId,
    required String title,
    MusicProvider? musicProvider,
  }) async {
    final url = Uri.parse('$baseUrl/api/youtube/download');
    final payload = jsonEncode({
      'id': videoId,
      'title': title,
    });

    try {
      final response = await _client
          .post(
            url,
            headers: {
              'Content-Type': 'application/json',
              'Accept': 'application/json',
            },
            body: payload,
          )
          .timeout(const Duration(seconds: 60));

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final song = SongModel.fromJson(data);

        // Prepend immediately to index 0 so it appears at top of playlist
        if (musicProvider != null) {
          musicProvider.prependSong(song);
        }

        return song;
      } else {
        throw YouTubeServiceException(
          'Download failed with status ${response.statusCode}: ${response.body}',
          statusCode: response.statusCode,
        );
      }
    } on TimeoutException {
      throw const YouTubeServiceException('Download timed out. Please try again.');
    } catch (e) {
      if (e is YouTubeServiceException) rethrow;
      throw YouTubeServiceException('Download error: $e');
    }
  }
}
