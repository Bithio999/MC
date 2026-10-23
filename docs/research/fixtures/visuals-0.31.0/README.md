# Eschaton / Aion visual review

Opt-in client fixture for the isolated **Myths Validation** world. It includes the 0.30.0 Eschaton commands and stages native, mastered Aion casts. None of these commands belong in the release JAR.

```sh
gradle -I docs/research/fixtures/visuals-0.31.0/preview.init.gradle runClient
```

- `/aioncast <0..3>` learns/masteries Aion on the review player, clears the previous transient cast, and invokes its actual stored native mode. Mode 0 receives a held-key heartbeat for at most 110 ticks. Use only in the disposable review world.
- `/aionback` manually recasts the player's existing Causal Reversal checkpoint through the native skill.
- `/aionreview <0..3>` invokes Aion from the initialized NoAI Eschaton; useful for viewing a remote caster.
- `/aionreturn` recasts that boss's existing checkpoint.
- `/reviewplay <1..400>` waits two real server seconds, unfreezes the world, then freezes after the requested number of ticks. This permits recordings without a chat-input overlay. It does not change individual skill durations.

Set the camera before casting, then step a few ticks so the client applies teleport and rotation interpolation. A screenshot immediately after a teleport while tick-frozen can retain the previous view. Avoid debug-key resource reloads during scripted input; restart the client when changing rendering code or masks.

Preview subjects are NoAI/invulnerable where appropriate. Player casts use creative mode; their energy payments and encounter balance are checked separately by the native gameplay suite. This fixture is for inspecting visual timing, material blending, silhouettes, native hit feedback and primary warnings. It is not a hardware-performance or endgame-DPS benchmark.
