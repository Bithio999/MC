# Public repository privacy and maintenance

TensuraAdditionalAdditions is the active project. Tensura Myths is discontinued at 0.36.1; its source and documentation are an archive.

## Public identity

Public Git author and committer metadata uses **Tensura Addons Contributors** with the non-deliverable address `contributors@example.invalid`. Future commits in this checkout use that identity too. Do not put real names, personal email addresses, credentials, player saves or account files into the repository.

Public GitHub repository links and upstream attribution remain intact. The repository owner’s public GitHub handle is part of its URL; this cleanup does not change the owning account or its profile.

## History cleanup

The cleanup removes the retired standalone project, its assets, releases and documentation throughout reachable project history. Shared archives are checked for embedded copies. Personal author/committer identities are replaced across the history while retaining the sequence of project changes; commit IDs change.

A history rewrite cannot erase existing clones, forks, downloaded files or GitHub’s retained/cached objects. Existing contributors should make a fresh clone rather than merge an old branch that could restore removed history. GitHub Support handles removal of retained cached views if needed.

## Checks

The audit covers tracked text, release archive contents and image/video metadata. Historical text blobs and commit identities are checked as well. No hardcoded credential patterns were found during this cleanup. Automated checks cannot certify that every possible kind of personal information is absent.

Use `python3 scripts/audit-public-repository.py` to check the current tracked tree. Add `--history` to check reachable history metadata and text. The report lists paths and categories, never matched private values. Third-party license notices and public upstream credits must be preserved.

Local `.env`, private keys, launcher accounts, player saves, logs and server configuration are ignored. Review staged files before publishing. Keep private backups outside the repository.
