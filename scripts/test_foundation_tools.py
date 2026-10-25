#!/usr/bin/env python3
"""Regression cases สำหรับ validation logic และ chapter command; ใช้ temporary fixtures."""

from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

from check_foundation import discover, link_errors


class FoundationToolsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()

    def test_zero_chapters_and_incomplete_project(self):
        self.assertEqual(discover(self.root), [])
        chapter = self.root / "01-example"
        chapter.mkdir()
        with self.assertRaisesRegex(ValueError, "Incomplete"):
            discover(self.root)
        (chapter / "pom.xml").touch()
        (chapter / "mvnw").touch()
        self.assertEqual(discover(self.root), ["01-example"])

    def test_symlink_chapter_rejected(self):
        target = self.root / "actual"
        target.mkdir()
        (self.root / "01-linked").symlink_to(target, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "Invalid chapter"):
            discover(self.root)

    def test_file_links_missing_escape_and_fenced_examples(self):
        page = self.root / "README.md"
        (self.root / "valid.md").touch()
        page.write_text(
            "[ok](valid.md#anchor)\n[missing](missing.md)\n"
            "[escape](../outside.md)\n[web](https://example.com)\n"
            "```md\n[example](not-a-file.md)\n```\n", encoding="utf-8",
        )
        errors = link_errors(self.root, page)
        self.assertEqual(len(errors), 2)
        self.assertTrue(any("missing link" in error for error in errors))
        self.assertTrue(any("escapes" in error for error in errors))

    def test_verify_command_validates_scope_and_preserves_exit_code(self):
        scripts = self.root / "scripts"
        scripts.mkdir()
        command = scripts / "verify-chapter.sh"
        shutil.copyfile(Path(__file__).with_name("verify-chapter.sh"), command)
        chapter = self.root / "01-example"
        chapter.mkdir()
        (chapter / "pom.xml").touch()
        (chapter / "mvnw").write_text(
            '[[ "$PWD" == *"/01-example" && "$#" == 2 && "$1" == clean && "$2" == verify ]] || exit 90\nexit 7\n',
            encoding="utf-8",
        )
        result = subprocess.run(["bash", str(command), "01-example"], capture_output=True)
        self.assertEqual(result.returncode, 7)
        for args in ([], ["../01-example"], ["21-example"], ["01-missing"], ["01-example", "-DskipTests"]):
            with self.subTest(args=args):
                result = subprocess.run(["bash", str(command), *args], capture_output=True)
                self.assertEqual(result.returncode, 2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
