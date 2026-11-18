import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import '../models/song_model.dart';

class PlayerService {
  final AudioPlayer _playerA = AudioPlayer();
  final AudioPlayer _playerB = AudioPlayer();
  late AudioPlayer _activePlayer;
  late AudioPlayer _standbyPlayer;

  bool _crossfadeEnabled = true;
  Duration _crossfadeDuration = const Duration(seconds: 4);
  VoidCallback? onNearEnd;
  bool _nearEndTriggered = false;
  bool _isTransitioning = false;
  double _userVolume = 1.0;

  bool get isTransitioning => _isTransitioning;
  double get volume => _userVolume;

  final StreamController<PlayerState> _playerStateController = StreamController<PlayerState>.broadcast();
  final StreamController<Duration> _positionController = StreamController<Duration>.broadcast();
  final StreamController<Duration?> _durationController = StreamController<Duration?>.broadcast();
  final StreamController<bool> _shuffleController = StreamController<bool>.broadcast();
  final StreamController<LoopMode> _loopModeController = StreamController<LoopMode>.broadcast();

  StreamSubscription? _stateSub;
  StreamSubscription? _posSub;
  StreamSubscription? _durSub;
  StreamSubscription? _shuffleSub;
  StreamSubscription? _loopSub;

  int _transitionCounter = 0;

  PlayerService() {
    _activePlayer = _playerA;
    _standbyPlayer = _playerB;
    _bindActivePlayerStreams();
  }

  AudioPlayer get player => _activePlayer;

  bool get crossfadeEnabled => _crossfadeEnabled;
  Duration get crossfadeDuration => _crossfadeDuration;

  void setCrossfadeEnabled(bool enabled) {
    _crossfadeEnabled = enabled;
  }

  void setCrossfadeDuration(Duration duration) {
    final seconds = duration.inSeconds.clamp(1, 12);
    _crossfadeDuration = Duration(seconds: seconds);
  }

  Stream<PlayerState> get playerStateStream => _playerStateController.stream;
  Stream<Duration> get positionStream => _positionController.stream;
  Stream<Duration?> get durationStream => _durationController.stream;
  Stream<bool> get shuffleModeEnabledStream => _shuffleController.stream;
  Stream<LoopMode> get loopModeStream => _loopModeController.stream;

  bool get isPlaying => _activePlayer.playing;

  void _bindActivePlayerStreams() {
    _stateSub?.cancel();
    _posSub?.cancel();
    _durSub?.cancel();
    _shuffleSub?.cancel();
    _loopSub?.cancel();

    _stateSub = _activePlayer.playerStateStream.listen((state) {
      if (!_playerStateController.isClosed) {
        _playerStateController.add(state);
      }
    });

    _posSub = _activePlayer.positionStream.listen((pos) {
      if (!_positionController.isClosed) {
        _positionController.add(pos);
      }
      _checkNearEnd(pos);
    });

    _durSub = _activePlayer.durationStream.listen((dur) {
      if (!_durationController.isClosed) {
        _durationController.add(dur);
      }
    });

    _shuffleSub = _activePlayer.shuffleModeEnabledStream.listen((shuf) {
      if (!_shuffleController.isClosed) {
        _shuffleController.add(shuf);
      }
    });

    _loopSub = _activePlayer.loopModeStream.listen((loop) {
      if (!_loopModeController.isClosed) {
        _loopModeController.add(loop);
      }
    });
  }

  void _checkNearEnd(Duration position) {
    if (!_crossfadeEnabled || onNearEnd == null || _nearEndTriggered) {
      return;
    }
    final total = _activePlayer.duration;
    if (total == null || total <= _crossfadeDuration * 2) {
      return;
    }

    final threshold = total - _crossfadeDuration;
    if (position >= threshold) {
      _nearEndTriggered = true;
      onNearEnd?.call();
    }
  }

  Future<void> _loadSource(AudioPlayer p, SongModel song) async {
    if (song.uri.startsWith('http://') || song.uri.startsWith('https://')) {
      await p.setUrl(song.uri);
    } else if (song.uri.startsWith('content://')) {
      await p.setAudioSource(AudioSource.uri(Uri.parse(song.uri)));
    } else if (kIsWeb) {
      await p.setAudioSource(AudioSource.uri(Uri.parse(song.uri)));
    } else {
      try {
        await p.setFilePath(song.uri);
      } catch (_) {
        await p.setAudioSource(AudioSource.uri(Uri.parse(song.uri)));
      }
    }
  }

