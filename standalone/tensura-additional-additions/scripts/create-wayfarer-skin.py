#!/usr/bin/env python3
"""Compatibility entry point: rebuild the native-density skin from its authored art islands."""
from pathlib import Path
import subprocess
subprocess.run(["bash",str(Path(__file__).with_name("pack-wayfarer-skin.sh"))],check=True)
