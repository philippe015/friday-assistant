#!/usr/bin/env python3
"""Check all 64-bit Android ELF .so payloads in APK or AAB for 16 KiB PT_LOAD alignment.

This checks ELF program headers, NOT whether the code works on a 16 KiB phone.
The Play Console, bundletool, and real-device tests are still required.
"""
import struct
import sys
import zipfile
from pathlib import Path

MIN_ALIGN = 16 * 1024
PT_LOAD = 1

def inspect_elf(data: bytes, name: str) -> list[str]:
    errors = []
    if len(data) < 64 or data[:4] != b"\x7fELF":
        return [f"{name}: not an ELF shared object"]
    cls, endian = data[4], data[5]
    if cls != 2 or endian not in (1, 2):
        return [f"{name}: expected ELF64 (little/big endian)"]
    order = "<" if endian == 1 else ">"
    phoff = struct.unpack_from(order + "Q", data, 32)[0]
    phentsize = struct.unpack_from(order + "H", data, 54)[0]
    phnum = struct.unpack_from(order + "H", data, 56)[0]
    if phentsize < 56 or phnum <= 0 or phoff + phentsize * phnum > len(data):
        return [f"{name}: invalid program header table"]
    load_segments = 0
    for i in range(phnum):
        at = phoff + i * phentsize
        p_type = struct.unpack_from(order + "I", data, at)[0]
        if p_type != PT_LOAD:
            continue
        load_segments += 1
        p_offset = struct.unpack_from(order + "Q", data, at + 8)[0]
        p_vaddr = struct.unpack_from(order + "Q", data, at + 16)[0]
        p_align = struct.unpack_from(order + "Q", data, at + 48)[0]
        if p_align < MIN_ALIGN or p_offset % MIN_ALIGN != p_vaddr % MIN_ALIGN:
            errors.append(f"{name}: PT_LOAD #{i} offset={p_offset:#x} "
                          f"vaddr={p_vaddr:#x} align={p_align:#x}; "
                          "requires 0x4000 alignment")
    if not load_segments:
        errors.append(f"{name}: no PT_LOAD segments")
    return errors

def check_archive(path: Path) -> int:
    try:
        with zipfile.ZipFile(path) as archive:
            members = [name for name in archive.namelist()
                       if name.endswith(".so") and not name.startswith("__MACOSX/")]
            if not members:
                print("ERROR: No .so found; cannot validate native Android support", file=sys.stderr)
                return 1
            problems = []
            for name in members:
                problems.extend(inspect_elf(archive.read(name), name))
    except (OSError, zipfile.BadZipFile) as exc:
        print(f"ERROR: Cannot read APK/AAB: {type(exc).__name__}", file=sys.stderr)
        return 1
    if problems:
        print("\n".join("FAIL " + problem for problem in problems), file=sys.stderr)
        return 1
    print(f"PASS: {len(members)} embedded ELF shared libraries meet 16 KiB PT_LOAD alignment.")
    print("Warning: runtime and bundle ZIP alignment must still be checked separately.")
    return 0

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: check_play_16kb_elf.py app-release.aab|app-debug.apk", file=sys.stderr)
        sys.exit(2)
    sys.exit(check_archive(Path(sys.argv[1])))
