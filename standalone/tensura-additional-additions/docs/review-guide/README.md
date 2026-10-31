# In-game guide review

The optional client fixture opens the actual guide and checks catalog loading, title/body/category search, empty results, selection, back/forward history, article scrolling, scrollbar dragging, a 640×480 window and restoration of the previous screen. Screenshots are unedited game captures. It grants no items or skills and edits no player stats.

With Java 21 and a display, run `gradle runClient -I docs/review-guide/client.init.gradle` from the full project. It reviews the guide from the title screen and uses an isolated client directory. Success writes `guide-review/passed.txt` and screenshots there.

The standalone fixture uses the exact Neo Otherworld client profile and the existing disposable `Schema Validation` world. Run `gradle clean runClient -PneoOtherworldDirectory=/path/to/disposable-pack -PconnectorCleanPath=/path/to/clean-mapped-minecraft.jar -I docs/review-guide/client.init.gradle`. It also checks the real F8 mapping, pause button, native status button and their parent screens. Use only a disposable world; normal world ticking/saving still occurs outside the paused guide. A clean build avoids stale classes when switching review profiles.

Optional review sources are excluded from production builds. The generator is `python3 scripts/build-ingame-guide.py`; it exports the curated reference data and validates progression rows and links. Search/catalog tests run with the normal Gradle build.
