import 'dart:ui';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:just_audio/just_audio.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_svg_icons.dart';
import '../../core/theme/app_theme.dart';
import '../../models/song_model.dart';
import '../../providers/music_provider.dart';
import 'album_art_widget.dart';
import 'app_svg_icon.dart';
import 'interactive_scale_tap.dart';
import 'synced_lyrics_widget.dart';

class NowPlayingSheet extends StatelessWidget {
  const NowPlayingSheet({super.key});

  String _formatDuration(Duration d) {
    final minutes = d.inMinutes.remainder(60).toString();
    final seconds = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }

  void _showAddToPlaylistDialog(BuildContext context, SongModel song, Color accentColor) {
    final provider = Provider.of<MusicProvider>(context, listen: false);
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'إضافة إلى قائمة تشغيل',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 16),
              if (provider.playlists.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 24),
                  child: Center(
                    child: Text(
                      'لا توجد قوائم تشغيل حالياً.\nيمكنك إنشاء قائمة من تبويب القوائم.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: AppColors.textSecondary),
                    ),
                  ),
                )
              else
                Flexible(
                  child: ListView.builder(
                    shrinkWrap: true,
                    itemCount: provider.playlists.length,
                    itemBuilder: (context, index) {
                      final pl = provider.playlists[index];
                      final isAdded = pl.songIds.contains(song.id);
                      return ListTile(
                        leading: Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: AppColors.surfaceLight,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Center(
                            child: AppSvgIcon(
                              AppSvgIcons.playlist,
                              color: accentColor,
                              size: 20,
                            ),
                          ),
                        ),
                        title: Text(
                          pl.name,
                          style: const TextStyle(
                            color: AppColors.textPrimary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        subtitle: Text(
                          '${pl.songIds.length} أغنية',
                          style: const TextStyle(color: AppColors.textMuted),
                        ),
                        trailing: AppSvgIcon(
                          isAdded
                              ? AppSvgIcons.checkCircle
                              : AppSvgIcons.addCircle,
                          color: isAdded ? accentColor : AppColors.textSecondary,
                          size: 22,
                        ),
                        onTap: () {
                          provider.toggleSongInPlaylist(pl.id, song.id);
                          Navigator.pop(ctx);
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              content: Text(
                                isAdded
                                    ? 'تمت الإزالة من ${pl.name}'
                                    : 'تمت الإضافة إلى ${pl.name}',
                              ),
                              backgroundColor: AppColors.surfaceElevated,
                              duration: const Duration(seconds: 2),
                            ),
                          );
                        },
                      );
                    },
                  ),
                ),
            ],
          ),
        );
      },
    );
  }

  void _showLyricsSheet(BuildContext context, SongModel song, Color accentColor) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.amoledBlack,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (ctx) {
        return Container(
          height: MediaQuery.of(context).size.height * 0.80,
          decoration: const BoxDecoration(
            color: AppColors.amoledBlack,
            borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
          ),
          child: Column(
            children: [
              const SizedBox(height: 12),
              Container(
                width: 44,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              const SizedBox(height: 16),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 24.0),
                child: Column(
                  children: [
                    Text(
                      song.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      song.artist,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: AppColors.textSecondary,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              ),
              const Divider(color: Colors.white12, height: 24),
              Expanded(
                child: SyncedLyricsWidget(
                  song: song,
                  activeColor: accentColor,
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        final song = provider.currentSong;
        if (song == null) return const SizedBox.shrink();

        final accentColor = DynamicPalette.getColor(song);

        final maxDurationMs = provider.totalDuration.inMilliseconds > 0
            ? provider.totalDuration.inMilliseconds.toDouble()
            : (song.duration > 0 ? song.duration.toDouble() : 1.0);

        final currentPositionMs = provider.currentPosition.inMilliseconds
            .toDouble()
            .clamp(0.0, maxDurationMs);

        final remainingDuration = provider.totalDuration > provider.currentPosition
            ? provider.totalDuration - provider.currentPosition
            : Duration.zero;

        return ClipRRect(
          borderRadius: const BorderRadius.vertical(top: Radius.circular(36)),
          child: Container(
            height: MediaQuery.of(context).size.height * 0.94,
            color: AppColors.amoledBlack,
            child: Stack(
              fit: StackFit.expand,
              children: [
                // 1. Soft Blurred Album Art Dynamic Full-Screen Backdrop with Crossfade
                Positioned.fill(
                  child: AnimatedSwitcher(
                    duration: const Duration(milliseconds: 400),
                    switchInCurve: Curves.easeOut,
                    switchOutCurve: Curves.easeIn,
                    child: KeyedSubtree(
                      key: ValueKey('backdrop_${song.id}'),
                      child: Stack(
                        fit: StackFit.expand,
                        children: [
                          const ColoredBox(color: AppColors.amoledBlack),
                          // Heavy gaussian blur over artwork
                          ImageFiltered(
                            imageFilter: ImageFilter.blur(
                              sigmaX: 45,
                              sigmaY: 45,
                              tileMode: TileMode.decal,
                            ),
                            child: Transform.scale(
                              scale: 1.15,
                              child: AlbumArtWidget(
                                song: song,
                                size: double.infinity,
                                borderRadius: 0,
                              ),
                            ),
                          ),
                          // Darkened overlay transitioning to pure AMOLED black at bottom
                          Container(
                            decoration: BoxDecoration(
                              gradient: LinearGradient(
                                begin: Alignment.topCenter,
                                end: Alignment.bottomCenter,
                                colors: [
                                  Colors.black.withValues(alpha: 0.55),
                                  Colors.black.withValues(alpha: 0.72),
                                  AppColors.amoledBlack.withValues(alpha: 0.94),
                                  AppColors.amoledBlack,
                                ],
                                stops: const [0.0, 0.40, 0.78, 1.0],
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

                // 2. Foreground Authentic Apple/Spotify Player UI
                SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 28.0),
                    child: Column(
                      children: [
                        const SizedBox(height: 8),

                        // Top Bar: Down Chevron / Dismiss Handle
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            InteractiveScaleTap(
                              onTap: () => Navigator.pop(context),
                              child: Container(
                                padding: const EdgeInsets.all(8),
                                decoration: const BoxDecoration(
                                  color: Colors.transparent,
                                  shape: BoxShape.circle,
                                ),
                                child: const AppSvgIcon(
                                  AppSvgIcons.chevronDown,
                                  color: Colors.white70,
                                  size: 26,
                                ),
                              ),
                            ),
                            // Optional minimal title / subtitle
                            Column(
                              children: [
                                const Text(
                                  'PLAYING FROM YOUR LIBRARY',
                                  style: TextStyle(
                                    color: Colors.white54,
                                    fontSize: 10,
                                    fontWeight: FontWeight.w700,
                                    letterSpacing: 1.1,
                                  ),
                                ),
                                if (song.playCount > 0)
                                  Text(
                                    '${song.playCount} plays',
                                    style: TextStyle(
                                      color: accentColor,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                              ],
                            ),
                            InteractiveScaleTap(
                              onTap: () => _showAddToPlaylistDialog(context, song, accentColor),
                              child: Container(
                                padding: const EdgeInsets.all(8),
                                decoration: const BoxDecoration(
                                  color: Colors.transparent,
                                  shape: BoxShape.circle,
                                ),
                                child: const AppSvgIcon(
                                  AppSvgIcons.playlist,
                                  color: Colors.white70,
                                  size: 22,
                                ),
                              ),
                            ),
                          ],
                        ),

                        const Spacer(flex: 1),

                        // Large Centered Album Artwork matching Mockup (responsive to viewport height)
                        Expanded(
                          flex: 10,
                          child: Center(
                            child: LayoutBuilder(
                              builder: (context, constraints) {
                                final artSize = constraints.maxHeight.clamp(140.0, 300.0);
                                return Container(
                                  width: artSize,
                                  height: artSize,
                                  decoration: BoxDecoration(
                                    borderRadius: BorderRadius.circular(24),
                                    boxShadow: [
                                      BoxShadow(
                                        color: Colors.black.withValues(alpha: 0.60),
                                        blurRadius: 36,
                                        offset: const Offset(0, 16),
                                        spreadRadius: 2,
                                      ),
                                    ],
                                  ),
                                  child: Hero(
                                    tag: 'player_art_${song.id}',
                                    child: AlbumArtWidget(
                                      song: song,
                                      size: artSize,
                                      borderRadius: 24,
                                    ),
                                  ),
                                );
                              },
                            ),
                          ),
                        ),

                        const Spacer(flex: 1),

                        // Track Title, Artist, & Favorite Heart Button Row
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            Expanded(
                              child: AnimatedSwitcher(
                                duration: const Duration(milliseconds: 250),
                                transitionBuilder: (child, animation) {
                                  return FadeTransition(
                                    opacity: animation,
                                    child: child,
                                  );
                                },
                                child: Column(
                                  key: ValueKey(song.id),
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    Text(
                                      song.title,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: AppColors.textPrimary,
                                        fontSize: 22,
                                        fontWeight: FontWeight.bold,
                                        letterSpacing: -0.4,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      song.artist,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: AppColors.textSecondary,
                                        fontSize: 16,
                                        fontWeight: FontWeight.w400,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            InteractiveScaleTap(
                              onTap: () => provider.toggleFavorite(song),
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: AppSvgIcon(
                                  song.isFavorite
                                      ? AppSvgIcons.heartSolid
                                      : AppSvgIcons.heartOutline,
                                  color: song.isFavorite
                                      ? AppColors.heart
                                      : AppColors.textSecondary,
                                  size: 26,
                                ),
                              ),
                            ),
                          ],
                        ),

                        const SizedBox(height: 18),

                        // Sleek Minimalist Progress Seekbar
                        SliderTheme(
                          data: SliderTheme.of(context).copyWith(
                            trackHeight: 3.5,
                            activeTrackColor: accentColor,
                            inactiveTrackColor: Colors.white.withValues(alpha: 0.18),
                            thumbColor: Colors.white,
                            thumbShape: const RoundSliderThumbShape(
                              enabledThumbRadius: 6,
                            ),
                            overlayColor: accentColor.withValues(alpha: 0.18),
                            overlayShape: const RoundSliderOverlayShape(
                              overlayRadius: 14,
                            ),
                          ),
                          child: Slider(
                            value: currentPositionMs,
                            min: 0.0,
                            max: maxDurationMs,
                            onChanged: (val) {
                              provider.seek(Duration(milliseconds: val.toInt()));
                            },
                          ),
                        ),

                        // Elapsed Time (Left) & Negative Remaining Time (Right) matching Mockup
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 14.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                _formatDuration(provider.currentPosition),
                                style: const TextStyle(
                                  color: AppColors.textMuted,
                                  fontSize: 12,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                              Text(
                                '-${_formatDuration(remainingDuration)}',
                                style: const TextStyle(
                                  color: AppColors.textMuted,
                                  fontSize: 12,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),

                        const SizedBox(height: 20),

                        // Primary Controls Row matching Mockup
                        // [Shuffle, Previous, Play/Pause (with dynamic glow), Next, Repeat]
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            // 1. Shuffle
                            InteractiveScaleTap(
                              onTap: provider.toggleShuffle,
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: AppSvgIcon(
                                  AppSvgIcons.shuffle,
                                  color: provider.isShuffle
                                      ? accentColor
                                      : AppColors.textMuted,
                                  size: 22,
                                ),
                              ),
                            ),

                            // 2. Previous Track
                            InteractiveScaleTap(
                              onTap: provider.playPrevious,
                              child: const Padding(
                                padding: EdgeInsets.all(8.0),
                                child: AppSvgIcon(
                                  AppSvgIcons.skipPrevious,
                                  color: AppColors.textPrimary,
                                  size: 28,
                                ),
                              ),
                            ),

                            // 3. Giant Play/Pause with Natural Radial Glow Halo matching Mockup
                            InteractiveScaleTap(
                              onTap: provider.togglePlayPause,
                              child: Container(
                                width: 72,
                                height: 72,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: const Color(0xFF282828),
                                  border: Border.all(
                                    color: accentColor.withValues(alpha: 0.70),
                                    width: 2.0,
                                  ),
                                  boxShadow: [
                                    BoxShadow(
                                      color: accentColor.withValues(alpha: 0.45),
                                      blurRadius: 28,
                                      spreadRadius: 2,
                                    ),
                                  ],
                                ),
                                child: Center(
                                  child: AppSvgIcon(
                                    provider.isPlaying
                                        ? AppSvgIcons.pause
                                        : AppSvgIcons.play,
                                    color: Colors.white,
                                    size: 32,
                                  ),
                                ),
                              ),
                            ),

                            // 4. Next Track
                            InteractiveScaleTap(
                              onTap: provider.playNext,
                              child: const Padding(
                                padding: EdgeInsets.all(8.0),
                                child: AppSvgIcon(
                                  AppSvgIcons.skipNext,
                                  color: AppColors.textPrimary,
                                  size: 28,
                                ),
                              ),
                            ),

                            // 5. Repeat Mode
                            InteractiveScaleTap(
                              onTap: provider.cycleRepeatMode,
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: AppSvgIcon(
                                  provider.loopMode == LoopMode.one
                                      ? AppSvgIcons.repeatOne
                                      : AppSvgIcons.repeatAll,
                                  color: provider.loopMode != LoopMode.off
                                      ? accentColor
                                      : AppColors.textMuted,
                                  size: 22,
                                ),
                              ),
                            ),
                          ],
                        ),

                        const Spacer(flex: 2),

                        // Frosted Glass "LYRICS" Pill Button at Bottom matching Mockup
                        Center(
                          child: InteractiveScaleTap(
                            onTap: () => _showLyricsSheet(context, song, accentColor),
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(20),
                              child: BackdropFilter(
                                filter: ImageFilter.blur(sigmaX: 16, sigmaY: 16),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 24,
                                    vertical: 9,
                                  ),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withValues(alpha: 0.12),
                                    borderRadius: BorderRadius.circular(20),
                                    border: Border.all(
                                      color: Colors.white.withValues(alpha: 0.18),
                                      width: 1,
                                    ),
                                  ),
                                  child: const Text(
                                    'LYRICS',
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 12,
                                      fontWeight: FontWeight.w700,
                                      letterSpacing: 1.5,
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),

                        const SizedBox(height: 16),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
