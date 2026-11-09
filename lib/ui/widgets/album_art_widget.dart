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
        artworkWidth: size,
        artworkHeight: size,
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
      child: Center(
        child: Icon(
          Icons.music_note_rounded,
          color: Colors.white.withValues(alpha: 0.9),
          size: size * 0.45,
        ),
      ),
    );
  }
}
