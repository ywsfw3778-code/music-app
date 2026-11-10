# Original User Request

## Approved Task Prompt — 2026-11-10T14:19:33Z

The user requested: "اعمل تيم واحد مختص في ديزاين بيشوف افضل اشكال علي النت ويوريهالي وانا اختار منها و وواحد يبقا بيجرب الاخطاء يتاكد انه مش بيهنج اعمل تيم كامل برمجه كل واحد بيبرمج واجهه ويصلح الاخطاء ويتاكد من انه استخدم الاسكيلز المناسبه وبرمجه صحيحه ويبقا بيجربو علي المتصفح، وضيف ايجنت يتاكد من الاكواد صحيحه ومفيش فيها اخطاء و 3 بيكتبو الكود كل واحد مختص في كود حتت معينه (مطور للواجهات ورسومات الـ SVG، ومطور لخدمات الصوت والـ HDD، ومطور لبحث وتحميل يوتيوب والكلمات المتزامنة، مع مدقق لصحة الكود وفاحص للاستقرار والمتصفح)".
Enhance the Flutter music player app running on Chrome web with an AMOLED pure black theme, dynamic color adaptation from album covers, smooth animations, custom SVG path drawings instead of icon fonts, local HDD music playback from E:\music, YouTube audio search/MP3 download, synced lyrics, and newest-first library sorting.

Working directory: c:\Users\Pc\Desktop\_Archive\Projects_Dev\music app
Integrity mode: development

## Requirements

### R1. AMOLED Visual Interface & Custom SVG Artwork
- The player interface must use pure AMOLED black (#000000) as its base theme.
- Dynamic background gradients and accent colors must automatically adapt to the album art of the active song.
- Implement smooth opening animations and screen/component transition animations.
- All controls and icons must be custom-drawn SVG vector paths specifically crafted for this player; do not use standard icon fonts.

### R2. Design Research & User Review
- The design specialist must explore modern AMOLED music player designs and present visual options/mockups to the user for review and selection before final UI implementation.

### R3. Local HDD Playback & Explicit Constraints
- Load and play local music files from `E:\music`.
- Display the playlist with newly added and downloaded songs at the top.
- Strict constraint: Absolutely NO EQUALIZER feature, sliders, or UI controls (explicitly forbidden by user).

### R4. YouTube Search, MP3 Download & Synced Lyrics
- Implement search to find songs on YouTube and download them locally as MP3 audio files into the music library.
- Newly downloaded MP3 files must automatically appear at the top of the track list.
- Fetch and display synced/timed lyrics from the web that scroll and animate synchronously with song playback.

### R5. Code Quality, Chrome Web Verification & Stability
- The app must run cleanly and be verified on Chrome web (e.g. at `http://localhost:8080`).
- Ensure code correctness and cleanliness across all specialized components through static analysis (`flutter analyze`) and automated tests (`flutter test`).
- Prevent any stuttering, freezing, or crashes during audio playback, background transitions, search, or downloading.

## Acceptance Criteria

### Visuals & Motion
- [ ] Base background color is pure AMOLED black (#000000).
- [ ] Background gradients and accent colors dynamically update when the playing track changes.
- [ ] All player controls (play, pause, next, previous, search, download) render using custom SVG vector paths without standard icon font glyphs.
- [ ] Opening animations and transitions render smoothly without lag or jank.
- [ ] Design mockups/options are presented to the user for selection.

### Local Playback & Constraints
- [ ] Successfully reads, lists, and plays audio files located in `E:\music`.
- [ ] Song list places newly added and downloaded tracks at the top.
- [ ] No equalizer controls, buttons, screens, or audio filters are present in the app.

### Online Search, Download & Lyrics
- [ ] Searching returns online YouTube audio results.
- [ ] Selected track can be downloaded locally as MP3 and added to the library.
- [ ] Synced / timed lyrics are fetched from the web and scroll in sync with playback.

### Verification & Reliability
- [ ] Static analysis passes cleanly (`flutter analyze`) with zero errors.
- [ ] Automated test suite passes (`flutter test`) with no errors or regressions.
- [ ] App launches and operates reliably on Chrome web at `http://localhost:8080` without crashes or freezing.


## Follow-up — 2026-11-10T14:35:54Z

Critical Design Direction Update from User:
The user explicitly rejected neon colors, blue/purple cyber aesthetics, and futuristic concepts:
"انا عايز شكل طبيعي بتاع MUSIC PLAYER زي الناس مش عايز الوان النيون والازرق والبنفسجي واللون يبقا اسود وبيتغير خالص وبتبقا خلفيه الاغانيه في الشكل الخلفي للبنرامج والايكونات بتتغير معاها وهكذا فاهم ؟"

Core Requirements for UI:
1. Natural, authentic mobile Music Player interface (like standard Apple Music / Spotify / Samsung Music):
   - Clean, standard, uncluttered mobile layout.
   - Large prominent album art in the center with smooth rounded corners.
   - Song title, artist name, clean progress bar with timestamp labels.
   - Standard intuitive playback controls (Shuffle, Previous, Play/Pause, Next, Repeat).
2. Dynamic Background & Adaptive Accent:
   - Base background is deep dark AMOLED black (#000000).
   - When any song plays: The album cover itself becomes the ambient blurred backdrop (soft blur over pure black AMOLED) across the entire screen.
   - The dominant colors extracted from the active song's album cover dynamically tint the buttons, slider, and icons.
   - Completely NO neon/cyan/magenta synthwave colors. Strictly natural, elegant, dynamic color extraction.


## Follow-up — 2026-11-10T14:38:04Z

USER DESIGN APPROVAL & NEW FEATURE REQUIREMENT:
1. User approved the Natural UI design 100%: "موافق علي الديزاين ده تمام"
   - Pure black AMOLED (#000000).
   - Blurred album cover backdrop transitioning dynamically.
   - Dynamic palette extraction for controls and scrubbers.
   - Natural layout (Apple Music / Spotify clean style).

2. New feature requirement: Crossfade (كروس فيد):
   - Seamless smooth audio crossfading between tracks (e.g. configurable 3s-12s fade out of ending track / fade in of incoming track) without gaps.


## Follow-up — 2026-11-10T16:12:34Z

User requested immediate resumption of teamwork execution:
"كمل شغله وشغل الفريق تاني"

Instructions:
Resume teamwork execution immediately. Check current progress, verify tests and static analysis, finalize Milestone 3 (Natural AMOLED UI & SVG Art), and execute Milestone 4 (Final end-to-end verification and Chrome web launch).
