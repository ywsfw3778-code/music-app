class SongModel {
  final int id;
  final String title;
  final String artist;
  final String album;
  final String uri;
  final int duration; // in milliseconds
  final int? albumId;
  final int dateAdded; // timestamp for sorting latest downloaded
  int playCount;
  int lastPlayed;
  bool isFavorite;

  SongModel({
    required this.id,
    required this.title,
    required this.artist,
    required this.album,
    required this.uri,
    required this.duration,
    this.albumId,
    this.dateAdded = 0,
    this.playCount = 0,
    this.lastPlayed = 0,
    this.isFavorite = false,
  });

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'artist': artist,
      'album': album,
      'uri': uri,
      'duration': duration,
      'albumId': albumId,
      'dateAdded': dateAdded,
      'playCount': playCount,
      'lastPlayed': lastPlayed,
      'isFavorite': isFavorite,
    };
  }

  factory SongModel.fromJson(Map<String, dynamic> json) {
    return SongModel(
      id: json['id'] as int,
      title: json['title'] as String? ?? 'Unknown Title',
      artist: json['artist'] as String? ?? 'Unknown Artist',
      album: json['album'] as String? ?? 'Unknown Album',
      uri: json['uri'] as String? ?? '',
      duration: json['duration'] as int? ?? 0,
      albumId: json['albumId'] as int?,
      dateAdded: json['dateAdded'] as int? ?? 0,
      playCount: json['playCount'] as int? ?? 0,
      lastPlayed: json['lastPlayed'] as int? ?? 0,
      isFavorite: json['isFavorite'] as bool? ?? false,
    );
  }

  SongModel copyWith({
    int? id,
    String? title,
    String? artist,
    String? album,
    String? uri,
    int? duration,
    int? albumId,
    int? dateAdded,
    int? playCount,
    int? lastPlayed,
    bool? isFavorite,
  }) {
    return SongModel(
      id: id ?? this.id,
      title: title ?? this.title,
      artist: artist ?? this.artist,
      album: album ?? this.album,
      uri: uri ?? this.uri,
      duration: duration ?? this.duration,
      albumId: albumId ?? this.albumId,
      dateAdded: dateAdded ?? this.dateAdded,
      playCount: playCount ?? this.playCount,
      lastPlayed: lastPlayed ?? this.lastPlayed,
      isFavorite: isFavorite ?? this.isFavorite,
    );
  }
}
