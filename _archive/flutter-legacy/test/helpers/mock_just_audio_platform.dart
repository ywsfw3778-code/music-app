// ignore_for_file: depend_on_referenced_packages
import 'dart:async';
import 'package:just_audio_platform_interface/just_audio_platform_interface.dart';

class MockAudioPlayerPlatform extends AudioPlayerPlatform {
  final _eventController = StreamController<PlaybackEventMessage>.broadcast();
  final _dataController = StreamController<PlayerDataMessage>.broadcast();

  double currentVolume = 1.0;
  bool isPlaying = false;
  LoopModeMessage loopMode = LoopModeMessage.off;
  ShuffleModeMessage shuffleMode = ShuffleModeMessage.none;
  Duration position = Duration.zero;
  Duration trackDuration = const Duration(seconds: 180);

  MockAudioPlayerPlatform(super.id);

  @override
  Stream<PlaybackEventMessage> get playbackEventMessageStream => _eventController.stream;

  @override
  Stream<PlayerDataMessage> get playerDataMessageStream => _dataController.stream;

  void emitPlaybackEvent({
    ProcessingStateMessage state = ProcessingStateMessage.ready,
    Duration? currentPos,
  }) {
    if (currentPos != null) {
      position = currentPos;
    }
    _eventController.add(PlaybackEventMessage(
      processingState: state,
      updateTime: DateTime.now(),
      updatePosition: position,
      bufferedPosition: position,
      duration: trackDuration,
      icyMetadata: null,
      currentIndex: 0,
      androidAudioSessionId: null,
    ));
  }

  void emitPlayerData({bool? playing, double? volume, LoopModeMessage? loop, ShuffleModeMessage? shuffle}) {
    if (playing != null) isPlaying = playing;
    if (volume != null) currentVolume = volume;
    if (loop != null) loopMode = loop;
    if (shuffle != null) shuffleMode = shuffle;
    _dataController.add(PlayerDataMessage(
      playing: isPlaying,
      volume: currentVolume,
      loopMode: loopMode,
      shuffleMode: shuffleMode,
    ));
  }

  @override
  Future<LoadResponse> load(LoadRequest request) async {
    emitPlaybackEvent(state: ProcessingStateMessage.ready);
    return LoadResponse(duration: trackDuration);
  }

  @override
  Future<PlayResponse> play(PlayRequest request) async {
    isPlaying = true;
    emitPlayerData(playing: true);
    emitPlaybackEvent(state: ProcessingStateMessage.ready);
    return PlayResponse();
  }

  @override
  Future<SetSpeedResponse> setSpeed(SetSpeedRequest request) async {
    return SetSpeedResponse();
  }

  @override
  Future<SetPitchResponse> setPitch(SetPitchRequest request) async {
    return SetPitchResponse();
  }

  @override
  Future<PauseResponse> pause(PauseRequest request) async {
    isPlaying = false;
    emitPlayerData(playing: false);
    return PauseResponse();
  }

  @override
  Future<DisposeResponse> dispose(DisposeRequest request) async {
    isPlaying = false;
    await _eventController.close();
    await _dataController.close();
    return DisposeResponse();
  }

  @override
  Future<SetVolumeResponse> setVolume(SetVolumeRequest request) async {
    currentVolume = request.volume;
    emitPlayerData(volume: request.volume);
    return SetVolumeResponse();
  }

  @override
  Future<SetLoopModeResponse> setLoopMode(SetLoopModeRequest request) async {
    loopMode = request.loopMode;
    emitPlayerData(loop: request.loopMode);
    return SetLoopModeResponse();
  }

  @override
  Future<SetShuffleModeResponse> setShuffleMode(SetShuffleModeRequest request) async {
    shuffleMode = request.shuffleMode;
    emitPlayerData(shuffle: request.shuffleMode);
    return SetShuffleModeResponse();
  }

  @override
  Future<SeekResponse> seek(SeekRequest request) async {
    if (request.position != null) {
      position = request.position!;
      emitPlaybackEvent(state: ProcessingStateMessage.ready, currentPos: position);
    }
    return SeekResponse();
  }

}

class MockJustAudioPlatform extends JustAudioPlatform {
  final Map<String, MockAudioPlayerPlatform> players = {};

  static void install() {
    JustAudioPlatform.instance = MockJustAudioPlatform();
  }

  @override
  Future<AudioPlayerPlatform> init(InitRequest request) async {
    final player = MockAudioPlayerPlatform(request.id);
    players[request.id] = player;
    return player;
  }

  @override
  Future<DisposePlayerResponse> disposePlayer(DisposePlayerRequest request) async {
    final player = players.remove(request.id);
    if (player != null) {
      await player.dispose(DisposeRequest());
    }
    return DisposePlayerResponse();
  }

  @override
  Future<DisposeAllPlayersResponse> disposeAllPlayers(DisposeAllPlayersRequest request) async {
    for (final player in players.values) {
      await player.dispose(DisposeRequest());
    }
    players.clear();
    return DisposeAllPlayersResponse();
  }
}
