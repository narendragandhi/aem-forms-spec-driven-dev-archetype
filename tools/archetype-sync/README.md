# archetype-sync

Maven archetypes generate a project once and then have no further
relationship to the template — if the archetype improves later, every
project already generated from it is stuck with whatever bugs existed
at generation time. `archetype-sync.sh` closes that gap: it's the
`cruft update` (cookiecutter) idea, built for Maven archetypes, which
have no such mechanism natively.

## How it works

1. Regenerates the archetype's output at the ref your project was
   originally generated from, and again at the ref you're updating to —
   both times with the exact `archetype:generate` parameters your project
   used, so the two outputs are directly comparable.
2. Diffs those two generated trees. That diff is exactly what changed in
   the template, already resolved through Velocity — not a diff of
   template source.
3. Imports that diff's git history into your project's repo and applies
   it with `git apply --3way`, so your own customizations merge with the
   incoming template changes. Where both sides touched the same lines,
   you get standard `<<<<<<<` conflict markers to resolve by hand;
   everything else merges automatically.

Requires the target project to be a git repo — that's what makes the
3-way merge possible instead of blindly overwriting local changes.

## Usage

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

`update` requires a clean git working tree (commit or stash first) and
builds the archetype twice (once per ref) to regenerate both baselines,
so expect it to take a couple of minutes. On success it rewrites
`.archetype-sync.json`'s tracked ref — commit that alongside your merge
resolution so the next `update` diffs from where you actually landed.

## Known limits

- `formsVersion` is the only generation parameter this repo currently
  exposes; if the archetype gains more parameters later, `init`/`update`
  will need matching `--flag` support.
- A ref with no `.mvn/jvm.config` (anything before this tool existed)
  needs `MAVEN_OPTS=-Xmx4g` in your environment when building — the
  script sets this as a fallback if `MAVEN_OPTS` isn't already set, but
  an explicit low value in your shell will override it.
- Renames are detected via `git diff -M`, but heavily restructured
  releases (whole directories moved) may still produce a noisier patch
  than a hand-reviewed migration guide would.
