import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:on_audio_query/on_audio_query.dart' as oaq;
import '../../models/song_model.dart';

class AlbumArtWidget extends StatelessWidget {
  final SongModel song;
  final double size;
  final double borderRadius;
  final bool isSpinning;

  const AlbumArtWidget({
    super.key,
    required this.song,
    this.size = 50.0,
    this.borderRadius = 12.0,
    this.isSpinning = false,
  });

  @override
  Widget build(BuildContext context) {
    Widget content;

    if (!kIsWeb && (Platform.isAndroid || Platform.isIOS)) {
      content = oaq.QueryArtworkWidget(
        id: song.id,
        type: oaq.ArtworkType.AUDIO,
        artworkWidth: 90,
        artworkHeight: 90,
        artworkQuality: FilterQuality.low,
        keepOldArtwork: true,
        artworkFit: BoxFit.cover,
        artworkBorder: BorderRadius.circular(borderRadius),
        nullArtworkWidget: _buildFallback(),
        errorBuilder: (context, error, stackTrace) => _buildFallback(),
      );
    } else {
      content = _buildFallback();
    }

    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: SizedBox(
        width: size,
        height: size,
        child: content,
      ),
    );
  }

  Widget _buildFallback() {
    final colors = [
      [const Color(0xFF6366F1), const Color(0xFF8B5CF6)],
      [const Color(0xFFEC4899), const Color(0xFFF43F5E)],
      [const Color(0xFF10B981), const Color(0xFF06B6D4)],
      [const Color(0xFFF59E0B), const Color(0xFFEF4444)],
      [const Color(0xFF8B5CF6), const Color(0xFF3B82F6)],
    ];
    final pair = colors[song.id.abs() % colors.length];

    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: pair,
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Stack(
        alignment: Alignment.center,
        children: [
          // Subtle Vinyl Groove Ring
          Container(
            width: size * 0.72,
            height: size * 0.72,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(
                color: Colors.white.withValues(alpha: 0.15),
                width: 1.2,
              ),
            ),
          ),
          Container(
            width: size * 0.44,
            height: size * 0.44,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.black.withValues(alpha: 0.25),
              border: Border.all(
                color: Colors.white.withValues(alpha: 0.2),
                width: 1,
              ),
            ),
          ),
          Icon(
            Icons.graphic_eq_rounded,
            color: Colors.white.withValues(alpha: 0.95),
            size: size * 0.35,
          ),
        ],
      ),
    );
  }
}
