#!/usr/bin/env python3
"""ตรวจ foundation ด้วย standard library; ไม่รัน Java, Docker หรือ external URLs."""

import argparse
import json
from pathlib import Path
import re
import sys
from urllib.parse import unquote, urlsplit


CHAPTER = re.compile(r"(?:0[1-9]|1[0-9]|20)-[a-z0-9]+(?:-[a-z0-9]+)*")
SKILLS = ("plan-chapter", "implement-chapter", "review-chapter")
REQUIRED = (
    "AGENTS.md", "README.md", "CONTRIBUTING.md", ".gitignore",
    ".editorconfig", ".gitattributes", "docs/architecture.md",
    "docs/compatibility.md", "docs/engineering-standards.md",
    "docs/reactive-opensearch.md", "docs/testing-standards.md",
    "docs/verification-standards.md", "docs/chapter-contract.md",
    "docs/reference-analysis.md", "docs/troubleshooting.md",
    "docs/foundation-verification.md",
    "docs/decisions/0001-independent-chapters.md",
    "docs/decisions/0002-async-opensearch-client.md",
    "docs/decisions/0003-test-platform-selection.md",
    "docs/templates/chapter-plan.md", "docs/templates/chapter-readme.md",
    "docs/templates/chapter-verification.md", "docs/templates/chapter-review.md",
    "scripts/check-foundation.sh", "scripts/verify-chapter.sh",
    "scripts/check_foundation.py", "scripts/test_foundation_tools.py",
    ".github/pull_request_template.md", ".github/workflows/foundation-checks.yml",
    ".github/workflows/chapter-verification.yml",
)


def discover(root):
    """ส่งเฉพาะ direct child projects; fail เมื่อพบ numbered directory ที่ไม่สมบูรณ์."""
    chapters = []
    for path in sorted(root.iterdir()):
        if not path.is_dir() or not re.match(r"^\d{2}-", path.name):
            continue
        if path.is_symlink() or not CHAPTER.fullmatch(path.name):
            raise ValueError(f"Invalid chapter directory: {path.name}")
        for file in ("pom.xml", "mvnw"):
            if not (path / file).is_file():
                raise ValueError(f"Incomplete chapter: {path.name}/{file}")
        chapters.append(path.name)
    return chapters


def prose_lines(text):
    """ไม่อ่านตัวอย่าง links ภายใน fenced code blocks เป็น documentation links."""
    marker = None
    for line in text.splitlines():
        fence = re.match(r"^\s*(`{3,}|~{3,})", line)
        if fence:
            value = fence.group(1)
            if marker is None:
                marker = value
            elif value[0] == marker[0] and len(value) >= len(marker):
                marker = None
            continue
        if marker is None:
            yield line


def link_errors(root, path):
    errors = []
    # รองรับ inline links ที่ใช้ใน repository; ไม่ใช่ full Markdown parser
    for line in prose_lines(path.read_text(encoding="utf-8")):
        for target in re.findall(r"\[[^\]]*\]\(([^)]+)\)", line):
            target = target.strip().strip("<>")
            parsed = urlsplit(target)
            if parsed.scheme or parsed.netloc or not parsed.path:
                continue
            destination = (path.parent / unquote(parsed.path)).resolve()
            if not destination.is_relative_to(root):
                errors.append(f"{path.relative_to(root)}: link escapes repository: {target}")
            elif not destination.exists():
                errors.append(f"{path.relative_to(root)}: missing link target: {target}")
    return errors


def validate(root):
    errors = []
    for name in REQUIRED:
        if not (root / name).is_file():
            errors.append(f"Missing foundation file: {name}")
    if (root / "pom.xml").exists():
        errors.append("Root pom.xml violates independent chapter policy")
    try:
        chapters = discover(root)
    except ValueError as error:
        errors.append(str(error))
        chapters = []

    readme = root / "README.md"
    if readme.is_file():
        rows = re.findall(
            r"^\| (\d{2}) \| (.*?) \| .*? \| (Planned|Implemented|Verified) \|$",
            readme.read_text(encoding="utf-8"), re.MULTILINE,
        )
        if [number for number, _, _ in rows] != [f"{i:02}" for i in range(1, 21)]:
            errors.append("Roadmap must list chapters 01–20 once, in order")
        names = []
        for number, cell, status in rows:
            match = re.search(r"(?:`|\[)((?:\d{2})-[a-z0-9-]+)(?:`|\])", cell)
            if not match or not CHAPTER.fullmatch(match.group(1)):
                errors.append(f"Invalid roadmap chapter: {number}")
                continue
            name = match.group(1)
            names.append(name)
            if not name.startswith(number + "-"):
                errors.append(f"Roadmap number mismatch: {name}")
            if status != "Planned" and name not in chapters:
                errors.append(f"{name}: {status} requires an existing chapter")
            if status == "Verified" and not (root / name / "VERIFICATION.md").is_file():
                errors.append(f"{name}: Verified requires VERIFICATION.md (content needs review)")
        for name in chapters:
            if name not in names:
                errors.append(f"Chapter missing from roadmap: {name}")

    for name in SKILLS:
        path = root / ".agents/skills" / name / "SKILL.md"
        if not path.is_file():
            errors.append(f"Missing skill: {name}")
            continue
        # Frontmatter intentionally uses two plain scalar fields; no YAML dependency.
        match = re.match(r"\A---\nname: ([a-z0-9-]+)\ndescription: ([^\n]+)\n---\n", path.read_text(encoding="utf-8"))
        if not match or match.group(1) != name or not match.group(2).strip():
            errors.append(f"Invalid skill frontmatter: {name}")

    markdown = [root / name for name in ("README.md", "AGENTS.md", "CONTRIBUTING.md")]
    markdown += list((root / "docs").rglob("*.md"))
    markdown += list((root / ".agents/skills").rglob("*.md"))
    markdown += list((root / ".github").glob("*.md"))
    for path in markdown:
        if path.is_file():
            errors.extend(link_errors(root, path))
    return errors, chapters


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--discover", action="store_true", help="Output chapter JSON for CI")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    if args.discover:
        try:
            print(json.dumps(discover(root)))
        except ValueError as error:
            print(str(error), file=sys.stderr)
            return 1
        return 0
    errors, chapters = validate(root)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"PASS: foundation files, local file links, roadmap and three skills; {len(chapters)} chapter(s)")
    print("NOT VERIFIED: Java/OpenSearch runtime, external URLs, Markdown anchors and hosted CI")
    return 0


if __name__ == "__main__":
    sys.exit(main())
