# Dispatch for Worker — Milestone 3 (Natural AMOLED UI, Dynamic Palette & Custom SVG Artwork)

You are `worker_m3_ui` (Archetype: teamwork_worker).
Your working directory is: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m3_ui\`
Parent conversation ID: 82c05d99-56aa-4fc3-9440-63d76af0b05f

Read:
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md` (See especially user critical design direction update lines 64-79)
- `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md`
- Visual Mockup Reference: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\web\mockups\natural_player_mockup.jpg`
- Custom SVG vector catalog: `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\spec_miner_design\custom_svg_vectors.dart`

## MANDATORY INTEGRITY WARNING
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## Exclusively Owned Files
- `lib/core/constants/app_svg_icons.dart`
- `lib/core/constants/app_colors.dart`
- `lib/core/theme/app_theme.dart`
- `lib/ui/widgets/app_svg_icon.dart`
- `lib/ui/widgets/now_playing_sheet.dart`
- `lib/ui/widgets/floating_mini_player.dart`
- `lib/ui/widgets/album_art_widget.dart`
- `lib/ui/views/main_navigation_screen.dart`
- `lib/ui/views/home_view.dart`
- `test/widget_test.dart`
- `test/amoled_ui_test.dart`

## Requirements to Implement:
1. **Pure AMOLED Black Base**:
   - Set base scaffold and background colors to true `#000000` (`Color(0xFF000000)`).
   - Zero dark charcoal or midnight blue substitutes. True 100% OLED black.
2. **Soft Blurred Album Art Dynamic Full-Screen Backdrop**:
   - In `NowPlayingSheet` (and player container), render a full-screen ambient backdrop using the active song's album art.
   - Apply `ImageFiltered(imageFilter: ImageFilter.blur(sigmaX: 45, sigmaY: 45))` darkened with black overlay (`Colors.black.withValues(alpha: 0.55)`).
   - Smoothly animate crossfade between covers when the playing track changes.
3. **Natural Adaptive Accent Color Extraction**:
   - Extract dominant accent color from current song's album art using `palette_generator_plus` (or vibrant fallback derived from track metadata).
   - Dynamically tint the large circular Play/Pause button, seekbar progress track, active lyric line, and selected indicators with this natural extracted color.
   - Strictly NO synthwave/neon pink/cyan colors. Keep it natural, clean, and authentic.
4. **Natural Authentic Player Layout (Apple Music / Spotify feel)**:
   - Match the exact visual layout in `web/mockups/natural_player_mockup.jpg`:
     - Clean, standard, uncluttered mobile layout.
     - Top bar: minimal down chevron and title.
     - Large central album art with smooth rounded corners (`BorderRadius.circular(24)`).
     - Song title in bold typography, artist name in secondary font, and favorite heart icon on the right.
     - Sleek minimalist progress seekbar with elapsed and remaining timestamps (`1:42`, `-2:48`).
     - Standard intuitive playback controls row: Shuffle, Previous, large circular Play/Pause button with dynamic glow, Next, Repeat.
     - Frosted glass `LYRICS` pill button at the bottom opening `SyncedLyricsWidget`.
5. **Custom SVG Vector Path Artwork (Zero Icon Fonts)**:
   - Create `AppSvgIcon` widget and `AppSvgIcons` paths in `lib/core/constants/app_svg_icons.dart` (using the 13 paths in `custom_svg_vectors.dart`).
   - Replace ALL standard icon fonts (`Icons.*`, `CupertinoIcons.*`) across `NowPlayingSheet`, `FloatingMiniPlayer`, `MainNavigationScreen`, `HomeView`, and `AlbumArtWidget` with custom SVG vector paths.
6. **Smooth Opening & Transition Animations**:
   - Smooth slide and fade transitions for the now-playing sheet.
   - Gentle spring scale on play/pause tap (`InteractiveScaleTap`).
7. **STRICT NEGATIVE CONSTRAINT**:
   - Absolutely NO equalizer controls, sliders, buttons, or audio filters.
8. **Verification**:
   - Run `flutter analyze` — 0 errors, 0 warnings.
   - Run `flutter test` — all tests must pass cleanly.

Write your handoff report to `c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m3_ui\handoff.md` and notify parent via `send_message`.

## 2026-11-10T15:44:39Z
Resume work at c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\worker_m3_ui\. Read c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\ORIGINAL_REQUEST.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\.agents\teamwork\PROJECT.md, c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app\web\mockups\natural_player_mockup.jpg, and DISPATCH.md. Implement Milestone 3: Pure AMOLED Black (#000000) base, soft blurred album art backdrop, natural dynamic color adaptation, clean Apple/Spotify-style layout matching the mockup, and custom SVG vector path controls replacing all icon fonts. Verify with flutter analyze and flutter test. Write your handoff report to handoff.md in your working directory and notify me via send_message when done.

## 2026-11-10T16:14:09Z
Context: System server restart recovery
Content: You were interrupted right while running `flutter test test/amoled_ui_test.dart`. Please resume: execute `flutter test` and `flutter analyze` to verify your Milestone 3 UI changes, complete your progress log, write `handoff.md`, and report completion so we can proceed to the Milestone 3 gate review.
Action: Run verification, write handoff.md, and reply with your completion report.
