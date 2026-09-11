// ShonenX Source Extension: Comix (Fallback Manga)
// Executed by d4rt interpreter with dartotsu_extension_bridge runtime.

import 'dart:convert';
import 'package:dartotsu_extension_bridge/dartotsu_extension_bridge.dart';

// Extension ID matching ShonenX preset constant: kFallbackMangaExtension
const String kFallbackMangaExtension = 'comix-manga-en';

class ComixSource extends MangaSource {
  @override
  SourceMetadata get metadata => SourceMetadata(
    id: kFallbackMangaExtension, // PLACEHOLDER_FALLBACK_MANGA_ID
    name: 'Comix',
    baseUrl: 'https://comix.to',
    lang: 'en', // PLACEHOLDER_FALLBACK_MANGA_LANGUAGE
    version: '1.0.0',
    iconUrl: 'https://comix.to/favicon.ico', // PLACEHOLDER_FALLBACK_MANGA_ICON_PATH
    type: SourceType.manga,
    isNsfw: false,
  );

  static const String _popularPath = '/comic-list'; // PLACEHOLDER_FALLBACK_MANGA_POPULAR_PATH
  static const String _latestPath = '/latest-releases'; // PLACEHOLDER_FALLBACK_MANGA_LATEST_PATH
  static const String _searchPath = '/search'; // PLACEHOLDER_FALLBACK_MANGA_SEARCH_PATH

