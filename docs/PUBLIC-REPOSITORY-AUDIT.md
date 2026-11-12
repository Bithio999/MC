# Public repository cleanup validation

- Remove 120 tracked files belonging to the retired standalone project: source, models, textures, review material, releases and wiki pages/media.
- Remove its records from the catalogue, navigation, ability directory, search, Wiki export and rebuilt offline bundle. Verify no remaining project references or embedded paths in the current tree/bundle.
- Mark Myths discontinued at 0.36.1 in the root documentation and archived wiki pages; make AdditionalAdditions the active project. Preserve existing Myths and AdditionalAdditions mod builds.
- Verify canonical and HTML links, anchors, media and addon boundaries across 211 articles and 140 abilities; export 212 Wiki pages.
- Scan current tracked text and release archive text for credential/email/home-path patterns and the identified personal identities. Scan 19,186 historical text-sized blobs for identified personal strings and credential patterns: no matches. The confirmed exposure is Git author/committer metadata.
- Inspect PNG ancillary metadata and all ten video files’ metadata; no personal author/location fields were found. The retained images have color/display metadata.
- Check remaining release JARs against their published SHA-256 files. No gameplay source or retained JAR is changed by this cleanup; gameplay rebuilds are unnecessary.
- Prepare and verify a private recovery bundle outside the repository before anonymizing public history. Use an explicit lease when updating the public main branch to protect against intervening changes.

The final history and current-tree audit must pass using `scripts/audit-public-repository.py --history` before publication. Values matched during the audit are never printed or committed. See [privacy and history notes](PUBLIC-REPOSITORY.md) for the scope and cache/clone limitations.
