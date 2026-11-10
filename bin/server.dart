import 'dart:convert';
import 'dart:io';
import 'package:youtube_explode_dart/youtube_explode_dart.dart';

const int serverPort = 8081;

Directory resolveMusicDirectory() {
  // Check override environment variable
  final envPath = Platform.environment['MUSIC_DIR'];
  if (envPath != null && envPath.isNotEmpty) {
    final dir = Directory(envPath);
    if (dir.existsSync()) return dir;
  }

  // Primary local HDD drive requirement: E:\music
  final eMusic = Directory(r'E:\music');
  if (eMusic.existsSync()) {
    return eMusic;
  }

  // Windows User Profile fallback
  final userProfile = Platform.environment['USERPROFILE'];
  if (userProfile != null) {
    final winMusic = Directory('$userProfile\\Music');
    if (winMusic.existsSync()) {
      return winMusic;
    }
  }

  // Workspace fallback
  final localFallback = Directory('${Directory.current.path}${Platform.pathSeparator}music_storage');
  if (!localFallback.existsSync()) {
    localFallback.createSync(recursive: true);
  }
  return localFallback;
}

final supportedExtensions = <String>{
  'mp3',
  'm4a',
  'wav',
  'flac',
  'aac',
  'ogg',
};

String getMimeType(String ext) {
  switch (ext.toLowerCase()) {
    case 'mp3':
      return 'audio/mpeg';
    case 'm4a':
    case 'aac':
      return 'audio/mp4';
    case 'wav':
      return 'audio/wav';
    case 'flac':
      return 'audio/flac';
    case 'ogg':
      return 'audio/ogg';
    default:
      return 'application/octet-stream';
  }
}

String cleanName(String name) {
  var s = name.replaceAll(RegExp(r'\.(mp3|m4a|wav|flac|aac|ogg)$', caseSensitive: false), '');
  s = s.replaceAll('_', ' ').trim();
  return s.isEmpty ? 'Unknown' : s;
}

