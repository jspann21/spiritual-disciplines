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

# These match the translations exposed in SettingsScreen. KJV intentionally
# aliases ESV in the app because the available KJV layout is verse-by-verse.
SOURCE_VERSIONS = ("ESV", "NIV", "NKJV", "NLT", "NASB1995", "LSB")
ASSET_VERSION_NAMES = {
    "ESV": "ESV",
    "NIV": "NIV",
    "NKJV": "NKJV",
    "NLT": "NLT",
    "NASB1995": "NASB",
    "LSB": "LSB",
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
    for paragraph in PARAGRAPH_RE.finditer(passage):
        verse = VERSE_CLASS_RE.search(paragraph.group(1))
        if verse is None:
            continue
        chapter = int(verse.group(1))
        number = int(verse.group(2))
        if first_chapter <= chapter <= last_chapter and number > 1:
            breaks[chapter].add(number)

    return source_version, book_id, first_chapter, last_chapter, dict(breaks)


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

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        json.dumps(metadata, separators=(",", ":"), sort_keys=True),
        encoding="utf-8",
    )
    print(f"Wrote {OUTPUT} ({OUTPUT.stat().st_size:,} bytes)")


if __name__ == "__main__":
    main()
