# Exponentiation multiplier preview

This is an animated measured-values reference, not gameplay footage. The native GameTest exports before/after values through actual AttributeInstance getters with a learned, slotted owner. The video lists every one of the 55 covered attributes in 1.8.1, plus completed-output examples, unchanged capacities and exclusions. Artificial input values are explicitly labeled; they are not default player stats. Output examples illustrate the regression-tested square rule and caps.

To reproduce the archived 1.8.1 recording, use the bundled `measurements-1.8.1.json` and run `python3 docs/preview-exponentiation/assemble_preview.py` from the addon directory. Requires Pillow, ffmpeg and the local baseline dependency cache used for translated labels. Output: `wiki/media/exponentiation-multipliers-1.8.1.mp4` and its poster. Silent, 1280×720, H.264, 24 fps.
