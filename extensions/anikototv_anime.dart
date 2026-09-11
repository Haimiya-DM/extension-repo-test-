// ShonenX Source Extension: AnikotoTV (Fallback Anime)
// Executed by d4rt interpreter with dartotsu_extension_bridge runtime.

import 'dart:convert';
import 'package:dartotsu_extension_bridge/dartotsu_extension_bridge.dart';

// Extension ID matching ShonenX preset constant: kFallbackAnimeExtension
const String kFallbackAnimeExtension = 'anikototv-anime-en';

class AnikotoTVSource extends AnimeSource {
  @override
  SourceMetadata get metadata => SourceMetadata(
    id: kFallbackAnimeExtension, // PLACEHOLDER_FALLBACK_ANIME_ID
    name: 'AnikotoTV',
    baseUrl: 'https://anikototv.to',
    lang: 'en', // PLACEHOLDER_FALLBACK_ANIME_LANGUAGE
    version: '1.0.0',
    iconUrl: 'https://anikototv.to/favicon.ico', // PLACEHOLDER_FALLBACK_ANIME_ICON_PATH
    type: SourceType.anime,
    isNsfw: false,
  );

  static const String _popularPath = '/trending'; // PLACEHOLDER_FALLBACK_ANIME_POPULAR_PATH
  static const String _latestPath = '/latest-episodes'; // PLACEHOLDER_FALLBACK_ANIME_LATEST_PATH
  static const String _searchPath = '/search'; // PLACEHOLDER_FALLBACK_ANIME_SEARCH_PATH

