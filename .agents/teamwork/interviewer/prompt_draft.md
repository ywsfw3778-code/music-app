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
