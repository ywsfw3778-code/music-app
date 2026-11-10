import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:on_audio_query/on_audio_query.dart' as oaq;
import '../../core/constants/app_svg_icons.dart';
import '../../models/song_model.dart';
import 'app_svg_icon.dart';

class AlbumArtWidget extends StatelessWidget {
  final SongModel song;
  final double size;
  final double borderRadius;
  final bool isSpinning;
  final BoxFit fit;

  const AlbumArtWidget({
    super.key,
    required this.song,
    this.size = 50.0,
    this.borderRadius = 12.0,
    this.isSpinning = false,
    this.fit = BoxFit.cover,
  });

  @override
  Widget build(BuildContext context) {
    Widget content;

    if (!kIsWeb && (Platform.isAndroid || Platform.isIOS)) {
      content = oaq.QueryArtworkWidget(
        id: song.id,
        type: oaq.ArtworkType.AUDIO,
        artworkWidth: 320,
        artworkHeight: 320,
        artworkQuality: FilterQuality.medium,
        keepOldArtwork: true,
        artworkFit: fit,
        artworkBorder: BorderRadius.circular(borderRadius),
        nullArtworkWidget: _buildFallback(),
        errorBuilder: (context, error, stackTrace) => _buildFallback(),
      );
    } else {
      content = _buildFallback();
    }

    final isFinite = size.isFinite && size > 0;

    return ClipRRect(
      borderRadius: BorderRadius.circular(borderRadius),
      child: SizedBox(
        width: isFinite ? size : null,
        height: isFinite ? size : null,
        child: content,
      ),
    );
  }

  Widget _buildFallback() {
    // Natural organic color palettes (warm sunset, terracotta, olive, bronze, slate)
    final naturalPairs = [
      [const Color(0xFFB45309), const Color(0xFFD97706)], // Warm Amber Sunset
      [const Color(0xFF9A3412), const Color(0xFFC2410C)], // Terracotta Copper
      [const Color(0xFF15803D), const Color(0xFF166534)], // Forest Sage
      [const Color(0xFF78350F), const Color(0xFF92400E)], // Warm Earth Mocha
      [const Color(0xFF334155), const Color(0xFF1E293B)], // Twilight Slate
      [const Color(0xFF881337), const Color(0xFF4C0519)], // Deep Rustic Wine
    ];
    final pair = naturalPairs[song.id.abs() % naturalPairs.length];
    final isFinite = size.isFinite && size > 0;
    final referenceDimension = isFinite ? size : 200.0;

    return Container(
      width: isFinite ? size : double.infinity,
      height: isFinite ? size : double.infinity,
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
            width: referenceDimension * 0.72,
            height: referenceDimension * 0.72,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(
                color: Colors.white.withValues(alpha: 0.12),
                width: 1.2,
              ),
            ),
          ),
          Container(
            width: referenceDimension * 0.44,
            height: referenceDimension * 0.44,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: Colors.black.withValues(alpha: 0.25),
              border: Border.all(
                color: Colors.white.withValues(alpha: 0.18),
                width: 1,
              ),
            ),
          ),
          AppSvgIcon(
            AppSvgIcons.musicNote,
            color: Colors.white.withValues(alpha: 0.90),
            size: referenceDimension * 0.32,
          ),
        ],
      ),
    );
  }
}
