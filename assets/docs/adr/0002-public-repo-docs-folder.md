# ADR-0002: `docs/` joins the repo root when the project goes public

**Date:** 2026-09-03. Amends [ADR-0001](0001-android-assets-repo-split.md).

## Context

ADR-0001 established that the repo root holds exactly two folders —
`android/` and `assets/` — with `.github/` and `CLAUDE.md`/`AGENTS.md`
tolerated only because their tooling reads them from the true root and
nowhere else.

Making the repository public added two requirements that the two-folder rule
could not absorb:

1. **A hosted privacy policy.** App stores require a public HTTPS URL; a
   Markdown file in a repo is not enough. This had been solved with a
   separate public repo (`novelauncher-legal`) because this repo was private
   and GitHub Pages is unavailable for private repos on the free plan. Once
   this repo became public, keeping the policy in a second repo meant two
   places to update and two URLs to keep straight.
2. **GitHub's own conventions.** GitHub only surfaces `README.md`,
   `LICENSE`, `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md` and
   `CHANGELOG.md` from the repository root.

GitHub Pages can only publish from one of: the repo root, a `/docs` folder on
a branch, or a dedicated `gh-pages` branch.

## Decision

Add `docs/` to the repo root, holding the published website (landing page,
privacy policy, terms) plus the translated READMEs, and serve GitHub Pages
from `main` → `/docs`. Retire the separate `novelauncher-legal` repo, leaving
a redirect at its old URL for any link already shared.

Keep the community health files (`README.md`, `LICENSE`, `CONTRIBUTING.md`,
`SECURITY.md`, `CODE_OF_CONDUCT.md`, `CHANGELOG.md`) at the root, for the
same reason `.github/` is already there: the tooling reads them from nowhere
else.

The rule ADR-0001 was really protecting — *"the root does not become a
dumping ground, and everything that can live inside `android/` or `assets/`
does"* — still holds. `docs/` is an exception on the same grounds as
`.github/`, not a relaxation of the rule.

## Why not the alternative(s)

- **Serve Pages from a `gh-pages` branch instead**: keeps the root at two
  folders, but splits the site from the source that generates it, so the site
  can silently drift from the app it documents. A branch nobody checks out is
  worse than a folder everyone can see.
- **Keep `novelauncher-legal` as a separate repo**: two repos, two update
  paths, and the privacy URL living somewhere unrelated to the app. The whole
  reason it existed (this repo being private) disappeared.
- **Put the site under `assets/`**: not possible — Pages will not serve from
  an arbitrary subfolder.

## Consequences

- The repo root is now `android/`, `assets/`, `docs/`, plus `.github/` and the
  root-level Markdown/licence files. Anything new that isn't forced to the
  root by tooling still belongs in `android/` or `assets/`.
- The site and the app version can drift: `docs/index.html` hardcodes the
  current version badge and feature list. Update it when cutting a release.
- ADRs stay under `assets/docs/adr/` (not the new root `docs/`) — they're
  internal engineering records, not published website content, and moving
  them would put them on the public site.