  @override
  Future<MediaPage> getPopular(int page) async {
    try {
      final url = '${metadata.baseUrl}$_popularPath?page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'AnikotoTV popular HTTP ${response.statusCode}');
      }
      return _parseMediaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Anime (AnikotoTV) getPopular failed: $e');
    }
  }

  @override
  Future<MediaPage> getLatest(int page) async {
    try {
      final url = '${metadata.baseUrl}$_latestPath?page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'AnikotoTV latest HTTP ${response.statusCode}');
      }
      return _parseMediaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Anime (AnikotoTV) getLatest failed: $e');
    }
  }

  @override
  Future<MediaPage> search(String query, int page, [FilterList? filters]) async {
    try {
      final encodedQuery = Uri.encodeComponent(query);
      final url = '${metadata.baseUrl}$_searchPath?keyword=$encodedQuery&page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'AnikotoTV search HTTP ${response.statusCode}');
      }
      return _parseMediaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Anime (AnikotoTV) search failed: $e');
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

      // TODO: selector for title -> PLACEHOLDER_FALLBACK_ANIME_TITLE_SELECTOR
      final title = doc.querySelector('.anisc-detail .film-name')?.text.trim() ??
          doc.querySelector('h2.film-name')?.text.trim() ?? 'Unknown Title';

      // TODO: selector for description -> PLACEHOLDER_FALLBACK_ANIME_DESCRIPTION_SELECTOR
      final description = doc.querySelector('.film-description .text')?.text.trim() ?? '';

      // TODO: selector for cover -> PLACEHOLDER_FALLBACK_ANIME_COVER_SELECTOR
      final coverUrl = doc.querySelector('.film-poster img.film-poster-img')?.attributes['src'] ??
          doc.querySelector('.film-poster img')?.attributes['data-src'];

      // TODO: selector for status -> PLACEHOLDER_FALLBACK_ANIME_STATUS_SELECTOR
      final statusText = doc.querySelector('.item-title:contains("Status") + .item-content')?.text.trim() ?? 'Ongoing';

      // TODO: selector for genres -> PLACEHOLDER_FALLBACK_ANIME_GENRES_SELECTOR
      final genres = doc.querySelectorAll('.item-list:contains("Genres") a, .genres a')
          .map((el) => el.text.trim())
          .where((s) => s.isNotEmpty).toList();

      // TODO: selector for studio -> PLACEHOLDER_FALLBACK_ANIME_STUDIO_SELECTOR
      final studio = doc.querySelector('.item-title:contains("Studios") + .item-content a')?.text.trim();

      return MediaDetails(
        id: urlOrId,
        title: title,
        description: description,
        coverUrl: coverUrl,
        genres: genres,
        status: statusText.toLowerCase().contains('completed') ? 'Completed' : 'Ongoing',
        authorOrStudio: studio,
      );
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Anime (AnikotoTV) getDetails failed: $e');
    }
  }

  @override
  Future<List<Episode>> getEpisodes(String mediaId) async {
    try {
      final fullUrl = mediaId.startsWith('http') ? mediaId : '${metadata.baseUrl}$mediaId';
      final response = await MClient.get(fullUrl, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Failed to fetch episode list: ${response.statusCode}');
      }

      final doc = parseHtml(response.body);
      final List<Episode> episodes = [];

      // TODO: selector for episode list items -> PLACEHOLDER_FALLBACK_ANIME_EPISODE_LIST_SELECTOR
      final elements = doc.querySelectorAll('.ss-list a.ssl-item, .episodes-ul li a');

      for (int i = 0; i < elements.length; i++) {
        final el = elements[i];
        final href = el.attributes['href'] ?? '';
        // TODO: selector for episode number/title -> PLACEHOLDER_FALLBACK_ANIME_EPISODE_TITLE_SELECTOR
        final title = el.querySelector('.ep-name')?.text.trim() ?? 'Episode ${i + 1}';
        final epNumber = double.tryParse(el.attributes['data-number'] ?? '${i + 1}') ?? (i + 1).toDouble();

        episodes.add(Episode(
          id: href.isNotEmpty ? href : '$mediaId?ep=${i + 1}',
          number: epNumber,
          title: title,
          scanlator: 'AnikotoTV',
        ));
      }

      return episodes.isEmpty ? [Episode(id: mediaId, number: 1.0, title: 'Episode 1')] : episodes;
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Anime (AnikotoTV) getEpisodes failed: $e');
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

      // TODO: selector for video embed/iframe -> PLACEHOLDER_FALLBACK_ANIME_STREAM_EMBED_SELECTOR
      final iframeSrc = doc.querySelector('#iframe-to-folder iframe')?.attributes['src'] ??
          doc.querySelector('iframe.film-iframe')?.attributes['src'] ?? '';

      final List<Video> videos = [];

      if (iframeSrc.isNotEmpty) {
        videos.add(Video(
          url: iframeSrc.endsWith('.m3u8') ? iframeSrc : '$iframeSrc/playlist.m3u8',
          quality: '1080p (Anikoto CDN)',
          originalUrl: epUrl,
          headers: {'Referer': metadata.baseUrl, 'User-Agent': _defaultHeaders['User-Agent']!},
          subtitles: [Track(file: '$iframeSrc/subtitles/en.vtt', label: 'English')],
        ));
      }

      if (videos.isEmpty) {
        throw SourceException(code: 404, message: 'Fallback Anime (AnikotoTV) found no stream for ep ${episode.number}');
      }

      return videos;
    } catch (e) {
      throw SourceException(code: 502, message: 'Fallback Anime (AnikotoTV) stream resolution failed: $e');
    }
  }

  MediaPage _parseMediaListPage(String html, int page) {
    final doc = parseHtml(html);
    final List<MediaItem> items = [];

    // TODO: selector for catalog item cards -> PLACEHOLDER_FALLBACK_ANIME_CARD_SELECTOR
    final cards = doc.querySelectorAll('.flw-item, .film_list-wrap .film-item');

    for (final card in cards) {
      // TODO: selector for card title -> PLACEHOLDER_FALLBACK_ANIME_CARD_TITLE_SELECTOR
      final title = card.querySelector('.film-name a, .dynamic-name')?.text.trim() ?? '';
      // TODO: selector for card link -> PLACEHOLDER_FALLBACK_ANIME_CARD_LINK_SELECTOR
      final href = card.querySelector('.film-name a')?.attributes['href'] ?? '';
      // TODO: selector for card cover -> PLACEHOLDER_FALLBACK_ANIME_CARD_COVER_SELECTOR
      final cover = card.querySelector('.film-poster-img')?.attributes['src'] ??
          card.querySelector('.film-poster img')?.attributes['data-src'];

      if (title.isNotEmpty && href.isNotEmpty) {
        items.add(MediaItem(id: href, title: title, coverUrl: cover, format: 'Anime'));
      }
    }

    // TODO: selector for next page pagination -> PLACEHOLDER_FALLBACK_ANIME_PAGINATION_SELECTOR
    final hasNext = doc.querySelector('.pagination .page-item.active + .page-item:not(.disabled)') != null;
    return MediaPage(list: items, hasNextPage: hasNext || items.length >= 24, page: page);
  }

  static const Map<String, String> _defaultHeaders = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  };
}