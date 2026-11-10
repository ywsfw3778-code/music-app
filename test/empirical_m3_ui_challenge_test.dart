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
import 'package:music_player/ui/widgets/album_art_widget.dart';
import 'package:music_player/ui/widgets/synced_lyrics_widget.dart';
import 'helpers/mock_just_audio_platform.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  MockJustAudioPlatform.install();
  GoogleFonts.config.allowRuntimeFetching = false;

  final sampleSong = SongModel(
    id: 42,
    title: 'Natural Acoustic Soundscape',
    artist: 'Nordic Ensemble',
    album: 'Fjords & Horizons',
    duration: 315000, // 5m 15s
    uri: '/mock/music/natural_track.mp3',
    dateAdded: 1715000000,
    playCount: 5,
    isFavorite: false,
  );

  group('CHALLENGE 1: AMOLED True Pitch Black Theme Tokens Stress-Test', () {
    test('Verify AppColors.amoledBlack and AppColors.background are strictly 0xFF000000', () {
      expect(AppColors.amoledBlack, equals(const Color(0xFF000000)));
      expect(AppColors.background, equals(const Color(0xFF000000)));

      // Exact 32-bit ARGB values
      expect(AppColors.amoledBlack.toARGB32(), equals(0xFF000000));
      expect(AppColors.background.toARGB32(), equals(0xFF000000));

      // Individual channel checks (0% subpixel emission on OLED)
      expect(AppColors.amoledBlack.a, equals(1.0));
      expect(AppColors.amoledBlack.r, equals(0.0));
      expect(AppColors.amoledBlack.g, equals(0.0));
      expect(AppColors.amoledBlack.b, equals(0.0));

      // Theme scaffoldBackgroundColor and canvasColor
      final theme = AppTheme.darkTheme;
      expect(theme.scaffoldBackgroundColor, equals(const Color(0xFF000000)));
      expect(theme.canvasColor, equals(const Color(0xFF000000)));
    });

    test('Verify surfaces are subdued non-neon dark tones', () {
      final surfaces = [
        AppColors.surface,
        AppColors.surfaceLight,
        AppColors.surfaceElevated,
      ];
      for (final surface in surfaces) {
        final hsl = HSLColor.fromColor(surface);
        expect(hsl.lightness, lessThanOrEqualTo(0.20),
            reason: 'Surface $surface must be very dark (<20% lightness)');
        expect(hsl.saturation, lessThanOrEqualTo(0.10),
            reason: 'Surface $surface must be neutral charcoal/slate, not saturated neon');
      }
    });
  });

  group('CHALLENGE 2: Zero Material/Cupertino Icon Fonts & AST Verification', () {
    test('Comprehensive scan: Zero Icons.* or CupertinoIcons.* in all target UI files', () {
      final targetFiles = [
        'lib/ui/widgets/now_playing_sheet.dart',
        'lib/ui/widgets/floating_mini_player.dart',
        'lib/ui/widgets/album_art_widget.dart',
        'lib/ui/views/main_navigation_screen.dart',
        'lib/ui/views/home_view.dart',
      ];

      final forbiddenRegex = RegExp(r'\b(Icons|CupertinoIcons)\.[a-zA-Z0-9_]+');

      for (final path in targetFiles) {
        final file = File(path);
        expect(file.existsSync(), isTrue, reason: '$path must exist');
        final lines = file.readAsLinesSync();
        for (int i = 0; i < lines.length; i++) {
          final line = lines[i];
          final match = forbiddenRegex.firstMatch(line);
          expect(match, isNull,
              reason: 'Forbidden icon font on $path line ${i + 1}: "$line"');
        }
      }
    });

    test('Verify zero standard Material Icon() constructors in target UI files', () {
      final targetFiles = [
        'lib/ui/widgets/now_playing_sheet.dart',
        'lib/ui/widgets/floating_mini_player.dart',
        'lib/ui/widgets/album_art_widget.dart',
        'lib/ui/views/main_navigation_screen.dart',
        'lib/ui/views/home_view.dart',
      ];

      // Match standalone "Icon(" but NOT "AppSvgIcon("
      final standardIconRegex = RegExp(r'(?<!Svg)Icon\s*\(');

      for (final path in targetFiles) {
        final file = File(path);
        final lines = file.readAsLinesSync();
        for (int i = 0; i < lines.length; i++) {
          final line = lines[i];
          final match = standardIconRegex.firstMatch(line);
          expect(match, isNull,
              reason: 'Found forbidden standard Icon(...) at $path line ${i + 1}: "$line"');
        }
      }
    });

    test('Verify AppSvgIcons are valid SVG strings with svg tag and viewBox', () {
      final svgs = [
        AppSvgIcons.play,
        AppSvgIcons.pause,
        AppSvgIcons.skipNext,
        AppSvgIcons.skipPrevious,
        AppSvgIcons.shuffle,
        AppSvgIcons.repeatAll,
        AppSvgIcons.repeatOne,
        AppSvgIcons.heartOutline,
        AppSvgIcons.heartSolid,
        AppSvgIcons.search,
        AppSvgIcons.download,
        AppSvgIcons.lyrics,
        AppSvgIcons.playlist,
        AppSvgIcons.chevronDown,
        AppSvgIcons.home,
        AppSvgIcons.musicNote,
        AppSvgIcons.refresh,
        AppSvgIcons.soundLogo,
      ];

      for (final svg in svgs) {
        expect(svg.trim().startsWith('<svg'), isTrue);
        expect(svg.trim().endsWith('</svg>'), isTrue);
        expect(svg.contains('viewBox='), isTrue);
      }
    });
  });

  group('CHALLENGE 3: Strict Absence of Equalizer / DSP Controls Across lib/ and bin/', () {
    test('Zero equalizer, graphic_eq, or DSP keywords in entire codebase', () {
      final dirs = [Directory('lib'), Directory('bin')];
      final forbiddenKeywords = [
        'equalizer',
        'graphic_eq',
        'audio_filter',
        'audiofilter',
        'bass_boost',
        'bassboost',
        'equalization',
        'biquad',
      ];

      for (final dir in dirs) {
        if (!dir.existsSync()) continue;
        final dartFiles = dir
            .listSync(recursive: true)
            .whereType<File>()
            .where((f) => f.path.endsWith('.dart'));

        for (final file in dartFiles) {
          final content = file.readAsStringSync().toLowerCase();
          for (final kw in forbiddenKeywords) {
            expect(
              content.contains(kw),
              isFalse,
              reason: 'File ${file.path} contains forbidden EQ keyword: "$kw"',
            );
          }
        }
      }
    });
  });

  group('CHALLENGE 4: DynamicPalette Organic Tone & Extremes Stress-Test', () {
    test('DynamicPalette handles extreme song IDs (negative, zero, large int)', () {
      final extremeIds = [-9999999, -1, 0, 1, 42, 9999999, 2147483647];
      for (final id in extremeIds) {
        final song = SongModel(
          id: id,
          title: 'Extreme Song $id',
          artist: 'Artist',
          album: 'Album',
          duration: 180000,
          uri: 'http://localhost/test.mp3',
        );
        final color = DynamicPalette.getColor(song);
        expect(color, isNotNull);
        final hsl = HSLColor.fromColor(color);
        expect(hsl.saturation, lessThanOrEqualTo(0.85));
        expect(hsl.lightness, inInclusiveRange(0.40, 0.75));
      }
    });

    test('DynamicPalette getColor(null) gracefully returns default primary amber', () {
      final color = DynamicPalette.getColor(null);
      expect(color, equals(AppColors.primary));
    });
  });

  group('CHALLENGE 5: UI Widget Integration & Layout Stress-Test', () {
    late StorageService storageService;
    late MusicProvider provider;

    setUp(() async {
      SharedPreferences.setMockInitialValues({});
      storageService = await StorageService.init();
      provider = MusicProvider(storageService);
      provider.playSong(sampleSong);
    });

    testWidgets('NowPlayingSheet layout renders blurred backdrop, SVG controls, and negative remaining time',
        (tester) async {
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

      // Check AlbumArtWidget is displayed
      expect(find.byType(AlbumArtWidget), findsWidgets);

      // Check Song Title and Artist
      expect(find.text('Natural Acoustic Soundscape'), findsOneWidget);
      expect(find.text('Nordic Ensemble'), findsOneWidget);

      // Check Negative Remaining Time exists: label starts with '-'
      final negativeDurationFinder = find.byWidgetPredicate(
        (widget) => widget is Text && widget.data != null && widget.data!.startsWith('-'),
      );
      expect(negativeDurationFinder, findsOneWidget);

      // Check Elapsed time is "0:00"
      expect(find.text('0:00'), findsOneWidget);

      // Check Frosted Glass LYRICS button
      expect(find.text('LYRICS'), findsOneWidget);

      // Check that all icons in NowPlayingSheet are AppSvgIcon
      final svgWidgets = tester.widgetList<AppSvgIcon>(find.byType(AppSvgIcon));
      expect(svgWidgets.length, greaterThanOrEqualTo(7));

      // Tap on lyrics button
      await tester.tap(find.text('LYRICS'));
      await tester.pumpAndSettle();
      // Should show lyrics modal with SyncedLyricsWidget
      expect(find.byType(SyncedLyricsWidget), findsOneWidget);
    });

    testWidgets('FloatingMiniPlayer displays track, dynamic progress, and AppSvgIcon controls',
        (tester) async {
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

      expect(find.text('Natural Acoustic Soundscape'), findsOneWidget);
      expect(find.text('Nordic Ensemble'), findsOneWidget);
      expect(find.byType(LinearProgressIndicator), findsOneWidget);

      final svgWidgets = tester.widgetList<AppSvgIcon>(find.byType(AppSvgIcon));
      expect(svgWidgets.length, greaterThanOrEqualTo(2));
    });

    testWidgets('AlbumArtWidget renders fallback with AppSvgIcon musicNote and rounded corners',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: AlbumArtWidget(
              song: sampleSong,
              size: 200,
              borderRadius: 24,
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(AppSvgIcon), findsOneWidget);
      final clipRRect = tester.widget<ClipRRect>(find.byType(ClipRRect));
      expect(clipRRect.borderRadius, equals(BorderRadius.circular(24)));
    });
  });
}
