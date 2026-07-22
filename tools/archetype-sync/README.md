# archetype-sync

Maven archetypes generate a project once and then have no further
relationship to the template — if the archetype improves later, every
project already generated from it is stuck with whatever bugs existed
at generation time, and a project that was never generated from it at
all has no way to adopt any of it later. `archetype-sync.sh` closes both
gaps: it's the `cruft update` (cookiecutter) idea, built for Maven
archetypes, which have no such mechanism natively.

Three commands, covering three starting points:

| Command   | Starting point | Result |
|-----------|-----------------|--------|
| `init`    | Project already generated wholesale from this archetype | Tracks it so `update` can pull in later fixes |
| `onboard` | Existing AEM Forms project, **not** generated from this archetype | Adds just the spec-driven-dev framework layer (`bmad/`, `specs/`, `AGENTS.md`, optionally the `SpecToCodeGenerator` stub) without touching anything else |
| `update`  | Project already tracked (via either of the above) | Pulls in changes since the tracked ref, scoped to whatever `init`/`onboard` originally covered |

Requires `git`, `jq`, and `mvn` on `PATH` (see "How it works" below for
why the target project also needs to be a git repo).

## How it works

1. Regenerates the archetype's output at an old ref and a new ref, using
   the exact `archetype:generate` parameters your project uses (or will
   use), so the two outputs are directly comparable. For `onboard`, the
   "old" side is an empty tree — the diff is just "here's everything
   being added." Both sides are pruned to a path allowlist first when one
   applies (`onboard` always sets one; `update` inherits it from whatever
   `init`/`onboard` recorded), so unrelated parts of the archetype never
   enter the diff.
2. Diffs those two (pruned) trees. That diff is exactly what changed —
   already resolved through Velocity, not a diff of template source.
3. Imports that diff's git history into your project's repo and applies
   it with `git apply --3way`, so your own customizations (or, for
   `onboard`, anything already in your project at a colliding path) merge
   with the incoming changes. Where both sides touched the same lines,
   you get standard `<<<<<<<` conflict markers to resolve by hand;
   everything else merges automatically.

Requires the target project to be a git repo — that's what makes the
3-way merge possible instead of blindly overwriting or clobbering
whatever's already there.

## Usage: project already generated from this archetype

```bash
# One-time: start tracking a project you already generated.
# You supply the original archetype:generate parameters yourself —
# check your shell history or bmad/00-Project-Initialization/.
./archetype-sync.sh init \
    --project-dir ./my-forms-project \
    --archetype-repo git@github.com:narendragandhi/aem-forms-spec-driven-dev-archetype.git \
    --from-ref 9d48f28 \
    --group-id com.mycompany --artifact-id my-forms-project \
    --version 1.0.0-SNAPSHOT --package com.mycompany \
    --app-name MyFormsApp --forms-version afaacs

git -C ./my-forms-project add -A
git -C ./my-forms-project commit -m "track project with archetype-sync"

# Whenever you want to pull in archetype improvements:
./archetype-sync.sh update --project-dir ./my-forms-project --to-ref main
```

## Usage: existing AEM Forms project adopting spec-driven-dev

For a project that predates this archetype and wasn't generated from it —
add just the methodology layer, leaving the rest of the project alone:

```bash
./archetype-sync.sh onboard \
    --project-dir ./existing-aem-forms-repo \
    --archetype-repo git@github.com:narendragandhi/aem-forms-spec-driven-dev-archetype.git \
    --group-id com.mycompany --artifact-id existing-aem-forms-repo \
    --version 1.0.0-SNAPSHOT --package com.mycompany \
    --app-name ExistingApp
```

This adds `bmad/` (the BMAD/Beads/GasTown methodology, agents, and
scripts), `specs/` (example JSON form specs), and `AGENTS.md`. If your
project already has a file at one of those paths (most likely
`AGENTS.md`), you'll get a `<<<<<<<` conflict instead of a silent
overwrite — resolve it like any merge conflict.

`--group-id`/`--package`/`--app-name`/`--version` are still required —
`archetype:generate` needs them regardless — but note that `bmad/`,
`specs/`, and `AGENTS.md` are all marked `filtered="false"` in the
archetype itself (Velocity treats markdown `##` headings as comments, so
these can't safely go through the filter). That means the handful of
`${appName}`/`${artifactId}` placeholders that do appear in this scope
(`bmad/06-Integrations/headless-forms.md`,
`bmad/06-Integrations/interactive-communications-guide.md`,
`bmad/bead-examples/issues/fin-app/fin-app-001.yaml`, and `AGENTS.md`'s
issue-ID example) land unresolved — same as they would from a real
`archetype:generate`. This is a pre-existing archetype limitation, not
something `onboard` introduces; everything else under `bmad/`/`specs/`
is placeholder-free, generic methodology content unaffected by it.

Add `--include-spec-to-code-generator` to also bring in the
`SpecToCodeGenerator` stub (`core/src/main/java/<package>/core/workflow/`)
— it's a genuine stub (a logged `TODO`, no real dependencies beyond OSGi
and slf4j), so it's low-risk to add, but only do this if your project
already has a `core` OSGi bundle module at that package; otherwise the
file lands with no module to compile it.

`onboard` writes `.archetype-sync.json` with the path list it used, so a
later `update --to-ref <newer-ref>` stays scoped to that same subset
instead of trying to pull in the rest of the archetype (`ui.apps`,
`dispatcher`, etc.) that your project never had in the first place.

## Notes

`update` requires a clean git working tree (commit or stash first) and
builds the archetype twice (once per ref) to regenerate both baselines,
so expect it to take a couple of minutes. On success (or on a conflicted
apply — see below) it rewrites `.archetype-sync.json`'s tracked ref;
commit that alongside your merge resolution so the next `update` diffs
from where you actually landed.

If `update` or `onboard` reports conflicts, `.archetype-sync.json` is
still rewritten to the new ref (or the onboarded ref) regardless —
resolve the `<<<<<<<` markers and commit that alongside it. You don't
need to re-run anything to make progress "stick": `onboard` can't be
re-run anyway (the manifest already exists, so it'll tell you to use
`update` instead), and re-running `update` with the same `--to-ref` is
just a harmless no-op once the manifest's ref has already caught up —
the tool only diffs `ref -> to-ref`, so with them equal there's nothing
left to apply.

## Known limits

- `formsVersion` is the only generation parameter this repo currently
  exposes; if the archetype gains more parameters later, all three
  commands will need matching `--flag` support.
- A ref with no `.mvn/jvm.config` (anything before this tool existed)
  needs `MAVEN_OPTS=-Xmx4g` in your environment when building — the
  script sets this as a fallback if `MAVEN_OPTS` isn't already set, but
  an explicit low value in your shell will override it.
- Renames are detected via `git diff -M`, but heavily restructured
  releases (whole directories moved) may still produce a noisier patch
  than a hand-reviewed migration guide would.
- Tested against macOS's default `/bin/bash` (3.2), which has a known
  wart around expanding empty arrays under `set -u` — this script works
  around it. If you're running some other unusual `bash` on `PATH`,
  `bash -n archetype-sync.sh` is a quick sanity check.
