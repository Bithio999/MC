# Hidden world library review

Use `/workspace/validation-world-library/client` with copied, disposable noise-generator saves. Copy a pre-hidden-library hopped world as `World Library Review`, its recorded previous save under the original folder name, and an unrelated control save as `Unrelated`. The fixture refuses other instance/name combinations. It edits inventory, XP, mastery and builds; performs three real jumps; briefly publishes the last world to LAN; then **deletes the active review save and its library using native level storage**. Never use a player save you want to keep.

With Java 21 and a 1280×720 X11 display:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 gradle runClient -I docs/review-world-library/client.init.gradle
```

The review exercises actual menu search/selection packets at GUI scales 2 and 3, automatic migration, B → A → B → fresh travel through one stable folder, latest placed block/custom-dimension terrain, character/EP/XP/inventory preservation, one-use selection, and LAN rejection. A temporary 128-entry test journal verifies that the native activation rejects creation before mastery/transfer while allowing return selection; the real journal is restored immediately. Final native world discovery shows only active + unrelated, and native deletion removes only the owner and its nested library.

Results and screenshots are written to `/workspace/validation-world-library/results`; success writes `passed.txt`. The review fixture is excluded from production builds. JUnit covers every directory-swap recovery boundary, rollback/failure cleanup, IDs/cap and library ownership. Native GameTests separately exercise world discovery/deletion and legacy/restored-backup migration.
