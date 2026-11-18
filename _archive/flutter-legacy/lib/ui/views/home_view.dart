import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../core/constants/app_svg_icons.dart';
import '../../core/theme/app_theme.dart';
import '../../providers/music_provider.dart';
import '../widgets/album_art_widget.dart';
import '../widgets/app_svg_icon.dart';
import '../widgets/interactive_scale_tap.dart';
import '../widgets/sound_wave_visualizer.dart';
import 'youtube_search_view.dart';

class HomeView extends StatefulWidget {
  const HomeView({super.key});

  @override
  State<HomeView> createState() => _HomeViewState();
}

class _HomeViewState extends State<HomeView> with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  String _formatDuration(int ms) {
    final d = Duration(milliseconds: ms);
    final minutes = d.inMinutes.remainder(60).toString();
    final seconds = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);

    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        if (provider.isLoading) {
          return Center(
            child: CircularProgressIndicator(color: AppColors.primary),
          );
        }

        final currentSong = provider.currentSong;
        final accentColor = DynamicPalette.getColor(currentSong);
        final mostPlayed = provider.mostPlayedSongs;
        final allSongs = provider.allSongs;
        final favorites = provider.favoriteSongs;

        return RefreshIndicator(
          color: accentColor,
          backgroundColor: AppColors.surface,
          onRefresh: provider.refreshSongs,
          child: CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              // Minimalist AMOLED App Bar
              SliverAppBar(
                floating: true,
                backgroundColor: AppColors.amoledBlack.withValues(alpha: 0.92),
                elevation: 0,
                title: Row(
                  children: [
                    Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        color: accentColor.withValues(alpha: 0.18),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: accentColor.withValues(alpha: 0.35),
                          width: 1,
                        ),
                      ),
                      child: Center(
                        child: AppSvgIcon(
                          AppSvgIcons.soundLogo,
                          color: accentColor,
                          size: 20,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    const Text(
                      'Echo Music',
                      style: TextStyle(
                        fontWeight: FontWeight.w900,
                        fontSize: 21,
                        letterSpacing: -0.5,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ],
                ),
                actions: [
                  IconButton(
                    icon: AppSvgIcon(
                      AppSvgIcons.search,
                      color: accentColor,
                      size: 22,
                    ),
                    onPressed: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(builder: (_) => const YouTubeSearchView()),
                      );
                    },
                    tooltip: 'بحث وتحميل YouTube',
                  ),
                  IconButton(
                    icon: const AppSvgIcon(
                      AppSvgIcons.refresh,
                      color: AppColors.textSecondary,
                      size: 20,
                    ),
                    onPressed: provider.refreshSongs,
                    tooltip: 'إعادة الفحص',
                  ),
                ],
              ),

              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 8),

                      // Featured Music / Quick Shuffle Card
                      InteractiveScaleTap(
                        onTap: () {
                          if (allSongs.isNotEmpty) {
                            if (!provider.isShuffle) provider.toggleShuffle();
                            provider.playSong(allSongs.first, queue: allSongs);
                          }
                        },
                        child: Container(
                          padding: const EdgeInsets.all(20),
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(24),
                            color: AppColors.surface,
                            border: Border.all(
                              color: Colors.white.withValues(alpha: 0.08),
                              width: 1,
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.6),
                                blurRadius: 20,
                                offset: const Offset(0, 6),
                              ),
                            ],
                          ),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 10,
                                        vertical: 4,
                                      ),
                                      decoration: BoxDecoration(
                                        color: accentColor.withValues(alpha: 0.14),
                                        borderRadius: BorderRadius.circular(20),
                                        border: Border.all(
                                          color: accentColor.withValues(alpha: 0.3),
                                        ),
                                      ),
                                      child: Text(
                                        '🎧 Hi-Res Audio • تجربة طبيعية',
                                        style: TextStyle(
                                          color: accentColor,
                                          fontSize: 11,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: 12),
                                    const Text(
                                      'تشغيل عشوائي لكل أغانيك',
                                      style: TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.w800,
                                        color: AppColors.textPrimary,
                                        letterSpacing: -0.3,
                                      ),
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      '${allSongs.length} أغنية جاهزة للاستماع',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: AppColors.textSecondary.withValues(alpha: 0.9),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Container(
                                width: 52,
                                height: 52,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: accentColor,
                                  boxShadow: [
                                    BoxShadow(
                                      color: accentColor.withValues(alpha: 0.4),
                                      blurRadius: 14,
                                      offset: const Offset(0, 4),
                                    ),
                                  ],
                                ),
                                child: const Center(
                                  child: AppSvgIcon(
                                    AppSvgIcons.play,
                                    color: Colors.black,
                                    size: 24,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),

                      const SizedBox(height: 28),

                      // Section 1: Most Played (الأكثر استماعاً)
                      if (mostPlayed.isNotEmpty) ...[
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Row(
                              children: [
                                const Text(
                                  'الأكثر استماعاً',
                                  style: TextStyle(
                                    fontSize: 18,
                                    fontWeight: FontWeight.w800,
                                    color: AppColors.textPrimary,
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 3,
                                  ),
                                  decoration: BoxDecoration(
                                    color: accentColor.withValues(alpha: 0.16),
                                    borderRadius: BorderRadius.circular(8),
                                    border: Border.all(
                                      color: accentColor.withValues(alpha: 0.3),
                                    ),
                                  ),
                                  child: Text(
                                    '🔥 TOP',
                                    style: TextStyle(
                                      color: accentColor,
                                      fontSize: 10,
                                      fontWeight: FontWeight.w900,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            Text(
                              '${mostPlayed.length} أغنية',
                              style: const TextStyle(
                                color: AppColors.textMuted,
                                fontSize: 12,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 14),

                        // Horizontal Most Played Cards
                        SizedBox(
                          height: 200,
                          child: ListView.builder(
                            scrollDirection: Axis.horizontal,
                            physics: const BouncingScrollPhysics(),
                            itemCount: mostPlayed.length,
                            itemBuilder: (context, index) {
                              final song = mostPlayed[index];
                              final isCurrent = provider.currentSong?.id == song.id;

                              return Container(
                                width: 142,
                                margin: const EdgeInsets.only(left: 12),
                                child: InteractiveScaleTap(
                                  onTap: () => provider.playSong(song, queue: mostPlayed),
                                  child: Container(
                                    padding: const EdgeInsets.all(10),
                                    decoration: BoxDecoration(
                                      color: isCurrent
                                          ? accentColor.withValues(alpha: 0.14)
                                          : AppColors.surface,
                                      borderRadius: BorderRadius.circular(20),
                                      border: Border.all(
                                        color: isCurrent
                                            ? accentColor
                                            : Colors.white.withValues(alpha: 0.08),
                                        width: isCurrent ? 1.5 : 1,
                                      ),
                                    ),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Stack(
                                          children: [
                                            AlbumArtWidget(
                                              song: song,
                                              size: 122,
                                              borderRadius: 14,
                                            ),
                                            // Rank Badge
                                            Positioned(
                                              top: 6,
                                              right: 6,
                                              child: Container(
                                                padding: const EdgeInsets.symmetric(
                                                  horizontal: 7,
                                                  vertical: 3,
                                                ),
                                                decoration: BoxDecoration(
                                                  color: index == 0
                                                      ? Colors.amber
                                                      : (index == 1
                                                          ? Colors.grey.shade300
                                                          : AppColors.surfaceElevated),
                                                  borderRadius: BorderRadius.circular(8),
                                                ),
                                                child: Text(
                                                  '#${index + 1}',
                                                  style: TextStyle(
                                                    color: index < 2 ? Colors.black : Colors.white,
                                                    fontSize: 10,
                                                    fontWeight: FontWeight.w900,
                                                  ),
                                                ),
                                              ),
                                            ),
                                            // Play count indicator
                                            Positioned(
                                              bottom: 6,
                                              left: 6,
                                              child: Container(
                                                padding: const EdgeInsets.symmetric(
                                                  horizontal: 6,
                                                  vertical: 2,
                                                ),
                                                decoration: BoxDecoration(
                                                  color: Colors.black.withValues(alpha: 0.8),
                                                  borderRadius: BorderRadius.circular(6),
                                                ),
                                                child: Row(
                                                  mainAxisSize: MainAxisSize.min,
                                                  children: [
                                                    AppSvgIcon(
                                                      AppSvgIcons.play,
                                                      color: accentColor,
                                                      size: 10,
                                                    ),
                                                    const SizedBox(width: 3),
                                                    Text(
                                                      '${song.playCount}',
                                                      style: const TextStyle(
                                                        color: Colors.white,
                                                        fontSize: 10,
                                                        fontWeight: FontWeight.bold,
                                                      ),
                                                    ),
                                                  ],
                                                ),
                                              ),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 8),
                                        Text(
                                          song.title,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(
                                            color: AppColors.textPrimary,
                                            fontWeight: FontWeight.w700,
                                            fontSize: 13,
                                          ),
                                        ),
                                        const SizedBox(height: 2),
                                        Text(
                                          song.artist,
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                          style: const TextStyle(
                                            color: AppColors.textSecondary,
                                            fontSize: 11,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                        const SizedBox(height: 28),
                      ],

                      // Section 2: Quick Play Grid
                      const Text(
                        'تشغيل سريع',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          color: AppColors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 12),
                      GridView.builder(
                        physics: const NeverScrollableScrollPhysics(),
                        shrinkWrap: true,
                        itemCount: allSongs.take(6).length,
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          childAspectRatio: 2.7,
                          crossAxisSpacing: 10,
                          mainAxisSpacing: 10,
                        ),
                        itemBuilder: (context, index) {
                          final song = allSongs[index];
                          final isCurrent = provider.currentSong?.id == song.id;

                          return InteractiveScaleTap(
                            onTap: () => provider.playSong(song),
                            child: Container(
                              decoration: BoxDecoration(
                                color: isCurrent
                                    ? accentColor.withValues(alpha: 0.14)
                                    : AppColors.surface,
                                borderRadius: BorderRadius.circular(16),
                                border: Border.all(
                                  color: isCurrent
                                      ? accentColor
                                      : Colors.white.withValues(alpha: 0.08),
                                  width: isCurrent ? 1.5 : 1,
                                ),
                              ),
                              child: Row(
                                children: [
                                  AlbumArtWidget(
                                    song: song,
                                    size: 52,
                                    borderRadius: 14,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      song.title,
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                        color: AppColors.textPrimary,
                                        fontSize: 12,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                  ),
                                  if (isCurrent && provider.isPlaying)
                                    Padding(
                                      padding: const EdgeInsets.symmetric(horizontal: 8.0),
                                      child: SoundWaveVisualizer(
                                        isPlaying: true,
                                        barCount: 3,
                                        maxHeight: 12,
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          );
                        },
                      ),

                      const SizedBox(height: 28),

                      // Section 3: Favorites
                      if (favorites.isNotEmpty) ...[
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text(
                              'أغانيك المفضلة ❤️',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: AppColors.textPrimary,
                              ),
                            ),
                            Text(
                              '${favorites.length}',
                              style: const TextStyle(color: AppColors.textMuted),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        ListView.builder(
                          physics: const NeverScrollableScrollPhysics(),
                          shrinkWrap: true,
                          itemCount: favorites.take(5).length,
                          itemBuilder: (context, index) {
                            final song = favorites[index];
                            final isCurrent = provider.currentSong?.id == song.id;

                            return Padding(
                              padding: const EdgeInsets.only(bottom: 8.0),
                              child: InteractiveScaleTap(
                                onTap: () => provider.playSong(song, queue: favorites),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12,
                                    vertical: 8,
                                  ),
                                  decoration: BoxDecoration(
                                    color: isCurrent
                                        ? accentColor.withValues(alpha: 0.12)
                                        : AppColors.surface,
                                    borderRadius: BorderRadius.circular(16),
                                    border: Border.all(
                                      color: isCurrent
                                          ? accentColor
                                          : Colors.white.withValues(alpha: 0.08),
                                      width: isCurrent ? 1.5 : 1,
                                    ),
                                  ),
                                  child: Row(
                                    children: [
                                      AlbumArtWidget(
                                        song: song,
                                        size: 46,
                                        borderRadius: 12,
                                      ),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              song.title,
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                              style: const TextStyle(
                                                color: AppColors.textPrimary,
                                                fontWeight: FontWeight.w600,
                                                fontSize: 14,
                                              ),
                                            ),
                                            Text(
                                              song.artist,
                                              maxLines: 1,
                                              style: const TextStyle(
                                                color: AppColors.textSecondary,
                                                fontSize: 12,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                      Text(
                                        _formatDuration(song.duration),
                                        style: const TextStyle(
                                          color: AppColors.textMuted,
                                          fontSize: 12,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            );
                          },
                        ),
                      ],

                      // Bottom spacing for Mini Player & Dock
                      const SizedBox(height: 130),
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}
