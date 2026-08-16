# Archive

Old screenshots and old app builds, kept for reference. Lives under `assets/`
(not the repo root) to preserve the two-folder rule documented in
[assets/docs/adr/0001-android-assets-repo-split.md](docs/adr/0001-android-assets-repo-split.md).

## `screenshots-pre-v9.3/`

The 5 store screenshots used before the v9.3.0 refresh (`assets/screenshots/`
now holds the current 10).

## `old-releases/`

Two old **debug** builds, recovered from GitHub Actions artifacts (14-day
retention — anything older had already expired and could not be recovered):

- `NoveLauncher-9.1.0-debug.apk` — from commit `c7342fe`
- `NoveLauncher-9.2.0-debug.apk` — from commit `4a8dad1`

**Caveats:**
- These are **debug** builds, not release-signed — no release keystore
  existed in this project until v9.3.0 (2026-08-16). They install fine on
  their own but won't update cleanly over a differently-signed build (see
  Pitfall #21 in [CLAUDE.md](../../CLAUDE.md) for why debug signatures matter).
- No versions before 9.1.0 were recoverable — their CI artifacts had already
  passed the 14-day retention window by the time this archive was created.
- Going forward, tagged releases via `.github/workflows/release.yml`
  (`git tag vX.Y.Z && git push origin vX.Y.Z`) create a permanent, signed
  GitHub Release with attached APK/AAB — a much better source of historical
  builds than CI artifacts, which always expire.

**Repo size note:** these two files are ~22 MB each and are committed to git
history permanently (exempted from the `*.apk` gitignore rule, the same way
`android/app/debug.keystore` is exempted from `*.keystore`) — every future
clone of this repo downloads them. That's an acceptable one-time cost for two
files; if this archive keeps growing, switch to [Git LFS](https://git-lfs.com/)
or just rely on GitHub Releases instead of committing binaries here.
