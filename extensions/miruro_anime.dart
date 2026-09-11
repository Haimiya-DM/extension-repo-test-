// ShonenX Source Extension: Miruro (Primary Anime)
// Executed by d4rt interpreter with dartotsu_extension_bridge runtime.

import 'dart:convert';
import 'package:dartotsu_extension_bridge/dartotsu_extension_bridge.dart';

// Extension ID matching ShonenX preset constant: kPrimaryAnimeExtension
const String kPrimaryAnimeExtension = 'miruro-anime-en';

class MiruroSource extends AnimeSource {
  @override
  SourceMetadata get metadata => SourceMetadata(
    id: kPrimaryAnimeExtension, // PLACEHOLDER_PRIMARY_ANIME_ID
    name: 'Miruro',
    baseUrl: 'https://miruro.to',
    lang: 'en', // PLACEHOLDER_PRIMARY_ANIME_LANGUAGE
    version: '1.0.0',
    iconUrl: 'https://miruro.to/favicon.ico', // PLACEHOLDER_PRIMARY_ANIME_ICON_PATH
    type: SourceType.anime,
    isNsfw: false,
  );

  static const String _popularPath = '/anime/popular'; // PLACEHOLDER_PRIMARY_ANIME_POPULAR_PATH
  static const String _latestPath = '/anime/recent'; // PLACEHOLDER_PRIMARY_ANIME_LATEST_PATH
  static const String _searchPath = '/search'; // PLACEHOLDER_PRIMARY_ANIME_SEARCH_PATH

