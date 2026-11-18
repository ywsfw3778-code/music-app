import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../core/constants/app_colors.dart';
import '../../models/song_model.dart';
import '../../providers/music_provider.dart';
import '../widgets/album_art_widget.dart';
import '../widgets/interactive_scale_tap.dart';
import '../widgets/sound_wave_visualizer.dart';

enum SongSortOption { dateAdded, title, artist, duration, playCount }

class AllSongsView extends StatefulWidget {
  const AllSongsView({super.key});

  @override
  State<AllSongsView> createState() => _AllSongsViewState();
}

class _AllSongsViewState extends State<AllSongsView>
    with AutomaticKeepAliveClientMixin {
  @override
  bool get wantKeepAlive => true;

  final TextEditingController _searchController = TextEditingController();
  SongSortOption _sortOption = SongSortOption.dateAdded;
  String _searchQuery = '';

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  String _formatDuration(int ms) {
    final d = Duration(milliseconds: ms);
    final minutes = d.inMinutes.remainder(60).toString().padLeft(2, '0');
    final seconds = d.inSeconds.remainder(60).toString().padLeft(2, '0');
    return '$minutes:$seconds';
  }

  void _showSortDialog() {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(26)),
      ),
      builder: (ctx) {
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 20.0),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 44,
                height: 4,
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: AppColors.textMuted.withValues(alpha: 0.4),
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              const Text(
                'ترتيب الأغاني حسب',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              _buildSortItem(ctx, 'المحملة مؤخراً (أحدث إضافة) ⚡', SongSortOption.dateAdded),
              _buildSortItem(ctx, 'الأكثر استماعاً 🔥', SongSortOption.playCount),
              _buildSortItem(ctx, 'اسم الأغنية (أ - ي)', SongSortOption.title),
              _buildSortItem(ctx, 'اسم الفنان', SongSortOption.artist),
              _buildSortItem(ctx, 'مدة المقطع', SongSortOption.duration),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSortItem(BuildContext ctx, String label, SongSortOption option) {
    final isSelected = _sortOption == option;
    return ListTile(
      leading: Icon(
        isSelected ? Icons.radio_button_checked_rounded : Icons.radio_button_off_rounded,
        color: isSelected ? AppColors.primary : AppColors.textMuted,
      ),
      title: Text(
        label,
        style: TextStyle(
          color: isSelected ? AppColors.primary : AppColors.textPrimary,
          fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        ),
      ),
      onTap: () {
        setState(() => _sortOption = option);
        Navigator.pop(ctx);
      },
    );
  }

  void _showSongOptions(BuildContext context, SongModel song) {
    final provider = Provider.of<MusicProvider>(context, listen: false);
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (ctx) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Row(
                  children: [
                    AlbumArtWidget(song: song, size: 50, borderRadius: 10),
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
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                            ),
                          ),
                          Text(
                            song.artist,
                            style: const TextStyle(color: AppColors.textSecondary, fontSize: 13),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                Divider(color: AppColors.border, height: 28),
                ListTile(
                  leading: Icon(
                    song.isFavorite ? Icons.favorite_rounded : Icons.favorite_border_rounded,
                    color: song.isFavorite ? AppColors.heart : AppColors.textPrimary,
                  ),
                  title: Text(
                    song.isFavorite ? 'إزالة من المفضلة' : 'إضافة إلى المفضلة',
                    style: const TextStyle(color: AppColors.textPrimary),
                  ),
                  onTap: () {
                    provider.toggleFavorite(song);
                    Navigator.pop(ctx);
                  },
                ),
                ListTile(
                  leading: Icon(Icons.playlist_add_rounded, color: AppColors.primary),
                  title: const Text('إضافة إلى قائمة تشغيل', style: TextStyle(color: AppColors.textPrimary)),
                  onTap: () {
                    Navigator.pop(ctx);
                    _showPlaylistSelector(context, song);
                  },
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  void _showPlaylistSelector(BuildContext context, SongModel song) {
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
                'اختر قائمة التشغيل',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary,
                ),
              ),
              const SizedBox(height: 12),
              if (provider.playlists.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 20),
                  child: Center(
                    child: Text(
                      'لا توجد قوائم تشغيل. أنشئ قائمة أولاً.',
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
                        leading: Icon(Icons.queue_music_rounded, color: AppColors.primary),
                        title: Text(pl.name, style: const TextStyle(color: AppColors.textPrimary)),
                        trailing: Icon(
                          isAdded ? Icons.check_circle_rounded : Icons.add_circle_outline_rounded,
                          color: isAdded ? AppColors.primary : AppColors.textSecondary,
                        ),
                        onTap: () {
                          provider.toggleSongInPlaylist(pl.id, song.id);
                          Navigator.pop(ctx);
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

  @override
  Widget build(BuildContext context) {
    super.build(context);

    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        var songs = List<SongModel>.from(provider.allSongs);

        // Filter by Search
        if (_searchQuery.trim().isNotEmpty) {
          final q = _searchQuery.toLowerCase();
          songs = songs.where((s) {
            return s.title.toLowerCase().contains(q) ||
                s.artist.toLowerCase().contains(q) ||
                s.album.toLowerCase().contains(q);
          }).toList();
        }

        // Apply Sorting
        switch (_sortOption) {
          case SongSortOption.dateAdded:
            songs.sort((a, b) => b.dateAdded.compareTo(a.dateAdded));
            break;
          case SongSortOption.title:
            songs.sort((a, b) => a.title.compareTo(b.title));
            break;
          case SongSortOption.artist:
            songs.sort((a, b) => a.artist.compareTo(b.artist));
            break;
          case SongSortOption.duration:
            songs.sort((a, b) => b.duration.compareTo(a.duration));
            break;
          case SongSortOption.playCount:
            songs.sort((a, b) => b.playCount.compareTo(a.playCount));
            break;
        }

        return Scaffold(
          backgroundColor: AppColors.background,
          appBar: AppBar(
            backgroundColor: AppColors.background.withValues(alpha: 0.85),
            elevation: 0,
            title: const Text(
              'جميع الأغاني',
              style: TextStyle(
                fontWeight: FontWeight.w800,
                fontSize: 22,
                color: AppColors.textPrimary,
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.sort_rounded, color: AppColors.textPrimary),
                onPressed: _showSortDialog,
                tooltip: 'ترتيب',
              ),
            ],
          ),
          body: Column(
            children: [
              // Liquid Glass Search Bar
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
                child: Container(
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.06),
                    borderRadius: BorderRadius.circular(18),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.15),
                      width: 1.2,
                    ),
                  ),
                  child: TextField(
                    controller: _searchController,
                    onChanged: (val) => setState(() => _searchQuery = val),
                    style: const TextStyle(color: AppColors.textPrimary),
                    decoration: InputDecoration(
                      hintText: 'ابحث عن اسم أغنية أو فنان...',
                      hintStyle: const TextStyle(color: AppColors.textMuted),
                      prefixIcon: const Icon(Icons.search_rounded, color: AppColors.textSecondary),
                      suffixIcon: _searchQuery.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear_rounded, color: AppColors.textSecondary),
                              onPressed: () {
                                _searchController.clear();
                                setState(() => _searchQuery = '');
                              },
                            )
                          : null,
                      border: InputBorder.none,
                      contentPadding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                  ),
                ),
              ),

              // Filter Header Info
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 18.0, vertical: 6),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      '${songs.length} أغنية • ${_sortOption == SongSortOption.dateAdded ? 'الأحدث أولاً ⚡' : 'مفرزة'}',
                      style: const TextStyle(
                        color: AppColors.textSecondary,
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                      ),
                    ),
                    Row(
                      children: [
                        Icon(Icons.check_circle_outline_rounded, color: AppColors.primary, size: 14),
                        const SizedBox(width: 4),
                        Text(
                          'أغاني وموسيقى فقط',
                          style: TextStyle(
                            color: AppColors.primary,
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              // Songs List with Liquid Glass Styling
              Expanded(
                child: songs.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(Icons.music_off_rounded, size: 64, color: AppColors.textMuted.withValues(alpha: 0.5)),
                            const SizedBox(height: 12),
                            const Text(
                              'لم يتم العثور على أي نتائج',
                              style: TextStyle(color: AppColors.textSecondary, fontSize: 16),
                            ),
                          ],
                        ),
                      )
                    : ListView.builder(
                        physics: const BouncingScrollPhysics(),
                        padding: const EdgeInsets.only(bottom: 130),
                        itemCount: songs.length,
                        itemBuilder: (context, index) {
                          final song = songs[index];
                          final isCurrent = provider.currentSong?.id == song.id;

                          return Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 4),
                            child: InteractiveScaleTap(
                              onTap: () => provider.playSong(song, queue: songs),
                              child: Container(
                                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(18),
                                  color: isCurrent
                                      ? AppColors.primary.withValues(alpha: 0.18)
                                      : Colors.white.withValues(alpha: 0.05),
                                  border: Border.all(
                                    color: isCurrent
                                        ? AppColors.primary
                                        : Colors.white.withValues(alpha: 0.12),
                                    width: isCurrent ? 1.6 : 1,
                                  ),
                                  boxShadow: isCurrent
                                      ? [
                                          BoxShadow(
                                            color: AppColors.primary.withValues(alpha: 0.25),
                                            blurRadius: 14,
                                          ),
                                        ]
                                      : null,
                                ),
                                child: Row(
                                  children: [
                                    Stack(
                                      alignment: Alignment.center,
                                      children: [
                                        AlbumArtWidget(
                                          song: song,
                                          size: 48,
                                          borderRadius: 14,
                                        ),
                                        if (isCurrent && provider.isPlaying)
                                          Container(
                                            width: 48,
                                            height: 48,
                                            decoration: BoxDecoration(
                                              color: Colors.black.withValues(alpha: 0.55),
                                              borderRadius: BorderRadius.circular(14),
                                            ),
                                            child: const Center(
                                              child: SoundWaveVisualizer(
                                                isPlaying: true,
                                                barCount: 3,
                                                maxHeight: 16,
                                              ),
                                            ),
                                          ),
                                      ],
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
                                            style: TextStyle(
                                              color: isCurrent ? AppColors.primary : AppColors.textPrimary,
                                              fontWeight: FontWeight.w700,
                                              fontSize: 14,
                                            ),
                                          ),
                                          const SizedBox(height: 3),
                                          Row(
                                            children: [
                                              Flexible(
                                                child: Text(
                                                  song.artist,
                                                  maxLines: 1,
                                                  overflow: TextOverflow.ellipsis,
                                                  style: const TextStyle(
                                                    color: AppColors.textSecondary,
                                                    fontSize: 12,
                                                  ),
                                                ),
                                              ),
                                              if (song.playCount > 0) ...[
                                                const Text(' • ', style: TextStyle(color: AppColors.textMuted)),
                                                Text(
                                                  '${song.playCount}▶',
                                                  style: TextStyle(
                                                    color: AppColors.primary,
                                                    fontSize: 11,
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                              ],
                                            ],
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
                                    const SizedBox(width: 4),
                                    IconButton(
                                      icon: const Icon(Icons.more_vert_rounded, color: AppColors.textSecondary),
                                      onPressed: () => _showSongOptions(context, song),
                                    ),
                                  ],
                                ),
                              ),
                            ),
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
}
