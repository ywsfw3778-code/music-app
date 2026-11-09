import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:just_audio/just_audio.dart';
import '../models/song_model.dart';

class PlayerService {
  final AudioPlayer _player = AudioPlayer();

  AudioPlayer get player => _player;

  Stream<PlayerState> get playerStateStream => _player.playerStateStream;
  Stream<Duration> get positionStream => _player.positionStream;
  Stream<Duration?> get durationStream => _player.durationStream;
  Stream<bool> get shuffleModeEnabledStream => _player.shuffleModeEnabledStream;
  Stream<LoopMode> get loopModeStream => _player.loopModeStream;

  bool get isPlaying => _player.playing;

  Future<void> playSong(SongModel song) async {
    try {
      await _player.stop();
      if (song.uri.startsWith('http://') || song.uri.startsWith('https://')) {
        await _player.setUrl(song.uri);
      } else if (song.uri.startsWith('content://')) {
        await _player.setAudioSource(AudioSource.uri(Uri.parse(song.uri)));
      } else {
        // Check if file exists, else use AudioSource.uri
        try {
          await _player.setFilePath(song.uri);
        } catch (_) {
          await _player.setAudioSource(AudioSource.uri(Uri.parse(song.uri)));
        }
      }
      await _player.play();
    } catch (e) {
      debugPrint('Error playing song (${song.title}): $e');
    }
  }

  Future<void> resume() async {
    await _player.play();
  }

  Future<void> pause() async {
    await _player.pause();
  }

  Future<void> seek(Duration position) async {
    await _player.seek(position);
  }

  Future<void> toggleShuffle(bool enable) async {
    await _player.setShuffleModeEnabled(enable);
  }

  Future<void> cycleRepeatMode() async {
    final current = _player.loopMode;
    switch (current) {
      case LoopMode.off:
        await _player.setLoopMode(LoopMode.all);
        break;
      case LoopMode.all:
        await _player.setLoopMode(LoopMode.one);
        break;
      case LoopMode.one:
        await _player.setLoopMode(LoopMode.off);
        break;
    }
  }

  void dispose() {
    _player.dispose();
  }
}
