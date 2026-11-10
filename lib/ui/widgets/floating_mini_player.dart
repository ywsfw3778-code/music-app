import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_svg_icons.dart';
import '../../core/theme/app_theme.dart';
import '../../providers/music_provider.dart';
import 'album_art_widget.dart';
import 'app_svg_icon.dart';
import 'interactive_scale_tap.dart';
import 'now_playing_sheet.dart';
import 'sound_wave_visualizer.dart';

class FloatingMiniPlayer extends StatefulWidget {
  const FloatingMiniPlayer({super.key});

  @override
  State<FloatingMiniPlayer> createState() => _FloatingMiniPlayerState();
}

class _FloatingMiniPlayerState extends State<FloatingMiniPlayer>
    with SingleTickerProviderStateMixin {
  late AnimationController _rotationController;

  @override
  void initState() {
    super.initState();
    _rotationController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 12),
    );
  }

  @override
  void dispose() {
    _rotationController.dispose();
    super.dispose();
  }

  void _openNowPlaying(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      transitionAnimationController: AnimationController(
        vsync: Navigator.of(context),
        duration: const Duration(milliseconds: 360),
      ),
      builder: (_) => const NowPlayingSheet(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        final song = provider.currentSong;
        if (song == null) return const SizedBox.shrink();

        final accentColor = DynamicPalette.getColor(song);

        if (provider.isPlaying) {
          if (!_rotationController.isAnimating) {
            _rotationController.repeat();
          }
        } else {
          _rotationController.stop();
        }

        final progress = provider.totalDuration.inMilliseconds > 0
            ? (provider.currentPosition.inMilliseconds /
                    provider.totalDuration.inMilliseconds)
                .clamp(0.0, 1.0)
            : 0.0;

        return GestureDetector(
          onTap: () => _openNowPlaying(context),
          onVerticalDragEnd: (details) {
            if (details.primaryVelocity != null &&
                details.primaryVelocity! < -150) {
              _openNowPlaying(context);
            }
          },
          child: Container(
            margin: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.75),
                  blurRadius: 24,
                  offset: const Offset(0, 8),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: BackdropFilter(
                filter: ImageFilter.blur(sigmaX: 25, sigmaY: 25),
                child: Container(
                  padding: const EdgeInsets.fromLTRB(12, 8, 14, 6),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceElevated.withValues(alpha: 0.85),
                    borderRadius: BorderRadius.circular(24),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.12),
                      width: 1.0,
                    ),
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Row(
                        children: [
                          // Rotating circular artwork
                          Hero(
                            tag: 'player_art_${song.id}',
                            child: RotationTransition(
                              turns: _rotationController,
                              child: Container(
                                padding: const EdgeInsets.all(2),
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                    color: accentColor.withValues(alpha: 0.6),
                                    width: 1.5,
                                  ),
                                ),
                                child: ClipOval(
                                  child: AlbumArtWidget(
                                    song: song,
                                    size: 44,
                                    borderRadius: 22,
                                  ),
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),

                          // Song Title & Artist
                          Expanded(
                            child: AnimatedSwitcher(
                              duration: const Duration(milliseconds: 220),
                              transitionBuilder: (child, animation) {
                                return FadeTransition(
                                  opacity: animation,
                                  child: SlideTransition(
                                    position: Tween<Offset>(
                                      begin: const Offset(0.0, 0.20),
                                      end: Offset.zero,
                                    ).animate(
                                      CurvedAnimation(
                                        parent: animation,
                                        curve: Curves.easeOutCubic,
                                      ),
                                    ),
                                    child: child,
                                  ),
                                );
                              },
                              child: Column(
                                key: ValueKey(song.id),
                                crossAxisAlignment: CrossAxisAlignment.start,
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Row(
                                    children: [
                                      Flexible(
                                        child: Text(
                                          song.title,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(
                                            color: AppColors.textPrimary,
                                            fontWeight: FontWeight.w700,
                                            fontSize: 14,
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: 6),
                                      SoundWaveVisualizer(
                                        isPlaying: provider.isPlaying,
                                        barCount: 3,
                                        maxHeight: 12,
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    song.artist,
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      color: AppColors.textSecondary,
                                      fontSize: 12,
                                      fontWeight: FontWeight.w400,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),

                          // Favorite Heart Button (SVG)
                          InteractiveScaleTap(
                            onTap: () => provider.toggleFavorite(song),
                            child: Padding(
                              padding: const EdgeInsets.all(6.0),
                              child: AppSvgIcon(
                                song.isFavorite
                                    ? AppSvgIcons.heartSolid
                                    : AppSvgIcons.heartOutline,
                                color: song.isFavorite
                                    ? AppColors.heart
                                    : AppColors.textSecondary,
                                size: 22,
                              ),
                            ),
                          ),
                          const SizedBox(width: 4),

                          // Play/Pause Button with dynamic glow & SVG
                          InteractiveScaleTap(
                            onTap: provider.togglePlayPause,
                            child: Container(
                              width: 38,
                              height: 38,
                              decoration: BoxDecoration(
                                color: const Color(0xFF282828),
                                shape: BoxShape.circle,
                                border: Border.all(
                                  color: accentColor.withValues(alpha: 0.6),
                                  width: 1.5,
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: accentColor.withValues(alpha: 0.35),
                                    blurRadius: 10,
                                    spreadRadius: 1,
                                  ),
                                ],
                              ),
                              child: Center(
                                child: AppSvgIcon(
                                  provider.isPlaying
                                      ? AppSvgIcons.pause
                                      : AppSvgIcons.play,
                                  color: Colors.white,
                                  size: 20,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 6),

                          // Next Song Button (SVG)
                          InteractiveScaleTap(
                            onTap: provider.playNext,
                            child: const Padding(
                              padding: EdgeInsets.all(6.0),
                              child: AppSvgIcon(
                                AppSvgIcons.skipNext,
                                color: AppColors.textPrimary,
                                size: 22,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),

                      // Sleek AMOLED Progress Bar
                      ClipRRect(
                        borderRadius: BorderRadius.circular(4),
                        child: SizedBox(
                          height: 2.5,
                          child: LinearProgressIndicator(
                            value: progress,
                            backgroundColor: Colors.white.withValues(alpha: 0.12),
                            valueColor: AlwaysStoppedAnimation<Color>(
                              accentColor,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
