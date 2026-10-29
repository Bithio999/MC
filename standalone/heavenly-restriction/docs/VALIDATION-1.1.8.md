# 1.1.8 validation

All **121 standalone native GameTests** and **30 exact-pack integration tests** passed. The full Myths counterpart passed **355 native tests** and **11 JUnit tests**. Final production builds and the independent distribution audit passed.

Ten new cases cover native Battlewill progression and existing-master backfill, normal/mastered costs and timing, forged or invalid requests, duplicate requests, portal rejection, native spatial-event vetoes, interruption and Chained support. The standalone now contains **five races, seven abilities and one non-damaging visual entity**.

The actual **Neo Otherworld 1.1.3a client** loaded all manifest projects and original overrides. A real player used the native coordinate screen: 20 blocks cost **200 aura** with the normal charge, then **100 aura** instantly at mastery; magicules stayed **700**. Reviewed screenshots are included. The normal runtime also remains validated on Tensura 2.0.1.3 / NeoForge 21.1.209.

Review code is opt-in and excluded from the normal JAR. All checks used disposable worlds; the new registry entry and learning backfill retain existing Instant Move progression.

[Full validation](../../../docs/VALIDATION-0.35.7.md) · [Instant Transmission guide](../../../wiki/pages/Instant-Transmission.md) · [Client review reproduction](review-transmission/README.md).
