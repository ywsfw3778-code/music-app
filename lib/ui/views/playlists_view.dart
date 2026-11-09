import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_animate/flutter_animate.dart';
import '../../core/constants/app_colors.dart';
import '../../models/playlist_model.dart';
import '../../providers/music_provider.dart';
import '../widgets/album_art_widget.dart';
import '../widgets/interactive_scale_tap.dart';

class PlaylistsView extends StatelessWidget {
  const PlaylistsView({super.key});

  void _showCreatePlaylistDialog(BuildContext context) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppColors.surface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
          title: const Text(
            'إنشاء قائمة جديدة',
            style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.bold),
          ),
          content: TextField(
            controller: controller,
            autofocus: true,
            style: const TextStyle(color: AppColors.textPrimary),
            decoration: InputDecoration(
              hintText: 'اسم القائمة (مثال: روقان، رياضة)...',
              hintStyle: const TextStyle(color: AppColors.textMuted),
              filled: true,
              fillColor: AppColors.surfaceLight,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('إلغاء', style: TextStyle(color: AppColors.textMuted)),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primary,
                foregroundColor: Colors.black,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
              ),
              onPressed: () {
                final name = controller.text.trim();
                if (name.isNotEmpty) {
                  Provider.of<MusicProvider>(context, listen: false).createPlaylist(name);
                  Navigator.pop(ctx);
                }
              },
              child: const Text('إنشاء', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        );
      },
    );
  }

  void _openPlaylistDetails(BuildContext context, PlaylistModel playlist) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => PlaylistDetailScreen(playlist: playlist),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        final playlists = provider.playlists;
        final favorites = provider.favoriteSongs;

        return Scaffold(
          backgroundColor: AppColors.background,
          appBar: AppBar(
            title: const Text(
              'قوائم التشغيل',
              style: TextStyle(
                fontWeight: FontWeight.w800,
                fontSize: 22,
                color: AppColors.textPrimary,
              ),
            ),
            actions: [
              IconButton(
                icon: const Icon(Icons.add_rounded, color: AppColors.primary, size: 28),
                onPressed: () => _showCreatePlaylistDialog(context),
                tooltip: 'إنشاء قائمة',
              ),
            ],
          ),
          body: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(),
              slivers: [
                // Create Playlist Action Card
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 12.0),
                    child: InteractiveScaleTap(
                      onTap: () => _showCreatePlaylistDialog(context),
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          gradient: const LinearGradient(
                            colors: [Color(0xFF2E1065), Color(0xFF1E1B4B)],
                          ),
                          borderRadius: BorderRadius.circular(18),
                          border: Border.all(color: AppColors.accent.withValues(alpha: 0.4)),
                        ),
                        child: Row(
                          children: [
                            Container(
                              width: 48,
                              height: 48,
                              decoration: BoxDecoration(
                                color: AppColors.primary,
                                borderRadius: BorderRadius.circular(14),
                              ),
                              child: const Icon(Icons.add_rounded, color: Colors.black, size: 30),
                            ),
                            const SizedBox(width: 16),
                            const Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    'إنشاء قائمة تشغيل جديدة',
                                    style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 16,
                                      color: AppColors.textPrimary,
                                    ),
                                  ),
                                  Text(
                                    'جمع أغانيك المفضلة في مكان واحد',
                                    style: TextStyle(color: AppColors.textSecondary, fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),

                // Built-in Favorites Playlist
                SliverToBoxAdapter(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(vertical: 6.0),
                    child: Material(
                      color: AppColors.surface,
                      clipBehavior: Clip.antiAlias,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                        side: const BorderSide(color: AppColors.border),
                      ),
                      child: ListTile(
                        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                        leading: Container(
                          width: 50,
                          height: 50,
                          decoration: BoxDecoration(
                            gradient: const LinearGradient(
                              colors: [Color(0xFFF43F5E), Color(0xFFBE123C)],
                            ),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: const Icon(Icons.favorite_rounded, color: Colors.white, size: 28),
                        ),
                        title: const Text(
                          'المفضلة ❤️',
                          style: TextStyle(
                            color: AppColors.textPrimary,
                            fontWeight: FontWeight.bold,
                            fontSize: 16,
                          ),
                        ),
                        subtitle: Text(
                          '${favorites.length} أغنية',
                          style: const TextStyle(color: AppColors.textSecondary, fontSize: 13),
                        ),
                        trailing: const Icon(Icons.arrow_forward_ios_rounded, color: AppColors.textMuted, size: 16),
                        onTap: () {
                          if (favorites.isNotEmpty) {
                            provider.playSong(favorites.first, queue: favorites);
                          }
                        },
                      ),
                    ),
                  ),
                ),

                // User Playlists List
                if (playlists.isEmpty)
                  SliverToBoxAdapter(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 40),
                      child: Center(
                        child: Column(
                          children: [
                            Icon(Icons.playlist_play_rounded, size: 64, color: AppColors.textMuted.withValues(alpha: 0.4)),
                            const SizedBox(height: 12),
                            const Text(
                              'لا توجد قوائم مخصصة بعد',
                              style: TextStyle(color: AppColors.textSecondary, fontSize: 15),
                            ),
                          ],
                        ),
                      ),
                    ),
                  )
                else
                  SliverList(
                    delegate: SliverChildBuilderDelegate(
                      (context, index) {
                        final pl = playlists[index];
                        final songCount = pl.songIds.length;

                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 6.0),
                          child: Material(
                            color: AppColors.surface,
                            clipBehavior: Clip.antiAlias,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(16),
                              side: const BorderSide(color: AppColors.border),
                            ),
                            child: ListTile(
                              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                              leading: Container(
                                width: 50,
                                height: 50,
                                decoration: BoxDecoration(
                                  gradient: const LinearGradient(
                                    colors: [Color(0xFF6366F1), Color(0xFF4338CA)],
                                  ),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: const Icon(Icons.queue_music_rounded, color: Colors.white, size: 26),
                              ),
                              title: Text(
                                pl.name,
                                style: const TextStyle(
                                  color: AppColors.textPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                ),
                              ),
                              subtitle: Text(
                                '$songCount أغنية',
                                style: const TextStyle(color: AppColors.textSecondary, fontSize: 13),
                              ),
                              trailing: IconButton(
                                icon: const Icon(Icons.delete_outline_rounded, color: AppColors.textMuted),
                                onPressed: () => provider.deletePlaylist(pl.id),
                              ),
                              onTap: () => _openPlaylistDetails(context, pl),
                            ),
                          ),
                        ).animate().fade().slideY(
                              begin: 0.1,
                              end: 0,
                              delay: Duration(milliseconds: 40 * index),
                            );
                      },
                      childCount: playlists.length,
                    ),
                  ),

                const SliverToBoxAdapter(
                  child: SizedBox(height: 120),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

class PlaylistDetailScreen extends StatelessWidget {
  final PlaylistModel playlist;

  const PlaylistDetailScreen({super.key, required this.playlist});

  @override
  Widget build(BuildContext context) {
    return Consumer<MusicProvider>(
      builder: (context, provider, _) {
        final songs = provider.getSongsForPlaylist(playlist);

        return Scaffold(
          backgroundColor: AppColors.background,
          appBar: AppBar(
            title: Text(playlist.name),
            actions: [
              if (songs.isNotEmpty)
                IconButton(
                  icon: const Icon(Icons.play_circle_fill_rounded, color: AppColors.primary, size: 36),
                  onPressed: () => provider.playSong(songs.first, queue: songs),
                  tooltip: 'تشغيل الكل',
                ),
            ],
          ),
          body: songs.isEmpty
              ? const Center(
                  child: Text(
                    'القائمة فارغة حالياً.\nيمكنك إضافة أغاني من شاشة الأغاني.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: AppColors.textSecondary, fontSize: 15),
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.only(bottom: 120, top: 12),
                  itemCount: songs.length,
                  itemBuilder: (context, index) {
                    final song = songs[index];
                    return Material(
                      color: Colors.transparent,
                      child: ListTile(
                        leading: AlbumArtWidget(song: song, size: 48, borderRadius: 10),
                        title: Text(
                          song.title,
                          maxLines: 1,
                          style: const TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600),
                        ),
                        subtitle: Text(song.artist, style: const TextStyle(color: AppColors.textSecondary)),
                        trailing: IconButton(
                          icon: const Icon(Icons.remove_circle_outline_rounded, color: AppColors.textMuted),
                          onPressed: () => provider.toggleSongInPlaylist(playlist.id, song.id),
                        ),
                        onTap: () => provider.playSong(song, queue: songs),
                      ),
                    );
                  },
                ),
        );
      },
    );
  }
}