  @override
  Future<MediaPage> getPopular(int page) async {
    try {
      final url = '${metadata.baseUrl}$_popularPath?sort=popular&page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Comix popular HTTP ${response.statusCode}');
      }
      return _parseMangaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Manga (Comix) getPopular failed: $e');
    }
  }

  @override
  Future<MediaPage> getLatest(int page) async {
    try {
      final url = '${metadata.baseUrl}$_latestPath?page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Comix latest HTTP ${response.statusCode}');
      }
      return _parseMangaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Manga (Comix) getLatest failed: $e');
    }
  }

  @override
  Future<MediaPage> search(String query, int page, [FilterList? filters]) async {
    try {
      final encodedQuery = Uri.encodeComponent(query);
      final url = '${metadata.baseUrl}$_searchPath?q=$encodedQuery&page=$page';
      final response = await MClient.get(url, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Comix search HTTP ${response.statusCode}');
      }
      return _parseMangaListPage(response.body, page);
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Manga (Comix) search failed: $e');
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

      // TODO: selector for title -> PLACEHOLDER_FALLBACK_MANGA_TITLE_SELECTOR
      final title = doc.querySelector('.comic-info .comic-title')?.text.trim() ??
          doc.querySelector('h1.entry-title')?.text.trim() ?? 'Unknown Title';

      // TODO: selector for description -> PLACEHOLDER_FALLBACK_MANGA_DESCRIPTION_SELECTOR
      final description = doc.querySelector('.comic-summary p')?.text.trim() ?? '';

      // TODO: selector for cover -> PLACEHOLDER_FALLBACK_MANGA_COVER_SELECTOR
      final coverUrl = doc.querySelector('.comic-thumb img')?.attributes['src'] ??
          doc.querySelector('.entry-thumbnail img')?.attributes['data-src'];

      // TODO: selector for status -> PLACEHOLDER_FALLBACK_MANGA_STATUS_SELECTOR
      final statusText = doc.querySelector('.comic-status .val')?.text.trim() ?? 'Ongoing';

      // TODO: selector for genres -> PLACEHOLDER_FALLBACK_MANGA_GENRES_SELECTOR
      final genres = doc.querySelectorAll('.comic-genres a, .genres a')
          .map((el) => el.text.trim())
          .where((s) => s.isNotEmpty).toList();

      // TODO: selector for author -> PLACEHOLDER_FALLBACK_MANGA_AUTHOR_SELECTOR
      final author = doc.querySelector('.comic-author .val')?.text.trim();

      return MediaDetails(
        id: urlOrId,
        title: title,
        description: description,
        coverUrl: coverUrl,
        genres: genres,
        status: statusText.toLowerCase().contains('completed') ? 'Completed' : 'Ongoing',
        authorOrStudio: author,
      );
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Manga (Comix) getDetails failed: $e');
    }
  }

  @override
  Future<List<Chapter>> getChapters(String mediaId) async {
    try {
      final fullUrl = mediaId.startsWith('http') ? mediaId : '${metadata.baseUrl}$mediaId';
      final response = await MClient.get(fullUrl, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Failed to fetch chapter list: ${response.statusCode}');
      }

      final doc = parseHtml(response.body);
      final List<Chapter> chapters = [];

      // TODO: selector for chapter list items -> PLACEHOLDER_FALLBACK_MANGA_CHAPTER_LIST_SELECTOR
      final elements = doc.querySelectorAll('.chapter-list li a, .chapters-container .chapter-link');

      for (int i = 0; i < elements.length; i++) {
        final el = elements[i];
        final href = el.attributes['href'] ?? '';
        // TODO: selector for chapter title -> PLACEHOLDER_FALLBACK_MANGA_CHAPTER_TITLE_SELECTOR
        final title = el.querySelector('.name')?.text.trim() ?? el.text.trim();
        final rawNum = RegExp(r'(\d+(\.\d+)?)').firstMatch(title)?.group(1) ?? '${i + 1}';
        final chNumber = double.tryParse(rawNum) ?? (i + 1).toDouble();

        chapters.add(Chapter(
          id: href.isNotEmpty ? href : '$mediaId/ch-$chNumber',
          number: chNumber,
          title: title.isNotEmpty ? title : 'Chapter $chNumber',
          scanlator: 'Comix',
        ));
      }

      return chapters;
    } catch (e) {
      throw SourceException(code: 500, message: 'Fallback Manga (Comix) getChapters failed: $e');
    }
  }

  @override
  Future<List<PageImage>> getPages(Chapter chapter) async {
    try {
      final chUrl = chapter.id.startsWith('http') ? chapter.id : '${metadata.baseUrl}${chapter.id}';
      final response = await MClient.get(chUrl, headers: _defaultHeaders);
      if (response.statusCode != 200) {
        throw SourceException(code: response.statusCode, message: 'Failed to resolve reader: ${response.statusCode}');
      }

      final doc = parseHtml(response.body);
      final List<PageImage> pages = [];

      // TODO: selector for reader page images -> PLACEHOLDER_FALLBACK_MANGA_PAGE_IMAGES_SELECTOR
      final imgElements = doc.querySelectorAll('.reading-content img.page-image, #readerarea img');

      for (int i = 0; i < imgElements.length; i++) {
        final el = imgElements[i];
        final src = el.attributes['data-src'] ?? el.attributes['src'] ?? '';
        if (src.isNotEmpty && !src.contains('banner')) {
          pages.add(PageImage(
            index: i,
            url: src.startsWith('//') ? 'https:$src' : src,
            headers: {'Referer': metadata.baseUrl, 'User-Agent': _defaultHeaders['User-Agent']!},
          ));
        }
      }

      if (pages.isEmpty) {
        throw SourceException(code: 404, message: 'Fallback Manga (Comix) found 0 readable pages for chapter ${chapter.number}');
      }
      return pages;
    } catch (e) {
      throw SourceException(code: 502, message: 'Fallback Manga (Comix) getPages failed: $e');
    }
  }

  MediaPage _parseMangaListPage(String html, int page) {
    final doc = parseHtml(html);
    final List<MediaItem> items = [];

    // TODO: selector for catalog item cards -> PLACEHOLDER_FALLBACK_MANGA_CARD_SELECTOR
    final cards = doc.querySelectorAll('.comic-list-grid .comic-card, .list-series .series-item');

    for (final card in cards) {
      // TODO: selector for card title -> PLACEHOLDER_FALLBACK_MANGA_CARD_TITLE_SELECTOR
      final title = card.querySelector('.title, h3.title a')?.text.trim() ?? '';
      // TODO: selector for card link -> PLACEHOLDER_FALLBACK_MANGA_CARD_LINK_SELECTOR
      final href = card.querySelector('a')?.attributes['href'] ?? '';
      // TODO: selector for card cover -> PLACEHOLDER_FALLBACK_MANGA_CARD_COVER_SELECTOR
      final cover = card.querySelector('img')?.attributes['src'] ?? card.querySelector('img')?.attributes['data-src'];

      if (title.isNotEmpty && href.isNotEmpty) {
        items.add(MediaItem(id: href, title: title, coverUrl: cover, format: 'Manga'));
      }
    }

    // TODO: selector for next page pagination -> PLACEHOLDER_FALLBACK_MANGA_PAGINATION_SELECTOR
    final hasNext = doc.querySelector('.pagination .next, a.next-page') != null;
    return MediaPage(list: items, hasNextPage: hasNext || items.length >= 20, page: page);
  }

  static const Map<String, String> _defaultHeaders = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  };
}