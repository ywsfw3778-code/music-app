import 'dart:math';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import '../core/constants/app_colors.dart';
import '../models/song_model.dart';
import '../models/playlist_model.dart';
import '../services/audio_scanner_service.dart';
import '../services/player_service.dart';
import '../services/storage_service.dart';

class MusicProvider extends ChangeNotifier {
  final AudioScannerService _scannerService;
  final PlayerService _playerService;
  final StorageService _storageService;

  List<SongModel> _allSongs = [];
  List<SongModel> _currentQueue = [];
  SongModel? _currentSong;
  int _currentIndex = -1;

  bool _isLoading = true;
  bool _isPlaying = false;
  Duration _currentPosition = Duration.zero;
  Duration _totalDuration = Duration.zero;
  bool _isShuffle = false;
  LoopMode _loopMode = LoopMode.off;

  List<PlaylistModel> _playlists = [];
  Set<int> _favoriteIds = {};
  Map<int, int> _playCounts = {};
  AppThemeStyle _themeStyle = AppThemeStyle.visionOS;

  MusicProvider(
    this._storageService, {
    AudioScannerService? scannerService,
    PlayerService? playerService,
  })  : _scannerService = scannerService ?? AudioScannerService(),
        _playerService = playerService ?? PlayerService() {
    _init();
  }

  // Getters
  List<SongModel> get allSongs => _allSongs;
  List<SongModel> get currentQueue => _currentQueue;
  SongModel? get currentSong => _currentSong;
  bool get isLoading => _isLoading;
  bool get isPlaying => _isPlaying;
  Duration get currentPosition => _currentPosition;
  Duration get totalDuration => _totalDuration;
  bool get isShuffle => _isShuffle;
  LoopMode get loopMode => _loopMode;
  List<PlaylistModel> get playlists => _playlists;
  AppThemeStyle get themeStyle => _themeStyle;

  void setThemeStyle(AppThemeStyle style) {
    _themeStyle = style;
    AppColors.currentStyle = style;
    notifyListeners();
  }

  // Most Played List
  List<SongModel> get mostPlayedSongs {
    final list = _allSongs.where((s) => (s.playCount > 0)).toList();
    list.sort((a, b) => b.playCount.compareTo(a.playCount));
    return list.take(20).toList();
  }

  // Favorites
  List<SongModel> get favoriteSongs {
    return _allSongs.where((s) => _favoriteIds.contains(s.id)).toList();
  }

  Future<void> _init() async {
    _playlists = _storageService.getPlaylists();
    _favoriteIds = _storageService.getFavorites();
    _playCounts = _storageService.getPlayCounts();

    _setupAudioStreams();
    await refreshSongs();
  }

  void _setupAudioStreams() {
    _playerService.playerStateStream.listen((state) {
      if (_isPlaying != state.playing) {
        _isPlaying = state.playing;
        notifyListeners();
      }
      if (state.processingState == ProcessingState.completed) {
        _onSongCompleted();
      }
    });

    _playerService.positionStream.listen((pos) {
      if (_currentPosition.inSeconds != pos.inSeconds) {
        _currentPosition = pos;
        notifyListeners();
      }
    });

    _playerService.durationStream.listen((dur) {
      if (dur != null && _totalDuration != dur) {
        _totalDuration = dur;
        notifyListeners();
      }
    });

    _playerService.shuffleModeEnabledStream.listen((shuffle) {
      if (_isShuffle != shuffle) {
        _isShuffle = shuffle;
        notifyListeners();
      }
    });

    _playerService.loopModeStream.listen((mode) {
      if (_loopMode != mode) {
        _loopMode = mode;
        notifyListeners();
      }
    });
  }

  Future<void> refreshSongs() async {
    _isLoading = true;
    notifyListeners();

    final scanned = await _scannerService.scanMusic();

    // Map persistent counts & favorites
    final mapped = scanned.map((s) {
      final count = _playCounts[s.id] ?? s.playCount;
      final isFav = _favoriteIds.contains(s.id);
      return s.copyWith(playCount: count, isFavorite: isFav);
    }).toList();

    // Default Sort: Latest downloaded (dateAdded DESC)
    mapped.sort((a, b) => b.dateAdded.compareTo(a.dateAdded));
    _allSongs = mapped;

    if (_currentQueue.isEmpty && _allSongs.isNotEmpty) {
      _currentQueue = List.from(_allSongs);
      _currentSong = _allSongs.first;
      _currentIndex = 0;
    }

    _isLoading = false;
    notifyListeners();
  }

