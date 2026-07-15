#!/usr/bin/env python3
"""Generate bundled paragraph-break metadata for the Bible reader."""

from __future__ import annotations

import concurrent.futures
import json
import re
import time
import urllib.parse
import urllib.request
from collections import defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "bible_paragraph_breaks.json"
MAX_VERSES_PER_REQUEST = 240
MAX_WORKERS = 4
USER_AGENT = "Spiritual Disciplines paragraph metadata generator"

# These match the copyrighted translations exposed in SettingsScreen. KJV is
# loaded separately from its historical pilcrows below; it must not inherit
# another edition's editorial paragraphing.
SOURCE_VERSIONS = ("ESV", "NIV", "NKJV", "NLT", "NASB1995", "LSB")
ASSET_VERSION_NAMES = {
    "ESV": "ESV",
    "NIV": "NIV",
    "NKJV": "NKJV",
    "NLT": "NLT",
    "NASB1995": "NASB",
    "LSB": "LSB",
}
BOLLS_VERSION_NAMES = {
    "ESV": "ESV",
    "NIV": "NIV",
    "KJV": "KJV",
    "NKJV": "NKJV",
    "NLT": "NLT",
    "NASB": "NASB",
    "LSB": "LSB",
}

# A stable, public-domain transcription of the Authorized Version's original
# print paragraph marks. Pinning the revision keeps the bundled table
# reproducible even if the upstream project later changes its source text.
KJV_SOURCE_URL = (
    "https://raw.githubusercontent.com/pmbauer/av-obsidian/"
    "dca358b8f0a30362dbe9e74316d83f4d49c20ebc/av.input"
)
KJV_BOOK_RE = re.compile(r"^:::SET BOOK (.+)$")
KJV_CHAPTER_RE = re.compile(r"^:::SET CHAPTER (\d+)$")
KJV_PARAGRAPH_RE = re.compile(r"^(\d+) ¶ ")

# BibleGateway is useful for a whole-canon extraction, but the official LSB
# reader is authoritative where its current paragraph markup differs. These
# photographed chapters were manually verified against read.lsbible.org.
REVIEWED_BREAK_OVERRIDES: dict[tuple[str, int, int], list[int]] = {
    ("LSB", 1, 3): [7, 8, 9, 16, 17, 20, 22],
    ("LSB", 40, 4): [5, 7, 8, 12, 17, 18, 22, 23, 24],
    ("LSB", 15, 4): [
        4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 21, 22, 23, 24
    ],
    ("LSB", 44, 4): [5, 7, 11, 13, 15, 19, 23, 27, 29, 32, 34, 36],
}

PARAGRAPH_RE = re.compile(r"<p\b[^>]*>(.*?)</p>", re.IGNORECASE | re.DOTALL)
VERSE_CLASS_RE = re.compile(
    r'''class=["']text\s+[^"']+-(\d+)-(\d+)[^"']*["']''', re.IGNORECASE
)


def fetch_text(url: str, attempts: int = 4) -> str:
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.read().decode("utf-8", errors="replace")
        except Exception:
            if attempt == attempts - 1:
                raise
            time.sleep(2**attempt)
    raise AssertionError("unreachable")


def load_canon() -> tuple[dict[int, str], dict[int, dict[int, int]]]:
    books_data = json.loads(fetch_text("https://bolls.life/get-books/ESV/"))
    books = {int(book["bookid"]): book["name"] for book in books_data}

    verses = json.loads(fetch_text("https://bolls.life/static/translations/ESV.json"))
    chapter_counts: dict[int, dict[int, int]] = defaultdict(dict)
    for verse in verses:
        book = int(verse["book"])
        chapter = int(verse["chapter"])
        number = int(verse["verse"])
        chapter_counts[book][chapter] = max(
            number, chapter_counts[book].get(chapter, 0)
        )

    if len(books) != 66 or sum(len(chapters) for chapters in chapter_counts.values()) != 1189:
        raise RuntimeError("Canonical book/chapter coverage is incomplete")
    return books, dict(chapter_counts)


