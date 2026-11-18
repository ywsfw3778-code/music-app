import 'dart:async';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../models/lyric_line.dart';
import '../../models/song_model.dart';
import '../../providers/music_provider.dart';
import '../../services/lyrics_service.dart';

class SyncedLyricsWidget extends StatefulWidget {
  final SongModel? song;
  final List<LyricLine>? initialLyrics;
  final LyricsService? lyricsService;
  final Color? activeColor;
  final Color? inactiveColor;
  final void Function(Duration timestamp)? onSeek;
  final bool autoScroll;

  const SyncedLyricsWidget({
    super.key,
    this.song,
    this.initialLyrics,
    this.lyricsService,
    this.activeColor,
    this.inactiveColor,
    this.onSeek,
    this.autoScroll = true,
  });

  @override
  State<SyncedLyricsWidget> createState() => _SyncedLyricsWidgetState();
}

class _SyncedLyricsWidgetState extends State<SyncedLyricsWidget> {
  late final LyricsService _lyricsService;
  final ScrollController _scrollController = ScrollController();

  List<LyricLine> _lyrics = [];
  bool _isLoading = false;
  int _lastActiveIndex = -1;
  bool _userIsScrolling = false;
  Timer? _resumeAutoScrollTimer;

  static const double _lineHeight = 56.0;

  @override
  void initState() {
    super.initState();
    _lyricsService = widget.lyricsService ?? LyricsService();

    if (widget.initialLyrics != null) {
      _lyrics = widget.initialLyrics!;
    } else if (widget.song != null) {
      _loadLyrics(widget.song!);
    }
  }

  @override
  void didUpdateWidget(covariant SyncedLyricsWidget oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.initialLyrics != null && widget.initialLyrics != oldWidget.initialLyrics) {
      setState(() {
        _lyrics = widget.initialLyrics!;
        _lastActiveIndex = -1;
      });
    } else if (widget.song?.id != oldWidget.song?.id) {
      if (widget.song != null) {
        _loadLyrics(widget.song!);
      } else {
        setState(() {
          _lyrics = [];
          _lastActiveIndex = -1;
        });
      }
    }
  }

  @override
  void dispose() {
    _resumeAutoScrollTimer?.cancel();
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> _loadLyrics(SongModel song) async {
    setState(() {
      _isLoading = true;
      _lastActiveIndex = -1;
    });

    try {
      final fetched = await _lyricsService.getLyricsForSong(song);
      if (mounted) {
        setState(() {
          _lyrics = fetched;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _lyrics = [];
          _isLoading = false;
        });
      }
    }
  }

  int _findActiveIndex(Duration currentPosition) {
    if (_lyrics.isEmpty) return -1;
    int active = -1;
    for (int i = 0; i < _lyrics.length; i++) {
      if (_lyrics[i].timestamp <= currentPosition) {
        active = i;
      } else {
        break;
      }
    }
    return active;
  }

  void _scrollToIndex(int index, double viewportHeight) {
    if (!widget.autoScroll || _userIsScrolling || !_scrollController.hasClients) {
      return;
    }

    // Center the active line in viewport
    final targetOffset = (index * _lineHeight) - (viewportHeight / 2) + (_lineHeight / 2);
    final clampedOffset = targetOffset.clamp(
      0.0,
      _scrollController.position.maxScrollExtent,
    );

    _scrollController.animateTo(
      clampedOffset,
      duration: const Duration(milliseconds: 380),
      curve: Curves.easeOutCubic,
    );
  }

  void _onUserScrollStart() {
    _userIsScrolling = true;
    _resumeAutoScrollTimer?.cancel();
    _resumeAutoScrollTimer = Timer(const Duration(seconds: 4), () {
      if (mounted) {
        setState(() => _userIsScrolling = false);
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final activeColor = widget.activeColor ?? AppColors.primary;
    final inactiveColor = widget.inactiveColor ?? Colors.white.withValues(alpha: 0.35);

    if (_isLoading) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 32,
              height: 32,
              child: CircularProgressIndicator(
                strokeWidth: 2.5,
                color: activeColor,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              'جارٍ جلب الكلمات المتزامنة...',
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.6),
                fontSize: 14,
              ),
            ),
          ],
        ),
      );
    }

    if (_lyrics.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.music_note_rounded,
                size: 42,
                color: Colors.white.withValues(alpha: 0.25),
              ),
              const SizedBox(height: 12),
              Text(
                'لا توجد كلمات متزامنة متاحة لهذه الأغنية',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.5),
                  fontSize: 15,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final viewportHeight = constraints.maxHeight;

        return Consumer<MusicProvider>(
          builder: (context, provider, _) {
            final currentPos = provider.currentPosition;
            final activeIndex = _findActiveIndex(currentPos);

            if (activeIndex != _lastActiveIndex && activeIndex >= 0) {
              _lastActiveIndex = activeIndex;
              WidgetsBinding.instance.addPostFrameCallback((_) {
                _scrollToIndex(activeIndex, viewportHeight);
              });
            }

            return NotificationListener<ScrollNotification>(
              onNotification: (notification) {
                if (notification is ScrollStartNotification &&
                    notification.dragDetails != null) {
                  _onUserScrollStart();
                }
                return false;
              },
              child: ListView.builder(
                controller: _scrollController,
                physics: const BouncingScrollPhysics(),
                padding: EdgeInsets.symmetric(
                  vertical: viewportHeight * 0.4,
                  horizontal: 20.0,
                ),
                itemCount: _lyrics.length,
                itemExtent: _lineHeight,
                itemBuilder: (context, index) {
                  final line = _lyrics[index];
                  final isActive = index == activeIndex;

                  return GestureDetector(
                    onTap: () {
                      if (widget.onSeek != null) {
                        widget.onSeek!(line.timestamp);
                      } else {
                        provider.seek(line.timestamp);
                      }
                      _userIsScrolling = false;
                      _scrollToIndex(index, viewportHeight);
                    },
                    child: Center(
                      child: AnimatedScale(
                        duration: const Duration(milliseconds: 250),
                        curve: Curves.easeOutBack,
                        scale: isActive ? 1.08 : 1.0,
                        child: AnimatedDefaultTextStyle(
                          duration: const Duration(milliseconds: 250),
                          curve: Curves.easeOutCubic,
                          style: TextStyle(
                            fontSize: isActive ? 20 : 16,
                            fontWeight: isActive ? FontWeight.bold : FontWeight.w500,
                            color: isActive ? activeColor : inactiveColor,
                            height: 1.3,
                            shadows: isActive
                                ? [
                                    Shadow(
                                      color: activeColor.withValues(alpha: 0.45),
                                      blurRadius: 16,
                                    ),
                                  ]
                                : null,
                          ),
                          textAlign: TextAlign.center,
                          child: Text(
                            line.text.isEmpty ? '• • •' : line.text,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ),
                    ),
                  );
                },
              ),
            );
          },
        );
      },
    );
  }
}
