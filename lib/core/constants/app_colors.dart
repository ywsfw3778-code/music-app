import 'package:flutter/material.dart';

enum AppThemeStyle { visionOS, cyberNeon, titanium }

class AppColors {
  static AppThemeStyle currentStyle = AppThemeStyle.visionOS;

  // Background
  static Color get background {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF090912); // Deep Obsidian
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF0D0614); // Cyber Midnight
      case AppThemeStyle.titanium:
        return const Color(0xFF050508); // Pure Titanium Dark
    }
  }

  // Surface
  static Color get surface {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF131322);
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF190F24);
      case AppThemeStyle.titanium:
        return const Color(0xFF111217);
    }
  }

  static Color get surfaceLight {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF1C1C30);
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF261536);
      case AppThemeStyle.titanium:
        return const Color(0xFF1C1D24);
    }
  }

  static Color get surfaceElevated {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF24243E);
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF331B47);
      case AppThemeStyle.titanium:
        return const Color(0xFF262832);
    }
  }

  // Primary Accent
  static Color get primary {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF8B5CF6); // Electric Violet
      case AppThemeStyle.cyberNeon:
        return const Color(0xFFFF007A); // Hot Magenta / Cyber Pink
      case AppThemeStyle.titanium:
        return const Color(0xFFE2E8F0); // Platinum Silver
    }
  }

  static Color get primaryGlow {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0x668B5CF6);
      case AppThemeStyle.cyberNeon:
        return const Color(0x66FF007A);
      case AppThemeStyle.titanium:
        return const Color(0x55FFFFFF);
    }
  }

  static Color get onPrimary {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return Colors.white;
      case AppThemeStyle.cyberNeon:
        return Colors.white;
      case AppThemeStyle.titanium:
        return Colors.black;
    }
  }

  // Secondary Accents
  static Color get accent {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF06B6D4); // Neon Cyan
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF00E5FF); // Electric Blue
      case AppThemeStyle.titanium:
        return const Color(0xFF94A3B8); // Slate Ice
    }
  }

  static Color get accentCyan => const Color(0xFF06B6D4);
  static Color get accentPink => const Color(0xFFEC4899);

  // Linear Gradient for primary hero elements
  static List<Color> get primaryGradient {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const [Color(0xFF8B5CF6), Color(0xFF06B6D4)];
      case AppThemeStyle.cyberNeon:
        return const [Color(0xFFFF007A), Color(0xFF7928CA)];
      case AppThemeStyle.titanium:
        return const [Color(0xFFFFFFFF), Color(0xFF94A3B8)];
    }
  }

  // Ambient mesh blobs behind the glass
  static Color get ambientOrb1 {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF8B5CF6); // Violet
      case AppThemeStyle.cyberNeon:
        return const Color(0xFFFF007A); // Magenta
      case AppThemeStyle.titanium:
        return const Color(0xFF64748B); // Slate
    }
  }

  static Color get ambientOrb2 {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF06B6D4); // Cyan
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF00E5FF); // Electric Blue
      case AppThemeStyle.titanium:
        return const Color(0xFF475569); // Titanium
    }
  }

  // Typography
  static const Color textPrimary = Color(0xFFF8FAFC);
  static const Color textSecondary = Color(0xFF94A3B8);
  static const Color textMuted = Color(0xFF64748B);

  // Borders
  static Color get border {
    switch (currentStyle) {
      case AppThemeStyle.visionOS:
        return const Color(0xFF2A2A44);
      case AppThemeStyle.cyberNeon:
        return const Color(0xFF381F4B);
      case AppThemeStyle.titanium:
        return const Color(0xFF22242D);
    }
  }

  static const Color borderLight = Color(0xFF38385A);
  static const Color error = Color(0xFFEF4444);
  static const Color heart = Color(0xFFF43F5E);
}
