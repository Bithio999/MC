# World Hop filming fixture

Use a **copied disposable, noise-generator world** named `World Hop Preview` in `/workspace/validation-world-hop-video/client/saves`. The fixture checks that exact folder name before seeding the test character. It replaces the inventory/skills, selects Human, sets 1M EP, equips a named sword and 37 diamonds, invokes the real World Hop activation, checks preservation and records the transition. Never run it in a player world you want to keep.

Requirements: Java 21, the pinned runtime dependencies, FFmpeg/H.264 with X11 capture, an X11 display at 1280×720, and DejaVu Sans. The profile uses the standalone mod and its required dependencies. Dismiss Minecraft’s accessibility onboarding on the first launch if it appears.

Create `/workspace/validation-world-hop-video/capture/alsoft.conf`:

```ini
[wave]
file = /workspace/validation-world-hop-video/capture/game.wav
```

From the standalone project:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=wave ALSOFT_CONF=/workspace/validation-world-hop-video/capture/alsoft.conf ./gradlew clean runClient -I docs/preview-world-hop/preview.init.gradle
```

The client waits for safe arrival and rendered terrain, records a pan in each world, verifies seeds/EP/race/inventory/progress and exits. The recorder produces `raw.mp4`, `game.wav` and `metrics.json` in the capture directory.

From the repository root:

```sh
python3 standalone/tensura-additional-additions/docs/preview-world-hop/assemble_preview.py   /workspace/validation-world-hop-video/capture   docs/media/world-hop-preview/world-hop.mp4
```

The editor adds title/result cards and measured captions, accelerates only a labelled loading interval, and omits idle render warmup. An optional `first-return.json` containing `first_return_at` can mark the first returned-world frame when filming was paused; the later idle gap is discarded rather than presented as loading time. `edit-timeline.json` records these cuts. Production sources and gameplay are not changed for the footage.