  Future<void> playSong(SongModel song, {bool? enableCrossfade}) async {
    final useFade = (enableCrossfade ?? _crossfadeEnabled) && _activePlayer.playing && _crossfadeDuration > Duration.zero;
    _nearEndTriggered = false;

    final transitionId = ++_transitionCounter;

    try {
      if (useFade) {
        _isTransitioning = true;
        final outgoing = _activePlayer;
        final incoming = _standbyPlayer;

        // Synchronize shuffle, loop mode, and user volume before deck swap
        await incoming.setShuffleModeEnabled(_activePlayer.shuffleModeEnabled);
        await incoming.setLoopMode(_activePlayer.loopMode);

        // Prepare incoming player at silent volume
        await incoming.setVolume(0.0);
        await _loadSource(incoming, song);

        // Swap active references
        _activePlayer = incoming;
        _standbyPlayer = outgoing;
        _bindActivePlayerStreams();

        // Start incoming
        await incoming.play();

        // Ramp volumes simultaneously
        _runCrossfadeRamp(outgoing, incoming, _crossfadeDuration, transitionId);
      } else {
        _isTransitioning = false;
        await _activePlayer.stop();
        await _activePlayer.setVolume(_userVolume);
        await _loadSource(_activePlayer, song);
        await _activePlayer.play();
      }
    } catch (e) {
      _isTransitioning = false;
      debugPrint('Error playing song (${song.title}): $e');
    }
  }

  Future<void> _runCrossfadeRamp(AudioPlayer outgoing, AudioPlayer incoming, Duration duration, int transitionId) async {
    const int steps = 20;
    final int stepMs = (duration.inMilliseconds / steps).round();

    try {
      for (int i = 1; i <= steps; i++) {
        if (_transitionCounter != transitionId) {
          break;
        }
        final double progress = i / steps;
        final double outVol = ((1.0 - progress) * _userVolume).clamp(0.0, 1.0);
        final double inVol = (progress * _userVolume).clamp(0.0, 1.0);

        try {
          await outgoing.setVolume(outVol);
          await incoming.setVolume(inVol);
        } catch (_) {}

        await Future.delayed(Duration(milliseconds: stepMs));
      }

      if (_transitionCounter == transitionId) {
        try {
          await outgoing.stop();
          await outgoing.setVolume(_userVolume);
          await incoming.setVolume(_userVolume);
        } catch (_) {}
      }
    } finally {
      if (_transitionCounter == transitionId) {
        _isTransitioning = false;
      }
    }
  }

  Future<void> resume() async {
    await _activePlayer.play();
  }

  Future<void> pause() async {
    await _activePlayer.pause();
  }

  Future<void> stop() async {
    _transitionCounter++;
    _isTransitioning = false;
    _nearEndTriggered = false;
    await _activePlayer.stop();
    await _standbyPlayer.stop();
    await _activePlayer.setVolume(_userVolume);
    await _standbyPlayer.setVolume(_userVolume);
  }

  Future<void> seek(Duration position) async {
    _nearEndTriggered = false;
    await _activePlayer.seek(position);
  }

  Future<void> toggleShuffle(bool enable) async {
    await _activePlayer.setShuffleModeEnabled(enable);
    await _standbyPlayer.setShuffleModeEnabled(enable);
  }

  Future<void> cycleRepeatMode() async {
    final current = _activePlayer.loopMode;
    LoopMode next;
    switch (current) {
      case LoopMode.off:
        next = LoopMode.all;
        break;
      case LoopMode.all:
        next = LoopMode.one;
        break;
      case LoopMode.one:
        next = LoopMode.off;
        break;
    }
    await _activePlayer.setLoopMode(next);
    await _standbyPlayer.setLoopMode(next);
  }

  Future<void> setVolume(double volume) async {
    _userVolume = volume.clamp(0.0, 1.0);
    await _activePlayer.setVolume(_userVolume);
    await _standbyPlayer.setVolume(_userVolume);
  }

  void dispose() {
    _stateSub?.cancel();
    _posSub?.cancel();
    _durSub?.cancel();
    _shuffleSub?.cancel();
    _loopSub?.cancel();

    _playerStateController.close();
    _positionController.close();
    _durationController.close();
    _shuffleController.close();
    _loopModeController.close();

    _playerA.dispose();
    _playerB.dispose();
  }
}
