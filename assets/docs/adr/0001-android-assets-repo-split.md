# ADR-0001: Repo root holds exactly two folders — `android/` and `assets/`

**Date:** 2026-08-08 (restructured), documented retroactively 2026-08-14.

> **Amended by [ADR-0002](0002-public-repo-docs-folder.md) (2026-09-03):** a third
> root folder, `docs/`, was added when the repository went public — GitHub Pages
> can only serve from the repo root or `/docs`. The principle below still stands;
> `docs/` is a tooling exception on the same grounds as `.github/`.

## Context

The repo originally kept Gradle project files at the true repo root
(`build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `app/`) alongside
unrelated material — Play Store listing text, design source, generated
graphics — all flattened into the same top level. This made the repo root
noisy to open in Android Studio (which expects a Gradle root, not a mixed
bag) and made it unclear which files were "the app" versus "everything else."

An earlier intermediate state split things into `playstore/` + the Gradle
project still at the root, which didn't fully solve the mixing problem.

## Decision

Move the entire Gradle/Android Studio project into `android/` (root build
files, `gradle/`, the `:app` module — everything). Move all non-code material
(design source, generated graphics, distribution/store text, and now `docs/`)
into `assets/`. The true repo root holds exactly these two folders, plus only
what tooling *requires* at the true root: `.github/` (GitHub only reads
workflow/dependabot config from `<repo-root>/.github/`) and `CLAUDE.md`/
`AGENTS.md` (Claude Code / Codex only auto-load project instructions from the
root they're opened in).

## Why not the alternative(s)

- **Keep Gradle at the repo root, move only non-code stuff out**: doesn't fix
  the actual pain point, which is opening the wrong folder in Android Studio
  and Gradle sync errors from the repo root not being a clean Gradle project.
- **A `misc/`/`archive/` folder for anything that doesn't fit**: considered
  and explicitly rejected — there was no genuine "old version" or
  archive-worthy content at the time, and manufacturing a junk-drawer folder
  for its own sake works against the two-folder goal rather than for it.

## Consequences

- Every reference to the Gradle project (CI workflow paths, `dependabot.yml`
  directory, scripts like `generate_graphics.py`, doc links) needs an
  `android/` prefix — a one-time cost paid when this landed, and an ongoing
  discipline: new tooling that reads repo-root-relative paths needs to account
  for this.
- A separate session partially attempted to revert this (deleted
  `.gitignore`/`AGENTS.md`, rewrote CLAUDE.md's prose to describe a flat
  layout) without actually moving files back — caught and rolled back
  2026-08-08. The lesson generalized into CLAUDE.md's guidance: verify the
  real filesystem with `ls`/`git status` before trusting doc prose if the two
  ever seem to disagree.
- New non-code material (this ADR folder included) goes in `assets/`, not the
  repo root — see [assets/docs/adr/README.md](README.md).
