# World Hop review

Use a **copied, disposable world** in the prepared Neo Otherworld client, named `World Hop Validation`. The fixture refuses any other folder name. It replaces two inventory slots and changes test character state, performs real native skill activations, and briefly opens the final disposable world to LAN to test the multiplayer block. Never point it at a player save you want to keep.

With Java 21, a display, the pinned pack and the clean mapped Minecraft JAR:

```sh
gradle clean runClient -PneoOtherworldDirectory=/path/to/disposable-pack -PconnectorCleanPath=/path/to/clean-mapped-minecraft.jar -I docs/review-world-hop/client.init.gradle
```

Default: two consecutive hops. Add `-PworldHopReviewHops=1` for a final single-hop check. Use a noise-generated world to verify visibly different terrain; flat presets intentionally stay flat. `world-hop-review/results.txt` records seeds and checks; success writes `passed.txt`.

The review verifies changed seeds, cleared terrain in vanilla/modded dimension directories, safe ground, unchanged EP/race/permanent skills/mastery, a named sword, diamond count and foreign persistent data. It checks that both earlier saves still exist and that published LAN blocks rolling/acquisition/learning/transfer. The Starter Kit record must carry forward so the pack cannot overwrite inventory on first join to the new folder.

Normal builds exclude this fixture. The standard JUnit suite covers file-copy boundaries, untouched originals, failed staging cleanup, linked/escaping paths, guide content and Starter Kit history. Native `WorldHopGameTests` exercise dedicated-server gates, NBT metadata and hazardous landings. The optional exact-pack suite forwards those tests.