Map<String, dynamic> extractTrackInfo(File file, Directory rootDir, int index) {
  var relativePath = file.path.substring(rootDir.path.length);
  while (relativePath.startsWith(r'\') || relativePath.startsWith('/')) {
    relativePath = relativePath.substring(1);
  }
  final fileName = file.uri.pathSegments.last;
  final withoutExt = cleanName(fileName);

  String artist = 'Local Artist';
  String title = withoutExt;

  if (withoutExt.contains(' - ')) {
    final parts = withoutExt.split(' - ');
    if (parts.length >= 2) {
      artist = parts[0].trim();
      title = parts.sublist(1).join(' - ').trim();
    }
  }

  final stat = file.statSync();
  final dateAdded = stat.modified.millisecondsSinceEpoch ~/ 1000;
  // Deterministic 31-bit positive int id derived from relative path
  final id = (relativePath.hashCode & 0x7FFFFFFF);

  final encodedRelative = Uri.encodeComponent(relativePath.replaceAll(r'\', '/'));
  final streamUri = 'http://localhost:$serverPort/api/stream?file=$encodedRelative';

  return {
    'id': id,
    'title': title,
    'artist': artist,
    'album': 'E:\\music',
    'uri': streamUri,
    'duration': 210000,
    'dateAdded': dateAdded,
    'playCount': 0,
    'lastPlayed': 0,
    'isFavorite': false,
  };
}

List<Map<String, dynamic>> scanMusicFiles(Directory rootDir) {
  final List<Map<String, dynamic>> songs = [];
  try {
    final entities = rootDir.listSync(recursive: true, followLinks: false);
    int index = 0;
    for (final entity in entities) {
      if (entity is File) {
        final ext = entity.path.split('.').last.toLowerCase();
        if (supportedExtensions.contains(ext)) {
          try {
            songs.add(extractTrackInfo(entity, rootDir, index++));
          } catch (e) {
            stderr.writeln('Warning: Failed to extract info for ${entity.path}: $e');
          }
        }
      }
    }
  } catch (e) {
    stderr.writeln('Error scanning directory ${rootDir.path}: $e');
  }

  // Requirement: Sort newest-first (dateAdded DESC)
  songs.sort((a, b) => (b['dateAdded'] as int).compareTo(a['dateAdded'] as int));
  return songs;
}

void applyCorsHeaders(HttpResponse response) {
  response.headers.set('Access-Control-Allow-Origin', '*');
  response.headers.set('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, HEAD');
  response.headers.set('Access-Control-Allow-Headers', 'Origin, Content-Type, Accept, Range');
  response.headers.set('Access-Control-Expose-Headers', 'Content-Length, Content-Range, Accept-Ranges');
}

Future<void> handleStreamRequest(HttpRequest request, Directory rootDir) async {
  final response = request.response;
  applyCorsHeaders(response);

  final rawFile = request.uri.queryParameters['file'];
  if (rawFile == null || rawFile.trim().isEmpty) {
    response.statusCode = HttpStatus.badRequest;
    response.write(jsonEncode({'error': 'Missing "file" query parameter'}));
    await response.close();
    return;
  }

  var decodedPath = rawFile.replaceAll('/', Platform.pathSeparator);
  while (decodedPath.startsWith(Platform.pathSeparator)) {
    decodedPath = decodedPath.substring(1);
  }
  final targetPath = '${rootDir.path}${Platform.pathSeparator}$decodedPath';
  final targetFile = File(targetPath);

  // Security: prevent directory traversal escaping rootDir
  final canonicalRoot = rootDir.resolveSymbolicLinksSync();
  final rootPrefix = canonicalRoot.endsWith(Platform.pathSeparator)
      ? canonicalRoot
      : '$canonicalRoot${Platform.pathSeparator}';

  if (!targetFile.existsSync()) {
    response.statusCode = HttpStatus.notFound;
    response.write(jsonEncode({'error': 'File not found'}));
    await response.close();
    return;
  }

  String canonicalTarget;
  try {
    canonicalTarget = targetFile.resolveSymbolicLinksSync();
  } catch (_) {
    canonicalTarget = targetFile.absolute.path;
  }

  final isInsideRoot = canonicalTarget.startsWith(rootPrefix) || canonicalTarget == canonicalRoot;
  if (!isInsideRoot) {
    response.statusCode = HttpStatus.notFound;
    response.write(jsonEncode({'error': 'File not found or outside root'}));
    await response.close();
    return;
  }

  final ext = targetFile.path.split('.').last.toLowerCase();
  final mimeType = getMimeType(ext);
  final fileLength = targetFile.lengthSync();
  final rangeHeader = request.headers.value(HttpHeaders.rangeHeader);

  response.headers.set(HttpHeaders.contentTypeHeader, mimeType);
  response.headers.set(HttpHeaders.acceptRangesHeader, 'bytes');

  if (rangeHeader != null && rangeHeader.startsWith('bytes=')) {
    final rangeValue = rangeHeader.substring(6).trim();
    final parts = rangeValue.split('-');
    int start;
    int end;

    if (parts[0].isEmpty) {
      // RFC 7233 Suffix Range: bytes=-50 (last 50 bytes)
      final suffixLength = int.tryParse(parts.length > 1 ? parts[1] : '') ?? 0;
      if (suffixLength <= 0) {
        response.statusCode = HttpStatus.requestedRangeNotSatisfiable;
        response.headers.set(HttpHeaders.contentRangeHeader, 'bytes */$fileLength');
        await response.close();
        return;
      }
      start = (fileLength - suffixLength).clamp(0, fileLength - 1);
      end = fileLength - 1;
    } else {
      start = int.tryParse(parts[0]) ?? 0;
      end = (parts.length > 1 && parts[1].isNotEmpty)
          ? (int.tryParse(parts[1]) ?? fileLength - 1)
          : fileLength - 1;
    }

    if (start < 0 || start >= fileLength || start > end) {
      response.statusCode = HttpStatus.requestedRangeNotSatisfiable;
      response.headers.set(HttpHeaders.contentRangeHeader, 'bytes */$fileLength');
      await response.close();
      return;
    }

    if (end >= fileLength) {
      end = fileLength - 1;
    }

    final contentLength = end - start + 1;
    response.statusCode = HttpStatus.partialContent;
    response.headers.set(HttpHeaders.contentRangeHeader, 'bytes $start-$end/$fileLength');
    response.headers.set(HttpHeaders.contentLengthHeader, contentLength.toString());

    if (request.method == 'HEAD') {
      await response.close();
      return;
    }

    await targetFile.openRead(start, end + 1).pipe(response);
  } else {
    response.statusCode = HttpStatus.ok;
    response.headers.set(HttpHeaders.contentLengthHeader, fileLength.toString());

    if (request.method == 'HEAD') {
      await response.close();
      return;
    }

    await targetFile.openRead().pipe(response);
  }
}

Future<void> main(List<String> args) async {
  final musicDir = resolveMusicDirectory();
  stdout.writeln('🎵 Companion Audio Server starting...');
  stdout.writeln('📂 Music library directory: ${musicDir.path}');

  final server = await HttpServer.bind(InternetAddress.anyIPv4, serverPort);
  stdout.writeln('🚀 Server listening at http://localhost:$serverPort/');

  final yt = YoutubeExplode();

  await for (final request in server) {
    try {
      final response = request.response;
      applyCorsHeaders(response);

      if (request.method == 'OPTIONS') {
        response.statusCode = HttpStatus.ok;
        await response.close();
        continue;
      }

      final path = request.uri.path;

      if (path == '/api/health') {
        final songs = scanMusicFiles(musicDir);
        response.headers.contentType = ContentType.json;
        response.statusCode = HttpStatus.ok;
        response.write(jsonEncode({
          'status': 'ok',
          'version': '1.0.0',
          'musicDir': musicDir.path,
          'songsCount': songs.length,
        }));
        await response.close();
      } else if (path == '/api/songs') {
        final songs = scanMusicFiles(musicDir);
        response.headers.contentType = ContentType.json;
        response.statusCode = HttpStatus.ok;
        response.write(jsonEncode(songs));
        await response.close();
      } else if (path == '/api/stream') {
        await handleStreamRequest(request, musicDir);
      } else if (path == '/api/youtube/search') {
        final q = request.uri.queryParameters['q'] ?? '';
        if (q.trim().isEmpty) {
          response.statusCode = HttpStatus.badRequest;
          response.write(jsonEncode({'error': 'Missing query parameter "q"'}));
          await response.close();
          continue;
        }

        try {
          final searchList = await yt.search.search(q);
          final results = searchList.take(20).map((video) {
            return {
              'id': video.id.value,
              'title': video.title,
              'author': video.author,
              'duration': video.duration?.toString() ?? '00:00',
              'thumbnail': video.thumbnails.highResUrl,
            };
          }).toList();

          response.headers.contentType = ContentType.json;
          response.statusCode = HttpStatus.ok;
          response.write(jsonEncode(results));
        } catch (e) {
          response.statusCode = HttpStatus.internalServerError;
          response.write(jsonEncode({'error': 'YouTube search failed: $e'}));
        }
        await response.close();
      } else if (path == '/api/youtube/download' && request.method == 'POST') {
        try {
          final body = await utf8.decodeStream(request);
          final data = jsonDecode(body) as Map<String, dynamic>;
          final videoId = data['id'] as String?;
          final rawTitle = (data['title'] as String?) ?? 'Downloaded Track';

          if (videoId == null || videoId.isEmpty) {
            response.statusCode = HttpStatus.badRequest;
            response.write(jsonEncode({'error': 'Missing "id"'}));
            await response.close();
            continue;
          }

          final safeTitle = rawTitle.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_').trim();
          final targetFile = File('${musicDir.path}${Platform.pathSeparator}$safeTitle.mp3');

          final manifest = await yt.videos.streamsClient.getManifest(videoId);
          final audioStreamInfo = manifest.audioOnly.withHighestBitrate();
          final audioStream = yt.videos.streamsClient.get(audioStreamInfo);

          final fileSink = targetFile.openWrite();
          await audioStream.pipe(fileSink);
          await fileSink.close();

          // Prepare created SongModel representation
          final createdSong = extractTrackInfo(targetFile, musicDir, 0);

          response.headers.contentType = ContentType.json;
          response.statusCode = HttpStatus.ok;
          response.write(jsonEncode(createdSong));
        } catch (e) {
          response.statusCode = HttpStatus.internalServerError;
          response.write(jsonEncode({'error': 'Download failed: $e'}));
        }
        await response.close();
      } else {
        response.statusCode = HttpStatus.notFound;
        response.write(jsonEncode({'error': 'Endpoint not found'}));
        await response.close();
      }
    } catch (e, stack) {
      stderr.writeln('Unhandled request error: $e\n$stack');
      try {
        request.response.statusCode = HttpStatus.internalServerError;
        await request.response.close();
      } catch (_) {}
    }
  }
}
