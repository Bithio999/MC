#!/usr/bin/env python3
"""Fetch the exact development runtime mods, verifying published SHA-512 hashes."""
import hashlib
import json
from pathlib import Path
import urllib.request

root = Path(__file__).resolve().parents[1]
destination = root / "libs/runtime"
destination.mkdir(parents=True, exist_ok=True)
for mod in json.loads((root / "runtime-mods.lock.json").read_text()):
    target = destination / mod["file"]
    if target.exists() and hashlib.sha512(target.read_bytes()).hexdigest() == mod["sha512"]:
        print("Verified cached", mod["file"])
        continue
    with urllib.request.urlopen(mod["url"], timeout=90) as response:
        content = response.read()
    if hashlib.sha512(content).hexdigest() != mod["sha512"]:
        raise SystemExit("Checksum mismatch: " + mod["file"])
    temporary = target.with_suffix(".download")
    temporary.write_bytes(content)
    temporary.replace(target)
    print("Downloaded and verified", mod["file"])
