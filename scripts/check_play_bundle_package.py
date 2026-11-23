#!/usr/bin/env python3
"""Sanity-check the Google Play App Bundle manifest package and component classes.

The manifest inside an AAB is protobuf-encoded by Android Gradle Plugin. The
identifiers are stored as UTF-8 strings in its manifest payload. This is an
early publishing guard, not a replacement for bundletool / Google Play review.
"""
import sys
import zipfile
from pathlib import Path


def check(path: Path, package: str) -> int:
    if package != "jarvis.myapp":
        print("FAIL: this Play workflow is reserved for jarvis.myapp", file=sys.stderr)
        return 1
    try:
        with zipfile.ZipFile(path) as z:
            manifest = z.read("base/manifest/AndroidManifest.xml")
    except (OSError, KeyError, zipfile.BadZipFile) as e:
        print(f"FAIL: missing AAB manifest ({type(e).__name__})", file=sys.stderr)
        return 1
    if package.encode("utf-8") not in manifest:
        print("FAIL: AAB does not declare the expected Play package", file=sys.stderr)
        return 1
    # AGP must retain the actual code's Java package despite the Play ID
    # being a different string from the Gradle namespace.
    for component in (b"fr.friday.assistant.MainActivity", b"fr.friday.assistant.FridayService"):
        if component not in manifest:
            print("FAIL: AAB manifest missing full Android component name", file=sys.stderr)
            return 1
    print("PASS: Play App Bundle has jarvis.myapp and resolved app component names.")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: check_play_bundle_package.py app-release.aab jarvis.myapp", file=sys.stderr)
        sys.exit(2)
    sys.exit(check(Path(sys.argv[1]), sys.argv[2]))