  void playSong(SongModel song, {List<SongModel>? queue}) {
    if (queue != null) {
      _currentQueue = List.from(queue);
    } else if (_currentQueue.isEmpty) {
      _currentQueue = List.from(_allSongs);
    }

    _currentIndex = _currentQueue.indexWhere((s) => s.id == song.id);
    _currentSong = song;

    song.playCount += 1;
    _playCounts[song.id] = song.playCount;
    _storageService.incrementPlayCount(song.id);
    _storageService.saveLastSongId(song.id);

    _playerService.playSong(song);
    notifyListeners();
  }

  void togglePlayPause() {
    if (_currentSong == null && _allSongs.isNotEmpty) {
      playSong(_allSongs.first);
      return;
    }

    if (_isPlaying) {
      _playerService.pause();
    } else {
      _playerService.resume();
    }
  }

  void seek(Duration position) {
    _playerService.seek(position);
  }

  void playNext() {
    if (_currentQueue.isEmpty) return;

    if (_isShuffle) {
      final random = Random();
      int nextIndex = random.nextInt(_currentQueue.length);
      if (nextIndex == _currentIndex && _currentQueue.length > 1) {
        nextIndex = (nextIndex + 1) % _currentQueue.length;
      }
      playSong(_currentQueue[nextIndex]);
      return;
    }

    if (_currentIndex + 1 < _currentQueue.length) {
      playSong(_currentQueue[_currentIndex + 1]);
    } else if (_loopMode == LoopMode.all) {
      playSong(_currentQueue.first);
    }
  }

  void playPrevious() {
    if (_currentQueue.isEmpty) return;

    if (_currentPosition.inSeconds > 4) {
      seek(Duration.zero);
      return;
    }

    if (_currentIndex > 0) {
      playSong(_currentQueue[_currentIndex - 1]);
    } else if (_loopMode == LoopMode.all) {
      playSong(_currentQueue.last);
    }
  }

  void _onSongCompleted() {
    if (_loopMode == LoopMode.one && _currentSong != null) {
      playSong(_currentSong!);
    } else {
      playNext();
    }
  }

  void toggleShuffle() {
    _playerService.toggleShuffle(!_isShuffle);
  }

  void cycleRepeatMode() {
    _playerService.cycleRepeatMode();
  }

  Future<void> toggleFavorite(SongModel song) async {
    await _storageService.toggleFavorite(song.id);
    if (_favoriteIds.contains(song.id)) {
      _favoriteIds.remove(song.id);
      song.isFavorite = false;
    } else {
      _favoriteIds.add(song.id);
      song.isFavorite = true;
    }
    notifyListeners();
  }

  // Playlist Management
  Future<void> createPlaylist(String name) async {
    final newPlaylist = PlaylistModel(
      id: DateTime.now().millisecondsSinceEpoch.toString(),
      name: name,
      songIds: [],
      createdAt: DateTime.now().millisecondsSinceEpoch,
    );
    _playlists.add(newPlaylist);
    await _storageService.savePlaylists(_playlists);
    notifyListeners();
  }

  Future<void> deletePlaylist(String id) async {
    _playlists.removeWhere((p) => p.id == id);
    await _storageService.savePlaylists(_playlists);
    notifyListeners();
  }

  Future<void> toggleSongInPlaylist(String playlistId, int songId) async {
    final index = _playlists.indexWhere((p) => p.id == playlistId);
    if (index != -1) {
      final list = List<int>.from(_playlists[index].songIds);
      if (list.contains(songId)) {
        list.remove(songId);
      } else {
        list.add(songId);
      }
      _playlists[index] = PlaylistModel(
        id: _playlists[index].id,
        name: _playlists[index].name,
        songIds: list,
        createdAt: _playlists[index].createdAt,
      );
      await _storageService.savePlaylists(_playlists);
      notifyListeners();
    }
  }

  List<SongModel> getSongsForPlaylist(PlaylistModel playlist) {
    final setIds = playlist.songIds.toSet();
    return _allSongs.where((s) => setIds.contains(s.id)).toList();
  }

  @override
  void dispose() {
    _playerService.dispose();
    super.dispose();
  }
}
