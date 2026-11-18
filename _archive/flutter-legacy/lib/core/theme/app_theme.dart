import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:palette_generator_plus/palette_generator_plus.dart';
import '../constants/app_colors.dart';
import '../../models/song_model.dart';

class AppTheme {
  static ThemeData get darkTheme {
    final baseFont = GoogleFonts.poppins();

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      fontFamily: baseFont.fontFamily,
      scaffoldBackgroundColor: AppColors.amoledBlack,
      canvasColor: AppColors.amoledBlack,
      primaryColor: AppColors.primary,
      colorScheme: ColorScheme.dark(
        primary: AppColors.primary,
        secondary: AppColors.accent,
        surface: AppColors.surface,
        onPrimary: AppColors.onPrimary,
        onSurface: AppColors.textPrimary,
      ),
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        iconTheme: IconThemeData(color: AppColors.textPrimary),
      ),
      bottomSheetTheme: const BottomSheetThemeData(
        backgroundColor: Colors.transparent,
        modalBackgroundColor: Colors.transparent,
        elevation: 0,
      ),
      cardTheme: CardThemeData(
        color: AppColors.surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: AppColors.border, width: 1),
        ),
      ),
    );
  }
}

/// Dynamic adaptive palette extractor for natural, authentic song colors.
/// Strictly avoids neon colors; extracts or falls back to organic tones.
class DynamicPalette {
  static final Map<int, Color> _cachedColors = {};

  // Natural authentic organic tones (amber sunset, terracotta, olive sage, warm copper, slate)
  static const List<Color> _naturalPalette = [
    Color(0xFFE5A93C), // Warm Amber Sunset (matches mockup)
    Color(0xFFD48422), // Rich Warm Gold (saturation <= 0.85)
    Color(0xFFC85A32), // Warm Terracotta
    Color(0xFF4D7C55), // Natural Forest Sage
    Color(0xFFB86237), // Warm Copper
    Color(0xFFD99B43), // Desert Sand Gold
    Color(0xFF9E3D48), // Deep Rustic Wine
    Color(0xFF5B7083), // Twilight Slate
  ];

  /// Immediate synchronous color getter (guaranteed zero flash)
  static Color getColor(SongModel? song) {
    if (song == null) return AppColors.primary;
    return _cachedColors[song.id] ?? getFallbackColor(song.id);
  }

  /// Deterministic natural organic fallback derived from song metadata
  static Color getFallbackColor(int songId) {
    return _naturalPalette[songId.abs() % _naturalPalette.length];
  }

  /// Asynchronous extraction using palette_generator_plus with natural clamping
  static Future<Color> extractColor(SongModel song, {ImageProvider? imageProvider}) async {
    if (_cachedColors.containsKey(song.id)) {
      return _cachedColors[song.id]!;
    }

    if (imageProvider != null) {
      try {
        final generator = await PaletteGenerator.fromImageProvider(
          imageProvider,
          maximumColorCount: 16,
        );

        final candidate = generator.vibrantColor?.color ??
            generator.dominantColor?.color ??
            generator.lightVibrantColor?.color ??
            generator.mutedColor?.color;

        if (candidate != null) {
          final hsl = HSLColor.fromColor(candidate);
          // Clamp saturation and lightness to natural, readable ranges on pure black
          final naturalColor = hsl
              .withLightness(hsl.lightness.clamp(0.45, 0.72))
              .withSaturation(hsl.saturation.clamp(0.35, 0.80))
              .toColor();
          _cachedColors[song.id] = naturalColor;
          return naturalColor;
        }
      } catch (_) {
        // Fall back gracefully
      }
    }

    final fallback = getFallbackColor(song.id);
    _cachedColors[song.id] = fallback;
    return fallback;
  }
}
