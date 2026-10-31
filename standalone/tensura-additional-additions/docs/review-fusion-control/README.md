# Fusion movement and targeting review

This opt-in fixture requires a disposable copied save named **Fusion Preview** in `/workspace/validation-fusion/client/saves`. It checks that name before editing, clears nearby golems, resets the review player's skills/effects, builds a stone platform, and saves the altered world. Never use a player save you want to keep.

With Java 21, an X11 display and the Jade jar from Neo Otherworld 1.1.3a at the path in `client.init.gradle`:

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ./gradlew runClient -I docs/review-fusion-control/client.init.gradle
```

The actual client presses movement for five ticks and releases for thirty, both normally and while controlling a fused subordinate. It checks that movement settles, verifies client `noPhysics`, and calls Jade's actual permissive entity-ray method and the exact native query used by Analytical Appraisal at four headings. A separate target ahead must remain detectable. Direct ID lookup must retain the rendering body, and separation must restore query visibility and physics.

Results are written to `/workspace/validation-fusion/control-review`. A failure prevents the pass result. This fixture is excluded from normal builds and the distribution audit verifies that exclusion. This is a baseline Tensura + Jade runtime review, not a full modpack playtest.
