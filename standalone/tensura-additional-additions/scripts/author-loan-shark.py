#!/usr/bin/env python3
"""Import the authored Loan Shark master into Minecraft's 64px skill texture."""
from pathlib import Path
import shutil,subprocess
root=Path(__file__).resolve().parents[1]
master=root/'art/loan-shark/master.png'
texture=root/'src/main/resources/assets/heavenlyrestriction/textures/skill/loan_shark.png'
subprocess.run(['magick',str(master),'-filter','point','-resize','64x64!',str(texture)],check=True)
shutil.copy2(texture,root.parents[1]/'wiki/assets/additions/loan_shark.png')