def load_translation_chapter_counts(version: str) -> dict[int, dict[int, int]]:
    verses = json.loads(
        fetch_text(
            f"https://bolls.life/static/translations/"
            f"{BOLLS_VERSION_NAMES[version]}.json"
        )
    )
    chapter_counts: dict[int, dict[int, int]] = defaultdict(dict)
    for verse in verses:
        book = int(verse["book"])
        # Some KJV datasets also bundle Apocrypha after the 66-book canon.
        if book < 1 or book > 66:
            continue
        chapter = int(verse["chapter"])
        number = int(verse["verse"])
        chapter_counts[book][chapter] = max(
            number, chapter_counts[book].get(chapter, 0)
        )
    return dict(chapter_counts)


def make_ranges(
    books: dict[int, str], chapter_counts: dict[int, dict[int, int]]
) -> list[tuple[int, str, int, int]]:
    ranges: list[tuple[int, str, int, int]] = []
    for book_id, name in books.items():
        chapters = chapter_counts[book_id]
        start = 1
        running_total = 0
        for chapter in range(1, len(chapters) + 1):
            count = chapters[chapter]
            if running_total and running_total + count > MAX_VERSES_PER_REQUEST:
                ranges.append((book_id, name, start, chapter - 1))
                start = chapter
                running_total = 0
            running_total += count
        ranges.append((book_id, name, start, len(chapters)))
    return ranges


def fetch_range(
    source_version: str,
    book_id: int,
    book_name: str,
    first_chapter: int,
    last_chapter: int,
) -> tuple[str, int, int, int, dict[int, set[int]]]:
    chapter_part = (
        str(first_chapter)
        if first_chapter == last_chapter
        else f"{first_chapter}-{last_chapter}"
    )
    query = urllib.parse.urlencode(
        {"search": f"{book_name} {chapter_part}", "version": source_version}
    )
    html = fetch_text(f"https://www.biblegateway.com/passage/?{query}")

    passage_start = html.find("passage-content passage-class-0")
    if passage_start == -1:
        raise RuntimeError(f"Missing passage content for {book_name} {chapter_part}")
    passage_end = html.find("publisher-info-bottom", passage_start)
    passage = html[passage_start : passage_end if passage_end != -1 else None]

    seen_chapters = {
        int(match.group(1))
        for match in VERSE_CLASS_RE.finditer(passage)
        if first_chapter <= int(match.group(1)) <= last_chapter
    }
    expected_chapters = set(range(first_chapter, last_chapter + 1))
    if seen_chapters != expected_chapters:
        missing = sorted(expected_chapters - seen_chapters)
        raise RuntimeError(
            f"Incomplete response for {source_version} {book_name} {chapter_part}; "
            f"missing chapters {missing}"
        )

    breaks: dict[int, set[int]] = defaultdict(set)
    seen_verses: set[tuple[int, int]] = set()
    for paragraph in PARAGRAPH_RE.finditer(passage):
        paragraph_html = paragraph.group(1)
        verse = VERSE_CLASS_RE.search(paragraph_html)
        if verse is None:
            continue
        chapter = int(verse.group(1))
        number = int(verse.group(2))
        verse_key = (chapter, number)

        # A quotation or poetry line can be emitted as a second <p> beginning
        # inside a verse already present in the preceding prose paragraph.
        # That is a visual line break, not a new Scripture paragraph.
        if (
            verse_key not in seen_verses
            and first_chapter <= chapter <= last_chapter
            and number > 1
        ):
            breaks[chapter].add(number)

        for occurrence in VERSE_CLASS_RE.finditer(paragraph_html):
            seen_verses.add((int(occurrence.group(1)), int(occurrence.group(2))))

    return source_version, book_id, first_chapter, last_chapter, dict(breaks)


