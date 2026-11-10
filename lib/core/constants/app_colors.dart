import 'package:flutter/material.dart';

enum AppThemeStyle { amoled, naturalDark, midnight, visionOS, cyberNeon, titanium }

/// Pure AMOLED Black palette and natural dynamic theme definitions.
/// Absolutely NO neon/cyan/magenta synthwave colors. True OLED black (#000000).
class AppColors {
  static AppThemeStyle currentStyle = AppThemeStyle.amoled;

  // Pure AMOLED Black base (True #000000 - 0% OLED power consumption on black)
  static const Color amoledBlack = Color(0xFF000000);
  static Color get background => amoledBlack;

  // Clean, sleek natural dark surfaces
  static const Color surfaceDark = Color(0xFF121212);
  static const Color surfaceLightDark = Color(0xFF1E1E1E);
  static const Color surfaceElevatedDark = Color(0xFF282828);

  static Color get surface => surfaceDark;
  static Color get surfaceLight => surfaceLightDark;
  static Color get surfaceElevated => surfaceElevatedDark;

  // Natural warm amber / golden honey default accent (authentic, organic)
  static const Color naturalAmber = Color(0xFFE5A93C);
  static const Color naturalWarmGold = Color(0xFFD97706);

  static Color get primary => naturalAmber;
  static Color get primaryGlow => const Color(0x40E5A93C);
  static Color get onPrimary => Colors.black;

  // Secondary natural accents
  static Color get accent => naturalWarmGold;
  static Color get accentCyan => const Color(0xFF38BDF8);
  static Color get accentPink => const Color(0xFFFB7185);

  // Natural organic gradient
  static List<Color> get primaryGradient => const [
        Color(0xFFE5A93C),
        Color(0xFFD97706),
      ];

  // Ambient backdrops for mockup frame
  static Color get ambientOrb1 => const Color(0xFF1F1F1F);
  static Color get ambientOrb2 => const Color(0xFF141414);

  // Typography (Ultra-crisp Apple/Spotify high legibility)
  static const Color textPrimary = Color(0xFFFFFFFF);
  static const Color textSecondary = Color(0xFFB3B3B3);
  static const Color textMuted = Color(0xFF757575);

  // Borders
  static Color get border => const Color(0xFF242424);
  static const Color borderLight = Color(0xFF333333);

  // State colors
  static const Color error = Color(0xFFEF4444);
  static const Color heart = Color(0xFFE11D48);
}
