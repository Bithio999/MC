# 1.6.2 — Fusion movement and targeting

The retained Fusion body previously had server-side `noPhysics`, but that vanilla field is not synchronized to clients. Client collision could still push the controlling player. The embedded participant is now explicitly non-physical on the client, cannot be pushed or collide through direct callbacks, and has its prior client flag restored after separation or a level change.

Jade's entity ray scans a spatial query without requiring `isPickable` and immediately selects an entity containing the ray origin. Tensura's Analytical Appraisal also scans nearby living entities. Fusion now excludes only its embedded participant from both untyped and typed spatial queries, including limited queries before they consume result slots. Direct entity lookup remains available for the fused model and recovery. The pilot and unrelated targets remain queryable; the original body returns to queries after separation.

## Verification

- Clean production build, **170 native GameTests** and **30 unit tests** passed. The new native regression covers typed/untyped/limited queries, direct push callbacks even with `noPhysics` cleared, retained UUID lookup, and restoration after separation.
- Actual client review with Tensura 2.0.1.3, NeoForge 21.1.209 and Jade from Neo Otherworld 1.1.3a: native forward input and release settled normally before and during subordinate Fusion. Both ended below 0.001 horizontal velocity after thirty released ticks. Baseline travel/coast: 0.8323 / 0.2405 blocks; fused travel/coast: 1.0404 / 0.3043 blocks, reflecting the subordinate's inherited movement speed.
- Jade's actual entity-ray method and the exact native Analytical Appraisal query ignored the embedded body at four headings; both still selected a separate target ahead. Client `noPhysics`, retained direct ID lookup, and restoration after separation passed.
- The client review uses a real connected player and an owned Iron Golem. It does not constitute a full Neo Otherworld runtime test or a two-human-client test.
- Distribution audit passed with the new mixins included and review classes excluded. The rebuilt wiki passed its link, media, anchor and addon-boundary checks.

[Client review reproduction](review-fusion-control/README.md) · [Previous validation](VALIDATION-1.6.1.md).
