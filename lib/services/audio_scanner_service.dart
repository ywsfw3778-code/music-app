import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:on_audio_query/on_audio_query.dart' as oaq;
import 'package:permission_handler/permission_handler.dart';
import '../models/song_model.dart';

class AudioScannerService {
  final oaq.OnAudioQuery _audioQuery = oaq.OnAudioQuery();

  // Folders and keywords to strictly exclude (recordings, voice notes, notifications)
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

  /// Request permissions for reading audio on Android / iOS
  Future<bool> requestPermissions() async {
    if (kIsWeb) return true;

    if (Platform.isAndroid) {
      // Android 13+ (API 33+) uses audio permission, older versions use storage
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

  /// Scans device and returns ONLY pure music tracks
  Future<List<SongModel>> scanMusic() async {
    List<SongModel> songs = [];

    try {
      if (Platform.isAndroid || Platform.isIOS) {
        final hasPermission = await requestPermissions();
        if (!hasPermission) {
          debugPrint('Storage/Audio permission not granted');
          return _getSampleTracks();
        }

        final rawSongs = await _audioQuery.querySongs(
          sortType: oaq.SongSortType.TITLE,
          orderType: oaq.OrderType.ASC_OR_SMALLER,
          uriType: oaq.UriType.EXTERNAL,
          ignoreCase: true,
        );

        for (final item in rawSongs) {
          if (_isStrictlyMusic(item)) {
            songs.add(
              SongModel(
                id: item.id,
                title: _cleanTitle(item.title),
                artist: _cleanArtist(item.artist),
                album: item.album ?? 'Single',
                uri: item.uri ?? item.data,
                duration: item.duration ?? 0,
                albumId: item.albumId,
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

    // If device has no local music files yet, return high quality sample tracks for immediate testing
    if (songs.isEmpty) {
      songs = _getSampleTracks();
    }

    return songs;
  }

  /// Strict filtering logic to ensure ONLY songs are shown
  bool _isStrictlyMusic(oaq.SongModel item) {
    // 1. Minimum duration: 40 seconds (filters out WhatsApp voice notes, UI sounds, ringtones)
    final duration = item.duration ?? 0;
    if (duration < 40000) return false;

    // 2. Check Android MediaStore flags
    if (item.isMusic == false) return false;
    if (item.isNotification == true) return false;
    if (item.isRingtone == true) return false;
    if (item.isAlarm == true) return false;
    if (item.isPodcast == true) return false;

    // 3. Blacklist path keywords (WhatsApp, Telegram, Call Recordings, Mic)
    final path = (item.data).toLowerCase();
    for (final keyword in _blacklistPathKeywords) {
      if (path.contains(keyword)) return false;
    }

    // 4. Blacklist filename / title prefixes (e.g. AUD-2023..., PTT-...)
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

  /// Scans Windows Music & User directory for desktop testing
  Future<List<SongModel>> _scanWindowsMusic() async {
    final List<SongModel> songs = [];
    try {
      final userProfile = Platform.environment['USERPROFILE'];
      if (userProfile == null) return [];

      final musicDir = Directory('$userProfile\\Music');
      if (await musicDir.exists()) {
        final files = musicDir.listSync(recursive: true);
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
                ),
              );
            }
          }
        }
      }
    } catch (_) {}
    return songs;
  }

  /// Curated royalty-free sample tracks for demonstration & zero-setup preview
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
      ),
      SongModel(
        id: 9003,
        title: 'Aurora Lights',
        artist: 'Ambient Dreams',
        album: 'Cosmic Journey',
        uri: 'https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3?filename=ambient-piano-amp-strings-10711.mp3',
        duration: 184000,
        playCount: 9,
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
      ),
    ];
  }
}