  @override
  Future<MediaPage> getPopular(int page) async {
    try {
      final url = '${metadata.baseUrl}$_popularPath?page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Miruro popular HTTP ${response.statusCode}');
      }
      return _parseMediaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Primary Anime (Miruro) getPopular failed: $e');
    }
  }

  @override
  Future<MediaPage> getLatest(int page) async {
    try {
      final url = '${metadata.baseUrl}$_latestPath?page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Miruro latest HTTP ${response.statusCode}');
      }
      return _parseMediaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Primary Anime (Miruro) getLatest failed: $e');
    }
  }

  @override
  Future<MediaPage> search(String query, int page, [FilterList? filters]) async {
    try {
      final encodedQuery = Uri.encodeComponent(query);
      final url = '${metadata.baseUrl}$_searchPath?q=$encodedQuery&page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Miruro search HTTP ${response.statusCode}');
      }
      return _parseMediaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Primary Anime (Miruro) search failed: $e');
    }
  }

  @override
  Future<MediaDetails> getDetails(String urlOrId) async {
    try {
      final fullUrl = urlOrId.startsWith('http') ? urlOrId : '${metadata.baseUrl}$urlOrId';
      final response = await MClient.get(fullUrl, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Failed to fetch details: ${response.statusCode}');
      }

      final doc = parseHtml(response.body);

      // TODO: selector for title -> PLACEHOLDER_PRIMARY_ANIME_TITLE_SELECTOR
      final title = doc.querySelector('.media-header h1.media-title')?.text.trim() ??
          doc.querySelector('h1.title')?.text.trim() ?? 'Unknown Title';

      // TODO: selector for description -> PLACEHOLDER_PRIMARY_ANIME_DESCRIPTION_SELECTOR
      final description = doc.querySelector('.media-description .content')?.text.trim() ??
          doc.querySelector('.description')?.text.trim() ?? '';

      // TODO: selector for cover -> PLACEHOLDER_PRIMARY_ANIME_COVER_SELECTOR
      final coverUrl = doc.querySelector('.media-poster img')?.attributes['src'] ??
          doc.querySelector('.poster img')?.attributes['data-src'];

      // TODO: selector for status -> PLACEHOLDER_PRIMARY_ANIME_STATUS_SELECTOR
      final statusText = doc.querySelector('.meta-item.status .value')?.text.trim() ?? 'Ongoing';

      // TODO: selector for genres -> PLACEHOLDER_PRIMARY_ANIME_GENRES_SELECTOR
      final genres = doc.querySelectorAll('.genre-tags a, .genres a')
          .map((el) => el.text.trim())
          .where((s) => s.isNotEmpty).toList();

      // TODO: selector for studio -> PLACEHOLDER_PRIMARY_ANIME_STUDIO_SELECTOR
      final studio = doc.querySelector('.meta-item.studio .value')?.text.trim();

      return MediaDetails(
        id: urlOrId,
        title: title,
        description: description,
        coverUrl: coverUrl,
        genres: genres,
        status: statusText.contains('Completed') ? 'Completed' : 'Ongoing',
        authorOrStudio: studio,
      );
    } catch (e) {
      throw SourceException(code: 500, message: 'Primary Anime (Miruro) getDetails failed: $e');
    }
  }

  @override
  Future<List<Episode>> getEpisodes(String mediaId) async {
    try {
      final fullUrl = mediaId.startsWith('http') ? mediaId : '${metadata.baseUrl}$mediaId';
      final response = await MClient.get(fullUrl, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Failed to fetch episodes: ${response.statusCode}');
      }

      final doc = parseHtml(response.body);
      final List<Episode> episodes = [];

      // TODO: selector for episode list items -> PLACEHOLDER_PRIMARY_ANIME_EPISODE_LIST_SELECTOR
      final elements = doc.querySelectorAll('.episodes-list .episode-card, .episode-item a');

      for (int i = 0; i < elements.length; i++) {
        final el = elements[i];
        final href = el.attributes['href'] ?? '';
        // TODO: selector for episode number/title -> PLACEHOLDER_PRIMARY_ANIME_EPISODE_TITLE_SELECTOR
        final title = el.querySelector('.ep-title')?.text.trim() ?? 'Episode ${i + 1}';
        final epNumber = double.tryParse(el.attributes['data-number'] ?? '${i + 1}') ?? (i + 1).toDouble();
        final date = el.querySelector('.ep-date')?.text.trim();

        episodes.add(Episode(
          id: href.isNotEmpty ? href : '$mediaId/ep-${i + 1}',
          number: epNumber,
          title: title,
          dateUpload: date,
          scanlator: 'Miruro',
        ));
      }

      return episodes.isEmpty ? [Episode(id: mediaId, number: 1.0, title: 'Episode 1')] : episodes;
    } catch (e) {
      throw SourceException(code: 500, message: 'Primary Anime (Miruro) getEpisodes failed: $e');
    }
  }

  @override
  Future<List<Video>> getStreams(Episode episode) async {
    try {
      final epUrl = episode.id.startsWith('http') ? episode.id : '${metadata.baseUrl}${episode.id}';
      final response = await MClient.get(epUrl, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Failed to resolve stream: ${response.statusCode}');
      }

      final doc = parseHtml(response.body);

      // TODO: selector for video embed/iframe -> PLACEHOLDER_PRIMARY_ANIME_STREAM_EMBED_SELECTOR
      final iframeSrc = doc.querySelector('iframe.player-iframe')?.attributes['src'] ??
          doc.querySelector('#player iframe')?.attributes['src'] ?? '';

      final List<Video> videos = [];

      if (iframeSrc.isNotEmpty) {
        videos.add(Video(
          url: iframeSrc.contains('m3u8') ? iframeSrc : '$iframeSrc/master.m3u8',
          quality: '1080p (Source HLS)',
          originalUrl: epUrl,
          headers: {
            'Referer': metadata.baseUrl,
            'User-Agent': _defaultHeaders['User-Agent']!,
            'Origin': metadata.baseUrl,
          },
          subtitles: [Track(file: '$iframeSrc/sub-en.vtt', label: 'English')],
        ));
        videos.add(Video(
          url: iframeSrc.contains('m3u8') ? iframeSrc.replaceAll('1080', '720') : '$iframeSrc/720p.m3u8',
          quality: '720p',
          originalUrl: epUrl,
          headers: {'Referer': metadata.baseUrl, 'User-Agent': _defaultHeaders['User-Agent']!},
        ));
      }

      if (videos.isEmpty) {
        throw SourceException(code: 404, message: 'No playable streams found for episode ${episode.number}');
      }
      return videos;
    } catch (e) {
      // Signals error so ShonenX immediately cascades to Fallback Anime (AnikotoTV)
      throw SourceException(code: 502, message: 'Primary Anime stream resolution failed: $e');
    }
  }

  MediaPage _parseMediaListPage(String html, int page) {
    final doc = parseHtml(html);
    final List<MediaItem> items = [];

    // TODO: selector for catalog item cards -> PLACEHOLDER_PRIMARY_ANIME_CARD_SELECTOR
    final cards = doc.querySelectorAll('.anime-grid .anime-card, .catalog-list .item');

    for (final card in cards) {
      // TODO: selector for card title -> PLACEHOLDER_PRIMARY_ANIME_CARD_TITLE_SELECTOR
      final title = card.querySelector('.anime-title, .title a')?.text.trim() ?? '';
      // TODO: selector for card link -> PLACEHOLDER_PRIMARY_ANIME_CARD_LINK_SELECTOR
      final href = card.querySelector('a')?.attributes['href'] ?? '';
      // TODO: selector for card cover -> PLACEHOLDER_PRIMARY_ANIME_CARD_COVER_SELECTOR
      final cover = card.querySelector('img')?.attributes['src'] ?? card.querySelector('img')?.attributes['data-src'];

      if (title.isNotEmpty && href.isNotEmpty) {
        items.add(MediaItem(id: href, title: title, coverUrl: cover, format: 'Anime'));
      }
    }

    // TODO: selector for next page pagination -> PLACEHOLDER_PRIMARY_ANIME_PAGINATION_SELECTOR
    final hasNext = doc.querySelector('.pagination .next:not(.disabled), a[rel="next"]') != null;
    return MediaPage(list: items, hasNextPage: hasNext || items.length >= 20, page: page);
  }

  static const Map<String, String> _defaultHeaders = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  };
}