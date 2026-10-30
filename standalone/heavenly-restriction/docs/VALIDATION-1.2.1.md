# 1.2.1 — TensuraAdditionalAdditions rename

Renames the standalone display name, resource pack description, Controls category, guide title and pause button. The release artifact is `TensuraAdditionalAdditions-1.2.1.jar`. The source project remains in `standalone/heavenly-restriction`.

The `heavenlyrestriction` mod ID, registry keys, persistent data, network IDs and configuration filenames are retained. Replace the old standalone JAR; existing standalone worlds retain their content and progress. Do not install both filenames together.

Validation: Java 21 clean Gradle build and all seven catalog/search tests passed. The distribution audit verified the new name/version, dependency metadata, assets, isolation and absence of review fixtures. Comparing against the 1.2.0 release found identical JAR entry lists and byte-identical gameplay classes, data, guide catalog and textures. Only the two guide presentation classes, display metadata, language category and resource-pack description changed. No additional in-game visual review was run for this text-only rename.

SHA-256: `cefd1e547c7670751bee548c2b3014a8ecdfe2f34bc7127a8aa0c35070892cd4`
