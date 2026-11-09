import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:on_audio_query/on_audio_query.dart' as oaq;
import 'package:permission_handler/permission_handler.dart';
import '../models/song_model.dart';

class AudioScannerService {
  final oaq.OnAudioQuery _audioQuery = oaq.OnAudioQuery();

  static final List<String> _blacklistPathKeywords = [
    'whatsapp',
    'voice notes',
    'voice_notes',
    'recordings',
    'sound_recorder',
    'soundrecorder',
    'call_recorder',
    'callrecording',
    'telegram',
    'notifications',
    'ringtones',
    'alarms',
    'voice_memo',
    'audiomemo',
    'mic_rec',
  ];

  static final List<String> _blacklistTitlePrefixes = [
    'aud-',
    'ptt-',
    'rec_',
    'voice_',
    'call_',
    'record_',
    'audio_',
  ];

  Future<bool> requestPermissions() async {
    if (kIsWeb) return true;

    if (Platform.isAndroid) {
      final audioStatus = await Permission.audio.status;
      if (audioStatus.isGranted) return true;

      final reqAudio = await Permission.audio.request();
      if (reqAudio.isGranted) return true;

      final storageStatus = await Permission.storage.request();
      return storageStatus.isGranted;
    } else if (Platform.isIOS) {
      final mediaStatus = await Permission.mediaLibrary.request();
      return mediaStatus.isGranted;
    }
    return true;
  }

  /// Scans device and returns ONLY pure music tracks sorted by Latest Downloaded
  Future<List<SongModel>> scanMusic() async {
    List<SongModel> songs = [];

    try {
      if (Platform.isAndroid || Platform.isIOS) {
        final hasPermission = await requestPermissions();
        if (!hasPermission) {
          debugPrint('Storage/Audio permission not granted');
          return _getSampleTracks();
        }

        // Query sorted by DATE_ADDED DESC (Latest downloaded first)
        final rawSongs = await _audioQuery.querySongs(
          sortType: oaq.SongSortType.DATE_ADDED,
          orderType: oaq.OrderType.DESC_OR_GREATER,
          uriType: oaq.UriType.EXTERNAL,
          ignoreCase: true,
        );

        for (final item in rawSongs) {
          if (_isStrictlyMusic(item)) {
            // Prefer content uri for playback, or data path
            final playbackUri = item.uri != null && item.uri!.isNotEmpty
                ? item.uri!
                : item.data;

            songs.add(
              SongModel(
                id: item.id,
                title: _cleanTitle(item.title),
                artist: _cleanArtist(item.artist),
                album: item.album ?? 'Single',
                uri: playbackUri,
                duration: item.duration ?? 0,
                albumId: item.albumId,
                dateAdded: item.dateAdded ?? 0,
              ),
            );
          }
        }
      } else if (Platform.isWindows) {
        songs = await _scanWindowsMusic();
      }
    } catch (e) {
      debugPrint('Error scanning music: $e');
    }

    if (songs.isEmpty) {
      songs = _getSampleTracks();
    }

    return songs;
  }

  bool _isStrictlyMusic(oaq.SongModel item) {
    final duration = item.duration ?? 0;
    if (duration < 40000) return false;

    if (item.isMusic == false) return false;
    if (item.isNotification == true) return false;
    if (item.isRingtone == true) return false;
    if (item.isAlarm == true) return false;
    if (item.isPodcast == true) return false;

    final path = (item.data).toLowerCase();
    for (final keyword in _blacklistPathKeywords) {
      if (path.contains(keyword)) return false;
    }

    final title = (item.title).toLowerCase().trim();
    for (final prefix in _blacklistTitlePrefixes) {
      if (title.startsWith(prefix)) return false;
    }

    return true;
  }

  String _cleanTitle(String title) {
    var cleaned = title.replaceAll(RegExp(r'\.(mp3|m4a|wav|flac|aac|ogg)$', caseSensitive: false), '');
    cleaned = cleaned.replaceAll('_', ' ').trim();
    return cleaned.isEmpty ? 'Unknown Track' : cleaned;
  }

  String _cleanArtist(String? artist) {
    if (artist == null || artist.trim().isEmpty || artist == '<unknown>') {
      return 'Unknown Artist';
    }
    return artist.trim();
  }

  Future<List<SongModel>> _scanWindowsMusic() async {
    final List<SongModel> songs = [];
    try {
      final userProfile = Platform.environment['USERPROFILE'];
      if (userProfile == null) return [];

      final musicDir = Directory('$userProfile\\Music');
      if (await musicDir.exists()) {
        final files = musicDir.listSync(recursive: false);
        int fakeId = 1000;
        for (final f in files) {
          if (f is File) {
            final ext = f.path.split('.').last.toLowerCase();
            if (['mp3', 'm4a', 'flac', 'wav'].contains(ext)) {
              final fileName = f.uri.pathSegments.last;
              songs.add(
                SongModel(
                  id: fakeId++,
                  title: _cleanTitle(fileName),
                  artist: 'Local Music',
                  album: 'Desktop Music',
                  uri: f.path,
                  duration: 210000,
                  dateAdded: DateTime.now().millisecondsSinceEpoch ~/ 1000,
                ),
              );
            }
          }
        }
      }
    } catch (_) {}
    return songs;
  }

  List<SongModel> _getSampleTracks() {
    return [
      SongModel(
        id: 9001,
        title: 'Midnight Echoes',
        artist: 'Cyberwave Synth',
        album: 'Neon Horizon',
        uri: 'https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3',
        duration: 147000,
        playCount: 15,
        isFavorite: true,
        dateAdded: 1700000004,
      ),
      SongModel(
        id: 9002,
        title: 'Chill Lofi Vibes',
        artist: 'Urban Melody',
        album: 'Coffee & Code',
        uri: 'https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=chill-abstract-intention-12099.mp3',
        duration: 132000,
        playCount: 28,
        isFavorite: true,
        dateAdded: 1700000003,
      ),
      SongModel(
        id: 9003,
        title: 'Aurora Lights',
        artist: 'Ambient Dreams',
        album: 'Cosmic Journey',
        uri: 'https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3?filename=ambient-piano-amp-strings-10711.mp3',
        duration: 184000,
        playCount: 9,
        dateAdded: 1700000002,
      ),
      SongModel(
        id: 9004,
        title: 'Summer Drive',
        artist: 'Solar Flare',
        album: 'Retro Wave',
        uri: 'https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=lifelike-126735.mp3',
        duration: 165000,
        playCount: 34,
        isFavorite: true,
        dateAdded: 1700000001,
      ),
    ];
  }
}