def load_kjv_breaks(
    chapter_counts: dict[int, dict[int, int]],
) -> dict[str, dict[str, list[int]]]:
    """Load the KJV's own historical paragraph marks, not another edition's."""
    metadata = {
        str(book_id): {str(chapter): [] for chapter in sorted(chapters)}
        for book_id, chapters in sorted(chapter_counts.items())
    }
    source = fetch_text(KJV_SOURCE_URL)
    book_id = 0
    chapter: int | None = None

    for line in source.splitlines():
        if KJV_BOOK_RE.match(line):
            book_id += 1
            chapter = None
            continue

        chapter_match = KJV_CHAPTER_RE.match(line)
        if chapter_match:
            chapter = int(chapter_match.group(1))
            if str(chapter) not in metadata.get(str(book_id), {}):
                raise RuntimeError(f"Unexpected KJV chapter {book_id}:{chapter}")
            continue

        paragraph_match = KJV_PARAGRAPH_RE.match(line)
        if paragraph_match and chapter is not None:
            verse = int(paragraph_match.group(1))
            if verse > 1:
                metadata[str(book_id)][str(chapter)].append(verse)

    if book_id != 66:
        raise RuntimeError(f"KJV source has {book_id} books instead of 66")
    return metadata


def main() -> None:
    books, chapter_counts = load_canon()
    ranges = make_ranges(books, chapter_counts)
    metadata = {
        ASSET_VERSION_NAMES[source]: {
            str(book_id): {
                str(chapter): [] for chapter in sorted(chapter_counts[book_id])
            }
            for book_id in sorted(books)
        }
        for source in SOURCE_VERSIONS
    }
    metadata["KJV"] = load_kjv_breaks(chapter_counts)

    jobs = [
        (source, book_id, name, first, last)
        for source in SOURCE_VERSIONS
        for book_id, name, first, last in ranges
    ]
    print(f"Fetching {len(jobs)} validated passage ranges...")
    completed = 0
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = [executor.submit(fetch_range, *job) for job in jobs]
        for future in concurrent.futures.as_completed(futures):
            source, book_id, first, last, breaks = future.result()
            version = ASSET_VERSION_NAMES[source]
            for chapter in range(first, last + 1):
                metadata[version][str(book_id)][str(chapter)] = sorted(
                    breaks.get(chapter, set())
                )
            completed += 1
            if completed % 50 == 0 or completed == len(jobs):
                print(f"Completed {completed}/{len(jobs)} ranges")

    for version, version_data in metadata.items():
        chapter_total = sum(len(chapters) for chapters in version_data.values())
        if chapter_total != 1189:
            raise RuntimeError(f"{version} has only {chapter_total} chapters")

    for (version, book_id, chapter), breaks in REVIEWED_BREAK_OVERRIDES.items():
        metadata[version][str(book_id)][str(chapter)] = breaks

    translation_chapter_counts = {"ESV": chapter_counts}
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        futures = {
            executor.submit(load_translation_chapter_counts, version): version
            for version in metadata
            if version != "ESV"
        }
        for future in concurrent.futures.as_completed(futures):
            translation_chapter_counts[futures[future]] = future.result()

    for version, version_data in metadata.items():
        version_counts = translation_chapter_counts[version]
        for book_id, chapters in chapter_counts.items():
            for chapter in chapters:
                last_verse = version_counts.get(book_id, {}).get(chapter)
                if last_verse is None:
                    raise RuntimeError(
                        f"{version} source is missing canonical chapter {book_id}:{chapter}"
                    )
                chapter_key = str(chapter)
                breaks = [
                    verse
                    for verse in version_data[str(book_id)][chapter_key]
                    if verse <= last_verse
                ]
                version_data[str(book_id)][chapter_key] = breaks
                if breaks != sorted(set(breaks)):
                    raise RuntimeError(
                        f"{version} {book_id}:{chapter} has unsorted or duplicate breaks"
                    )
                if any(verse <= 1 or verse > last_verse for verse in breaks):
                    raise RuntimeError(
                        f"{version} {book_id}:{chapter} has an invalid break: {breaks}"
                    )

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps(metadata, separators=(",", ":"), sort_keys=True),
        encoding="utf-8",
    )
    print(f"Wrote {OUTPUT} ({OUTPUT.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
