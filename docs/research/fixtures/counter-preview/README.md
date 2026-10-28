# Reproduce the Counter video

[Finished preview and measured values](../../../media/counter-preview/README.md).

This opt-in integrated-client fixture runs only in the isolated **Myths Validation** review world. It stages the demo player at exactly 8M effective EP, obtains mastered Unbound through its native evolution, places the original Eschaton and a native training dummy side by side, and invokes Eschaton’s actual apocalypse mode. Normal gameplay source paths do not include this directory.

Requirements: Java 21, the repository’s pinned runtime dependencies, FFmpeg with X11 capture/H.264, an X11 display at **1280×720**, Python with Pillow, and Open Sans for the motion graphics. The recording uses OpenAL Soft’s wave backend for actual game sounds.

Create `/tmp/counter-preview/alsoft.conf`:

```ini
[wave]
file = /tmp/counter-preview/game.wav
```

Run from the repository root:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 \
ALSOFT_DRIVERS=wave ALSOFT_CONF=/tmp/counter-preview/alsoft.conf \
./gradlew runClient -I docs/research/fixtures/counter-preview/preview.init.gradle
```

The fixture writes `raw.mp4`, `game.wav` and `metrics.json` into `/tmp/counter-preview`. It verifies the exact EP, native attack identity, one negated hit, one Counter hit on each target, unchanged player HP/SHP, aura payment, and an actual Infinite Regeneration healing call. The magicule loss must match the native mastered per-HP cost. The client closes after recording.

Render and assemble:

```sh
python3 docs/research/fixtures/counter-preview/render_breakdown.py \
  /tmp/counter-preview/metrics.json /tmp/counter-preview/breakdown-final.mp4
python3 docs/research/fixtures/counter-preview/assemble_preview.py \
  /tmp/counter-preview docs/media/counter-preview/counter-eschaton-8m.mp4
```

The assembly keeps the real attack and Counter, labels the 4× accelerated warning and 0.5× replay, and appends animated arithmetic, HP restoration and magicule depletion. Original quiet interface tones accompany the motion-graphics cards; combat audio comes from the game. Metric values are read from the capture, not invented by the rendering scripts.

Run a normal `./gradlew build` after the optional client run before producing a release JAR, and check that `CounterPreview` and `CounterPreviewAudio` classes are absent from it. No new mod build is required to watch this preview.
