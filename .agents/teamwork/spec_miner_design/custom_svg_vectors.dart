// Custom SVG Vector Definitions for AMOLED Music Player
// All controls are purely vector paths (no standard icon fonts used)

class PlayerSvgVectors {
  // 1. Play Button (Smooth rounded geometry)
  static const String play = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M7.5 4.75C6.67 4.27 5.62 4.87 5.62 5.83V18.17C5.62 19.13 6.67 19.73 7.5 19.25L18.18 13.08C19 12.6 19 11.4 18.18 10.92L7.5 4.75Z" fill="currentColor"/>
</svg>
''';

  // 2. Pause Button (Dual rounded capsules)
  static const String pause = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="5.5" y="4.5" width="4" height="15" rx="2" fill="currentColor"/>
  <rect x="14.5" y="4.5" width="4" height="15" rx="2" fill="currentColor"/>
</svg>
''';

  // 3. Skip Next
  static const String skipNext = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M5.5 5.25C4.8 4.78 3.88 5.28 3.88 6.12V17.88C3.88 18.72 4.8 19.22 5.5 18.75L13.8 13.25C14.4 12.85 14.4 11.15 13.8 10.75L5.5 5.25Z" fill="currentColor"/>
  <rect x="16.5" y="5" width="3" height="14" rx="1.5" fill="currentColor"/>
</svg>
''';

  // 4. Skip Previous
  static const String skipPrevious = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M18.5 5.25C19.2 4.78 20.12 5.28 20.12 6.12V17.88C20.12 18.72 19.2 19.22 18.5 18.75L10.2 13.25C9.6 12.85 9.6 11.15 10.2 10.75L18.5 5.25Z" fill="currentColor"/>
  <rect x="4.5" y="5" width="3" height="14" rx="1.5" fill="currentColor"/>
</svg>
''';

  // 5. Shuffle (Smooth bezier crossover)
  static const String shuffle = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M16 4H20M20 4V8M20 4L14 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M4 20L9 15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M16 20H20M20 20V16M20 20L4 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
''';

  // 6. Repeat All
  static const String repeatAll = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M17 2L21 6M21 6L17 10M21 6H7C4.79 6 3 7.79 3 10V11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M7 22L3 18M3 18L7 14M3 18H17C19.21 18 21 16.21 21 14V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
''';

  // 7. Repeat One
  static const String repeatOne = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M17 2L21 6M21 6L17 10M21 6H7C4.79 6 3 7.79 3 10V11" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M7 22L3 18M3 18L7 14M3 18H17C19.21 18 21 16.21 21 14V13" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M11.5 11.5L12.5 10.5V14.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
''';

  // 8. Favorite Heart (Outline)
  static const String heartOutline = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M12 21.35L10.55 20.03C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5C22 12.28 18.6 15.36 13.45 20.04L12 21.35Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
''';

  // 9. Favorite Heart (Solid Glow)
  static const String heartSolid = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M12 21.35L10.55 20.03C5.4 15.36 2 12.28 2 8.5C2 5.42 4.42 3 7.5 3C9.24 3 10.91 3.81 12 5.09C13.09 3.81 14.76 3 16.5 3C19.58 3 22 5.42 22 8.5C22 12.28 18.6 15.36 13.45 20.04L12 21.35Z" fill="currentColor"/>
</svg>
''';

  // 10. Search (Clean geometric lens)
  static const String search = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
  <path d="M16.5 16.5L21 21" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>
''';

  // 11. Download / Cloud MP3 Download
  static const String download = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M12 3V15M12 15L7 10M12 15L17 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M4 17V19C4 20.1 4.9 21 6 21H18C19.1 21 20 20.1 20 19V17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>
''';

  // 12. Synced Lyrics
  static const String lyrics = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M4 6H20M4 11H14M4 16H18" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
  <circle cx="18.5" cy="11.5" r="2.5" fill="currentColor"/>
</svg>
''';

  // 13. Playlist / Queue
  static const String playlist = '''
<svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M3 6H15M3 12H15M3 18H11" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
  <path d="M18 8V16M18 16C18 17.1 17.1 18 16 18C14.9 18 14 17.1 14 16C14 14.9 14.9 14 16 14C17.1 14 18 14.9 18 16ZM18 8C18 6.9 18.9 6 20 6H21V9H18V8Z" fill="currentColor"/>
</svg>
''';
}
