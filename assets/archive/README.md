# Archive

Superseded assets kept for reference. Lives under `assets/` (not the repo
root) to preserve the two-folder rule documented in
[assets/docs/adr/0001-android-assets-repo-split.md](../docs/adr/0001-android-assets-repo-split.md).

## `screenshots-pre-v9.3/`

The 5 store screenshots used before the v9.3.0 refresh. The current sets live
in [`assets/screenshots/`](../screenshots/) — `google-play/`, `apkpure/` and
the raw captures they're derived from.

## Old app builds

Old APKs are **not** stored in this repository — they're attached to
[GitHub Releases](https://github.com/DrummingBird1/NoveLauncher/releases),
which is where binaries belong and keeps the repo lean.

Two historical debug builds (9.1.0 and 9.2.0) were recovered from GitHub
Actions artifacts before their 14-day retention window expired and are
attached to their respective releases. Anything older than that was already
unrecoverable — CI artifacts are the only place those builds ever existed,
and they expire.

Note that 9.1.0 and 9.2.0 are **debug** builds: no release keystore existed
in this project until v9.3.0, so they are signed with a throwaway debug key
and will not install as an update over a release build. From v9.3.0 onward
every release is signed with the same release key.
