# World Select review

Use a copied, disposable client at `/workspace/validation-world-select/client`. Copy a pre-1.4.0 hopped world as `saves/World Select Review`, and copy its recorded previous world into the same saves folder under its original folder name. The fixture refuses any other instance/name. It changes test inventory, XP, skill mastery, persistent progress and places a diamond block; never use a player save you want to keep.

With Java 21 and a 1280×720 X11 display:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 gradle runClient -I docs/review-world-select/client.init.gradle
```

The native review checks legacy-history import, learning/forged-ID/current-world restrictions, the actual menu's name/seed search and selection packet, GUI scales 2 and 3, B → A → B travel, latest placed block and custom-dimension terrain, current inventory/EP/XP/progress, one-use selection, a subsequent fresh-seed jump, safe landings and LAN rejection. Results/screenshots are written to `/workspace/validation-world-select/results`. `passed.txt` marks a successful round trip.

`-PworldSelectMenuOnly` stops after the compact menu screenshot, for visual review after a layout change. This mode does not claim to test the round trip. The fixture and init script are excluded from normal builds.
