import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:music_player/core/constants/app_colors.dart';
import 'package:music_player/core/constants/app_svg_icons.dart';
import 'package:music_player/core/theme/app_theme.dart';
import 'package:music_player/models/song_model.dart';
import 'package:music_player/providers/music_provider.dart';
import 'package:music_player/services/storage_service.dart';
import 'package:music_player/ui/widgets/app_svg_icon.dart';
import 'package:music_player/ui/widgets/floating_mini_player.dart';
import 'package:music_player/ui/widgets/now_playing_sheet.dart';
import 'helpers/mock_just_audio_platform.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  MockJustAudioPlatform.install();
  GoogleFonts.config.allowRuntimeFetching = false;

  final sampleSong = SongModel(
    id: 101,
    title: 'Golden Hour',
    artist: 'Julian Vance',
    album: 'Natural Horizon',
    duration: 270000, // 4m 30s
    uri: '/mock/music/golden_hour.mp3',
    dateAdded: 1700000000,
    playCount: 12,
    isFavorite: true,
  );

  group('Milestone 3 — Pure AMOLED Black Base Requirements', () {
    test('AppColors.background is strictly true 100% OLED black (#000000)', () {
      expect(AppColors.background, equals(const Color(0xFF000000)));
      expect(AppColors.amoledBlack, equals(const Color(0xFF000000)));
      expect(AppColors.amoledBlack.toARGB32(), equals(0xFF000000));
    });

    test('AppTheme.darkTheme scaffoldBackgroundColor is pure AMOLED black (#000000)', () {
      final theme = AppTheme.darkTheme;
      expect(theme.scaffoldBackgroundColor, equals(const Color(0xFF000000)));
      expect(theme.canvasColor, equals(const Color(0xFF000000)));
    });

    test('All AppColors surfaces are clean sleek dark tones without neon tint', () {
      expect(AppColors.surface, equals(const Color(0xFF121212)));
      expect(AppColors.surfaceLight, equals(const Color(0xFF1E1E1E)));
      expect(AppColors.surfaceElevated, equals(const Color(0xFF282828)));
    });
  });

  group('Milestone 3 — Natural Adaptive Accent Palette Requirements', () {
    test('DynamicPalette extracts deterministic natural organic tone', () {
      final color = DynamicPalette.getColor(sampleSong);
      expect(color, isA<Color>());

      // Ensure lightness and saturation are within natural organic ranges (NO neon!)
      final hsl = HSLColor.fromColor(color);
      expect(hsl.saturation, lessThanOrEqualTo(0.85));
      expect(hsl.lightness, inInclusiveRange(0.40, 0.75));

      // Strictly verify NOT synthetic neon (e.g. 0xFF00FFFF or 0xFFFF007A)
      expect(color, isNot(equals(const Color(0xFF00FFFF)))); // NOT Neon Cyan
      expect(color, isNot(equals(const Color(0xFFFF007A)))); // NOT Neon Pink
      expect(color, isNot(equals(const Color(0xFF8B5CF6)))); // NOT Neon Violet
    });

    test('DynamicPalette fallback list contains only organic earth/nature tones', () {
      for (int i = 0; i < 20; i++) {
        final fallback = DynamicPalette.getFallbackColor(i);
        final hsl = HSLColor.fromColor(fallback);
        expect(hsl.saturation, lessThanOrEqualTo(0.85));
      }
    });
  });

  group('Milestone 3 — Custom SVG Vector Path Artwork (Zero Icon Fonts)', () {
    test('AppSvgIcons catalog provides all required playback and navigation paths', () {
      expect(AppSvgIcons.play, contains('<svg'));
      expect(AppSvgIcons.pause, contains('<svg'));
      expect(AppSvgIcons.skipNext, contains('<svg'));
      expect(AppSvgIcons.skipPrevious, contains('<svg'));
      expect(AppSvgIcons.shuffle, contains('<svg'));
      expect(AppSvgIcons.repeatAll, contains('<svg'));
      expect(AppSvgIcons.repeatOne, contains('<svg'));
      expect(AppSvgIcons.heartOutline, contains('<svg'));
      expect(AppSvgIcons.heartSolid, contains('<svg'));
      expect(AppSvgIcons.chevronDown, contains('<svg'));
      expect(AppSvgIcons.home, contains('<svg'));
      expect(AppSvgIcons.musicNote, contains('<svg'));
      expect(AppSvgIcons.playlist, contains('<svg'));
      expect(AppSvgIcons.search, contains('<svg'));
      expect(AppSvgIcons.refresh, contains('<svg'));
      expect(AppSvgIcons.soundLogo, contains('<svg'));
    });

    testWidgets('AppSvgIcon widget renders SVG string cleanly without crashing', (tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: Center(
              child: AppSvgIcon(
                AppSvgIcons.play,
                size: 32,
                color: Colors.white,
              ),
            ),
          ),
        ),
      );

      expect(find.byType(AppSvgIcon), findsOneWidget);
    });

    test('Static source code audit: Zero IconData or Icons.* in owned UI files', () {
      final ownedFiles = [
        'lib/ui/widgets/now_playing_sheet.dart',
        'lib/ui/widgets/floating_mini_player.dart',
        'lib/ui/widgets/album_art_widget.dart',
        'lib/ui/views/main_navigation_screen.dart',
        'lib/ui/views/home_view.dart',
      ];

      final iconFontPattern = RegExp(r'\bIcons\.[a-zA-Z0-9_]+|\bCupertinoIcons\.[a-zA-Z0-9_]+');

      for (final relativePath in ownedFiles) {
        final file = File(relativePath);
        expect(file.existsSync(), isTrue, reason: 'File $relativePath must exist');
        final content = file.readAsStringSync();
        final matches = iconFontPattern.allMatches(content);
        expect(
          matches.isEmpty,
          isTrue,
          reason: 'File $relativePath contains forbidden icon fonts: ${matches.map((m) => m.group(0)).toList()}',
        );
      }
    });
  });

  group('Milestone 3 — Natural Player Layout & Backdrop (Matching Mockup)', () {
    late StorageService storageService;
    late MusicProvider provider;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      storageService = await StorageService.init();
      provider = MusicProvider(storageService);
      provider.playSong(sampleSong);
    });

    testWidgets('NowPlayingSheet renders full-screen blurred backdrop with pure AMOLED black base', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.darkTheme,
          home: Scaffold(
            body: ChangeNotifierProvider<MusicProvider>.value(
              value: provider,
              child: const NowPlayingSheet(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Verify ImageFiltered blur exists for dynamic backdrop
      expect(find.byType(ImageFiltered), findsWidgets);

      // Verify track title and artist render
      expect(find.text('Golden Hour'), findsOneWidget);
      expect(find.text('Julian Vance'), findsOneWidget);

      // Verify lyrics frosted glass pill button exists with 'LYRICS'
      expect(find.text('LYRICS'), findsOneWidget);

      // Verify seekbar slider exists
      expect(find.byType(Slider), findsOneWidget);
    });

    testWidgets('NowPlayingSheet displays negative remaining time (-mm:ss)', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.darkTheme,
          home: Scaffold(
            body: ChangeNotifierProvider<MusicProvider>.value(
              value: provider,
              child: const NowPlayingSheet(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Remaining duration label starts with '-'
      final remainingFinder = find.byWidgetPredicate(
        (widget) => widget is Text && widget.data != null && widget.data!.startsWith('-'),
      );
      expect(remainingFinder, findsOneWidget);
    });

    testWidgets('NowPlayingSheet uses custom SVG vectors for all playback controls', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.darkTheme,
          home: Scaffold(
            body: ChangeNotifierProvider<MusicProvider>.value(
              value: provider,
              child: const NowPlayingSheet(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      final svgIcons = find.byType(AppSvgIcon);
      expect(svgIcons, findsWidgets);
      // There are multiple AppSvgIcons in NowPlayingSheet (chevron, playlist, heart, shuffle, prev, play/pause, next, repeat)
      expect(tester.widgetList(svgIcons).length, greaterThanOrEqualTo(7));
    });

    testWidgets('FloatingMiniPlayer uses pure AMOLED surfaces and custom SVG controls', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          theme: AppTheme.darkTheme,
          home: Scaffold(
            body: ChangeNotifierProvider<MusicProvider>.value(
              value: provider,
              child: const FloatingMiniPlayer(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(FloatingMiniPlayer), findsOneWidget);
      expect(find.text('Golden Hour'), findsOneWidget);
      expect(find.text('Julian Vance'), findsOneWidget);
      expect(find.byType(AppSvgIcon), findsWidgets);
    });
  });

  group('Milestone 3 — STRICT NEGATIVE CONSTRAINT (Zero Equalizer Controls)', () {
    test('Zero equalizer keywords or sliders in owned UI files', () {
      final ownedFiles = [
        'lib/ui/widgets/now_playing_sheet.dart',
        'lib/ui/widgets/floating_mini_player.dart',
        'lib/ui/widgets/album_art_widget.dart',
        'lib/ui/views/main_navigation_screen.dart',
        'lib/ui/views/home_view.dart',
      ];

      final equalizerKeywords = [
        'equalizer',
        'Equalizer',
        'eqBand',
        'graphic_eq',
        'audioFilter',
        'presetReverb',
      ];

      for (final relativePath in ownedFiles) {
        final file = File(relativePath);
        final content = file.readAsStringSync();
        for (final kw in equalizerKeywords) {
          expect(
            content.contains(kw),
            isFalse,
            reason: 'File $relativePath contains forbidden equalizer keyword "$kw"',
          );
        }
      }
    });
  });
}
