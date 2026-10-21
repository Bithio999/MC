# Ananke 0.25.0 diagnostic fixture

This is our audit probe, archived outside the compiled source tree. It exports native runtime state; its successful completion is **not** an assertion that the observed behavior is correct. The recorded output is [here](../../data/ANANKE_SKILL_PACKAGE_0.25.0.json), and the interpretation is in the [audit](../../ANANKE_SKILL_PACKAGE_0.25.0.md).

Reproduce in a disposable checkout of release commit `9b3c947a2b6106f26e9c2f05a1bdaca1668e0d11` using the project's Java 21 / Gradle setup. Copy these audit fixture files from the audit commit into that checkout first. Do not run concurrent servers in the same `run` directory.

From the repository root:

```bash
mkdir -p /tmp/ananke-package-audit
mkdir -p src/main/resources/data/wildhunt_audit/structure
cp docs/research/fixtures/ananke-0.25.0/AnankePackageProbe.java src/main/java/dev/wildhunt/AnankePackageProbe.java
cp src/main/resources/data/wildhunt/structure/empty.nbt src/main/resources/data/wildhunt_audit/structure/empty.nbt
gradle -I docs/research/fixtures/ananke-0.25.0/audit.init.gradle runGameTestServer --console=plain
```

The init script selects only namespace `wildhunt_audit`, containing one diagnostic test. Output is `/tmp/ananke-package-audit/runtime.json`. It spawns native reference bosses with AI disabled, observes stored skills and recovery, invokes the native physical-resistance callback with the stored skill OFF then ON on a disposable boss, restores OFF, and tests entity save/load. It runs in the native GameTest level and does not require a player world.

Keep these fixture files outside `src` in the normal project. They are not part of the published mod JAR. Native release dependencies and server configuration must match the audited version for meaningful comparisons.
