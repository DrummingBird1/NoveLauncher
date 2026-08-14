# Architecture Decision Records

Short, dated write-ups for decisions that are expensive to reverse or easy to
second-guess later without the original reasoning — repo layout, signing
strategy, choice of a library over its alternatives, that kind of thing. Not
for routine feature work; CLAUDE.md/AGENTS.md's "Pitfalls" sections already
cover implementation gotchas, and commit messages cover routine changes.

Lives under `assets/` (not the repo root) because the repo root is
intentionally limited to exactly two folders — `android/` and `assets/` — see
[ADR-0001](0001-android-assets-repo-split.md) for why.

## When to write one

- A structural decision someone might plausibly want to revert or redo later
  (a restructuring, a new cross-cutting dependency, a signing/security posture
  change).
- A decision where "why not the obvious alternative?" isn't self-evident from
  the code alone.

Don't write one for a normal feature, bugfix, or anything already explained
by a Pitfall entry in CLAUDE.md/AGENTS.md.

## Format

Filename: `NNNN-short-slug.md`, numbered sequentially. Each ADR is short:
**Context** (what prompted this), **Decision** (what was chosen), **Why not
the alternative(s)**, **Consequences** (what this makes harder/easier later).
