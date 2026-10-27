# Chained native-player visual fixture

Opt-in source for the disposable **Myths Validation** world. It is excluded from normal builds. Do not run this in a survival save: `setup` clears the review player's skills and effects, changes their race/resources and removes staged Myth bosses and visuals.

```sh
gradle -I docs/research/fixtures/chained-0.32.0/preview.init.gradle runClient
```

- `/chainedreview setup` gives the actual Fettered Human race and Chained to the real review player.
- `/tick freeze`, `/chainedreview cast`, then `/tick step 3` stages the actual player dash and changes the camera for a wide view. The server prints actual distance and aura payment before moving the camera.
- The native race/evolution and skill menus remain available. No preview renderer or replacement combat implementation is used.

Close the client and run a clean normal build before packaging. Verify `ChainedReview.class` is absent from the release JAR.
