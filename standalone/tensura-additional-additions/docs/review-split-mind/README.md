# Split Mind runtime review

Java 21 and the normal baseline runtime dependencies are required.

Run the six native Split Mind scenarios with the full integration suite:

```sh
./gradlew clean test runGameTestServer -I docs/review-high-realm/server.init.gradle
```

Coverage includes additive learning/mastery bonuses, native Chant Annulment and Thought Acceleration, toggle cleanup/coexistence, learned-only magic filtering, type filters, invalid assignments, NBT persistence, hostile/combat targeting and ownership/range exclusions, real native magic costs/aim/cooldowns, manual input, skill seals, unmastered cast time, cancellation and mastery earned through paid casts. Ordinary activation cannot use the reserved virtual slot outside the internal invocation.

For the integrated client check, copy a **disposable** existing save into `/workspace/validation-split-mind/client/saves/Split Mind Review`. Configure the same normal runtime dependencies. The fixture refuses other save directory names. It resets this review character's skills and edits an isolated test arena; never use a personal save.

```sh
DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null ./gradlew clean runClient -I docs/review-split-mind/client.init.gradle
```

This drives real S2C/C2S menu messages, filters learned magic, assigns Spiritual Fire, checks controls at two GUI scales and selects Split Mind in Tensura's native Unique screen. It then activates Auto-Cast through the native skill lifecycle with an unequipped, unmastered Fire Ball and checks that actual automatic player/held ticks damage a hostile while the player faces away. Captures and a pass/fail marker go to `/workspace/validation-split-mind/capture`.

Clean between profiles. The opt-in client fixture and High Realm test preset must not reach a release; `scripts/verify-distribution.py` audits that exclusion.
