import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../providers/music_provider.dart';
import '../widgets/album_art_widget.dart';
import '../widgets/interactive_scale_tap.dart';
import '../widgets/sound_wave_visualizer.dart';

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
    final minutes = d.inMinutes.remainder(60).toString().padLeft(2, '0');
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

        final mostPlayed = provider.mostPlayedSongs;
        final allSongs = provider.allSongs;
        final favorites = provider.favoriteSongs;

        return RefreshIndicator(
          color: AppColors.primary,
          backgroundColor: AppColors.surface,
          onRefresh: provider.refreshSongs,
          child: CustomScrollView(
            physics: const BouncingScrollPhysics(),
            slivers: [
              // Liquid Glass App Bar
              SliverAppBar(
                floating: true,
                backgroundColor: AppColors.background.withValues(alpha: 0.85),
                elevation: 0,
                title: Row(
                  children: [
                    Container(
                      width: 38,
                      height: 38,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: AppColors.primaryGradient,
                        ),
                        borderRadius: BorderRadius.circular(12),
                        boxShadow: [
                          BoxShadow(
                            color: AppColors.primary.withValues(alpha: 0.4),
                            blurRadius: 10,
                            offset: const Offset(0, 3),
                          ),
                        ],
                      ),
                      child: Center(
                        child: Icon(Icons.graphic_eq_rounded, color: AppColors.onPrimary, size: 22),
                      ),
                    ),
                    const SizedBox(width: 12),
                    const Text(
                      'Echo Music',
                      style: TextStyle(
                        fontWeight: FontWeight.w900,
                        fontSize: 22,
                        letterSpacing: -0.5,
                        color: AppColors.textPrimary,
                      ),
                    ),
                  ],
                ),
                actions: [
                  IconButton(
                    icon: const Icon(Icons.refresh_rounded, color: AppColors.textSecondary),
                    onPressed: provider.refreshSongs,
                    tooltip: 'إعادة الفحص',
                  ),
                ],
              ),

              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 18.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const SizedBox(height: 12),

                      // 3-Theme Live Switcher Capsule
                      Container(
                        margin: const EdgeInsets.only(bottom: 16),
                        padding: const EdgeInsets.all(4),
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.06),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: Colors.white.withValues(alpha: 0.12),
                          ),
                        ),
                        child: Row(
                          children: [
                            _buildThemePill(provider, AppThemeStyle.visionOS, '✨ VisionOS'),
                            _buildThemePill(provider, AppThemeStyle.cyberNeon, '⚡ Cyber Neon'),
                            _buildThemePill(provider, AppThemeStyle.titanium, '💎 Titanium'),
                          ],
                        ),
                      ),

                      // Liquid Glass Status Banner
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(22),
                          border: Border.all(
                            color: Colors.white.withValues(alpha: 0.16),
                            width: 1.2,
                          ),
                          gradient: LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: [
                              Colors.white.withValues(alpha: 0.10),
                              AppColors.surface.withValues(alpha: 0.70),
                            ],
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: AppColors.primary.withValues(alpha: 0.08),
                              blurRadius: 16,
                              offset: const Offset(0, 4),
                            ),
                          ],
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                gradient: LinearGradient(
                                  colors: AppColors.primaryGradient,
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: AppColors.primary.withValues(alpha: 0.35),
                                    blurRadius: 8,
                                  ),
                                ],
                              ),
                              child: const Icon(
                                Icons.headphones_rounded,
                                color: Colors.black,
                                size: 22,
                              ),
                            ),
                            const SizedBox(width: 14),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text(
                                    'أغانيك صافية ومفرزة',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 15,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                  const SizedBox(height: 3),
                                  Text(
                                    'تم فحص ${allSongs.length} أغنية • بدون ريكوردات أو واتساب',
                                    style: TextStyle(
                                      fontSize: 12,
                                      color: AppColors.textSecondary.withValues(alpha: 0.9),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),

                      const SizedBox(height: 24),

                      // Section 1: Most Played 🔥 (الأكثر استماعاً)
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
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(
                                    color: AppColors.primary.withValues(alpha: 0.16),
                                    borderRadius: BorderRadius.circular(8),
                                    border: Border.all(
                                      color: AppColors.primary.withValues(alpha: 0.3),
                                    ),
                                  ),
                                  child: Text(
                                    '🔥 TOP',
                                    style: TextStyle(
                                      color: AppColors.primary,
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

                        // Horizontal Most Played Liquid Glass Cards
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
                                          ? AppColors.primary.withValues(alpha: 0.15)
                                          : Colors.white.withValues(alpha: 0.05),
                                      borderRadius: BorderRadius.circular(20),
                                      border: Border.all(
                                        color: isCurrent
                                            ? AppColors.primary
                                            : Colors.white.withValues(alpha: 0.14),
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
                                                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                                                decoration: BoxDecoration(
                                                  color: index == 0
                                                      ? Colors.amber
                                                      : (index == 1 ? Colors.grey.shade300 : AppColors.surfaceElevated),
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
                                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                                decoration: BoxDecoration(
                                                  color: Colors.black.withValues(alpha: 0.8),
                                                  borderRadius: BorderRadius.circular(6),
                                                ),
                                                child: Row(
                                                  mainAxisSize: MainAxisSize.min,
                                                  children: [
                                                    Icon(Icons.play_arrow_rounded, color: AppColors.primary, size: 12),
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
                        const SizedBox(height: 24),
                      ],

                      // Section 2: Quick Play Grid (Liquid Glass Tiles)
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
                                    ? AppColors.primary.withValues(alpha: 0.16)
                                    : Colors.white.withValues(alpha: 0.05),
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(
                                  color: isCurrent
                                      ? AppColors.primary
                                      : Colors.white.withValues(alpha: 0.12),
                                  width: isCurrent ? 1.5 : 1,
                                ),
                              ),
                              child: Row(
                                children: [
                                  AlbumArtWidget(
                                    song: song,
                                    size: 52,
                                    borderRadius: 12,
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
                                    const Padding(
                                      padding: EdgeInsets.symmetric(horizontal: 8.0),
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

                      // Section 3: Favorites (Liquid Glass list)
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
                                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                  decoration: BoxDecoration(
                                    color: isCurrent
                                        ? AppColors.primary.withValues(alpha: 0.14)
                                        : Colors.white.withValues(alpha: 0.05),
                                    borderRadius: BorderRadius.circular(16),
                                    border: Border.all(
                                      color: isCurrent
                                          ? AppColors.primary
                                          : Colors.white.withValues(alpha: 0.12),
                                      width: isCurrent ? 1.5 : 1,
                                    ),
                                  ),
                                  child: Row(
                                    children: [
                                      AlbumArtWidget(
                                        song: song,
                                        size: 46,
                                        borderRadius: 10,
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
                                              style: const TextStyle(color: AppColors.textSecondary, fontSize: 12),
                                            ),
                                          ],
                                        ),
                                      ),
                                      Text(
                                        _formatDuration(song.duration),
                                        style: const TextStyle(color: AppColors.textMuted, fontSize: 12),
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

  Widget _buildThemePill(MusicProvider provider, AppThemeStyle style, String label) {
    final isSelected = provider.themeStyle == style;

    return Expanded(
      child: GestureDetector(
        onTap: () => provider.setThemeStyle(style),
        behavior: HitTestBehavior.opaque,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 240),
          curve: Curves.easeOutCubic,
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            gradient: isSelected
                ? LinearGradient(colors: AppColors.primaryGradient)
                : null,
            color: isSelected ? null : Colors.transparent,
            borderRadius: BorderRadius.circular(12),
            boxShadow: isSelected
                ? [
                    BoxShadow(
                      color: AppColors.primary.withValues(alpha: 0.35),
                      blurRadius: 10,
                      offset: const Offset(0, 2),
                    ),
                  ]
                : null,
          ),
          child: Center(
            child: Text(
              label,
              style: TextStyle(
                color: isSelected ? AppColors.onPrimary : Colors.white.withValues(alpha: 0.7),
                fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                fontSize: 12,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
