# Eschaton native-client review fixture

This source is opt-in and excluded from the normal JAR. It opens only the isolated **Myths Validation** singleplayer world. Do not point it at a survival save: `setup` removes staged Eschaton/seal/visual entities and creates a new encounter subject plus an invulnerable target.

```sh
JAVA_HOME=/path/to/jdk-21 GRADLE_USER_HOME=/path/to/gradle-cache \
  gradle -I docs/research/fixtures/visuals-0.30.0/preview.init.gradle runClient
```

- `/originreview setup`: spawn the actual initialized boss, NoAI, at 0,70,8; create a living target.
- `/originreview form <1..3>`: spend fatal forms through actual generic-kill sources up to the requested form. This staging bypass is not a test of funded regeneration priority.
- `/originreview cast <0..7>`: reset that mode's cooldown and invoke the real stored Demiurge skill, with its native payment and warning sequence. Exchange first lowers current MP so conversion can begin.
- `/originreview break <0..2>`: kill one actual owned Genesis Seal.
- `/originreview clear`: clear the staged cast and visual entities.

Use `/tick freeze` explicitly after opening the world, then `/tick step <ticks>` to inspect warning and impact frames. Reloading the client does not preserve the tick-freeze state. Inspect every art, both form changes, the four resource bars, native hit flashes, the golden refuge and the new items. Rebuild without this init script before publishing; verify the release contains neither `OriginReview.class` nor `VisualPreview.class`.
