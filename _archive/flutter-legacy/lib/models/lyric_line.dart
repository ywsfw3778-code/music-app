class LyricLine implements Comparable<LyricLine> {
  final Duration timestamp;
  final String text;

  const LyricLine({
    required this.timestamp,
    required this.text,
  });

  @override
  int compareTo(LyricLine other) => timestamp.compareTo(other.timestamp);

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is LyricLine &&
          runtimeType == other.runtimeType &&
          timestamp == other.timestamp &&
          text == other.text;

  @override
  int get hashCode => timestamp.hashCode ^ text.hashCode;

  @override
  String toString() => '[${timestamp.inMinutes.toString().padLeft(2, '0')}:${(timestamp.inSeconds % 60).toString().padLeft(2, '0')}.${(timestamp.inMilliseconds % 1000).toString().padLeft(3, '0')}] $text';
}

class LrcParser {
  static final RegExp _tagPattern = RegExp(r'\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]');

  /// Parses raw LRC string content into a sorted list of [LyricLine]s.
  /// Ignores metadata tags like `[ar:...]`, `[ti:...]`, etc.
  /// Supports multiple timestamp tags per line (e.g., `[00:01.00][00:05.00]Repeat`).
  static List<LyricLine> parse(String? lrcContent) {
    if (lrcContent == null || lrcContent.trim().isEmpty) {
      return const [];
    }

    final lines = lrcContent.split(RegExp(r'\r?\n'));
    final List<LyricLine> result = [];

    for (final rawLine in lines) {
      final line = rawLine.trim();
      if (line.isEmpty) continue;

      final matches = _tagPattern.allMatches(line).toList();
      if (matches.isEmpty) {
        // Line without timestamp (e.g. metadata tag like [ar:Artist] or plain text header)
        continue;
      }

      // Extract the text after all timestamp tags
      final lastMatch = matches.last;
      final text = line.substring(lastMatch.end).trim();

      // Skip lines that have empty text unless user desires empty spacer lines
      // Usually synced lyrics with empty text represent instrumental pauses. We keep them with empty text.
      for (final match in matches) {
        final minutes = int.tryParse(match.group(1) ?? '') ?? 0;
        final seconds = int.tryParse(match.group(2) ?? '') ?? 0;
        final fracStr = match.group(3) ?? '0';

        int milliseconds = 0;
        if (fracStr.isNotEmpty) {
          final frac = int.tryParse(fracStr) ?? 0;
          if (fracStr.length == 1) {
            milliseconds = frac * 100;
          } else if (fracStr.length == 2) {
            milliseconds = frac * 10;
          } else {
            milliseconds = frac;
          }
        }

        final duration = Duration(
          minutes: minutes,
          seconds: seconds,
          milliseconds: milliseconds,
        );

        result.add(LyricLine(timestamp: duration, text: text));
      }
    }

    // Sort by timestamp ascending
    result.sort((a, b) => a.timestamp.compareTo(b.timestamp));
    return result;
  }
}
