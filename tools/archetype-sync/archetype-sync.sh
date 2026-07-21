#!/usr/bin/env bash
#
# archetype-sync.sh — pull template improvements into a project that was
# already generated from aem-forms-bmad-archetype (Maven archetypes don't
# have a built-in update mechanism; this adds one).
#
# Mechanism (same idea as `cruft update` for cookiecutter, adapted for
# Maven archetypes, which have no native notion of "regenerate in place"):
#
#   1. Regenerate the archetype's output at the OLD ref and the NEW ref,
#      using the exact parameters the target project was originally
#      generated with. Both regenerations are apples-to-apples: same
#      inputs, only the template differs.
#   2. Diff those two regenerated trees. That diff is exactly what changed
#      in the template, already resolved through Velocity.
#   3. Import that diff's git objects into the target project's repo and
#      apply it with `git apply --3way`, so the user's own customizations
#      merge with the template's changes (conflict markers where they
#      overlap) instead of being silently overwritten.
#
# Requires the target project to be a git repository — that's what lets
# --3way merge template changes against local edits instead of clobbering
# them.
set -euo pipefail

SCRIPT_NAME="$(basename "$0")"
ARCHETYPE_GROUP_ID="com.example.aem.archetype"
ARCHETYPE_ARTIFACT_ID="aem-forms-bmad-archetype"

usage() {
    cat <<'EOF'
Usage:
  archetype-sync.sh init --project-dir DIR --archetype-repo REPO --from-ref REF \
      --group-id ID --artifact-id ID --version V --package PKG --app-name NAME \
      [--forms-version afaacs|6.5]

      Start tracking an already-generated project. Writes
      DIR/.archetype-sync.json recording the archetype repo, the ref it was
      generated from, and the exact archetype:generate parameters used.
      You must supply these yourself (they aren't auto-detected) — check
      your shell history or bmad/00-Project-Initialization/ for the
      original `mvn archetype:generate` command.

  archetype-sync.sh update --project-dir DIR [--archetype-repo REPO] [--to-ref REF]

      Bring the tracked project up to date with REF (default: HEAD).
      Requires a clean git working tree in DIR (commit or stash first).
      On conflict, leaves standard <<<<<<< conflict markers in the
      affected files for manual resolution — nothing is auto-committed.

Examples:
  archetype-sync.sh init \
      --project-dir ./my-forms-project \
      --archetype-repo git@github.com:narendragandhi/aem-forms-spec-driven-dev-archetype.git \
      --from-ref 9d48f28 \
      --group-id com.mycompany --artifact-id my-forms-project \
      --version 1.0.0-SNAPSHOT --package com.mycompany \
      --app-name MyFormsApp --forms-version afaacs

  archetype-sync.sh update --project-dir ./my-forms-project --to-ref main
EOF
}

die() { echo "error: $*" >&2; exit 1; }

need_bin() {
    command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not on PATH"
}

cmd_init() {
    local project_dir="" archetype_repo="" from_ref="" group_id="" artifact_id=""
    local version="" package="" app_name="" forms_version="afaacs"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --project-dir) project_dir="$2"; shift 2 ;;
            --archetype-repo) archetype_repo="$2"; shift 2 ;;
            --from-ref) from_ref="$2"; shift 2 ;;
            --group-id) group_id="$2"; shift 2 ;;
            --artifact-id) artifact_id="$2"; shift 2 ;;
            --version) version="$2"; shift 2 ;;
            --package) package="$2"; shift 2 ;;
            --app-name) app_name="$2"; shift 2 ;;
            --forms-version) forms_version="$2"; shift 2 ;;
            *) die "unknown argument: $1" ;;
        esac
    done

    [[ -n "$project_dir" ]] || die "--project-dir is required"
    [[ -n "$archetype_repo" ]] || die "--archetype-repo is required"
    [[ -n "$from_ref" ]] || die "--from-ref is required"
    [[ -n "$group_id" ]] || die "--group-id is required"
    [[ -n "$artifact_id" ]] || die "--artifact-id is required"
    [[ -n "$version" ]] || die "--version is required"
    [[ -n "$package" ]] || die "--package is required"
    [[ -n "$app_name" ]] || die "--app-name is required"
    [[ -d "$project_dir" ]] || die "project dir '$project_dir' does not exist"

    local manifest="$project_dir/.archetype-sync.json"
    [[ -f "$manifest" ]] && die "$manifest already exists — this project is already tracked"

    jq -n \
        --arg archetypeRepo "$archetype_repo" \
        --arg ref "$from_ref" \
        --arg groupId "$group_id" \
        --arg artifactId "$artifact_id" \
        --arg version "$version" \
        --arg package "$package" \
        --arg appName "$app_name" \
        --arg formsVersion "$forms_version" \
        '{
            archetypeRepo: $archetypeRepo,
            ref: $ref,
            parameters: {
                groupId: $groupId,
                artifactId: $artifactId,
                version: $version,
                package: $package,
                appName: $appName,
                formsVersion: $formsVersion
            }
        }' > "$manifest"

    echo "Wrote $manifest"
    echo "Tracking $project_dir against $archetype_repo @ $from_ref"
    echo "Commit $manifest before running 'update' — it needs a clean working tree."
}

# Regenerates the archetype at $1 (git ref) using the parameters in
# $manifest_params_json, installs it under a throwaway coordinate so it
# never collides with a real local install, and prints the path to the
# generated project directory.
generate_baseline() {
    local ref="$1" label="$2" tmp="$3"
    local group_id="$4" artifact_id="$5" version="$6" package="$7" app_name="$8" forms_version="$9"

    echo "==> [$label] checking out archetype @ $ref" >&2
    local src="$tmp/src-$label"
    mkdir -p "$src"
    git -C "$tmp/repo" archive "$ref" | tar -x -C "$src"

    local synthetic_version="0.0.0-sync-$label"
    # the archetype's own pom.xml pins itself to 1.0.0-SNAPSHOT (the string
    # is unique in the file, so a plain substitution is enough and stays
    # portable across GNU and BSD sed); retarget this checkout to a
    # throwaway version so it can't collide with a real local install of
    # the archetype or with the other ref's checkout
    sed -i.bak "s/<version>1.0.0-SNAPSHOT<\/version>/<version>${synthetic_version}<\/version>/" "$src/pom.xml"
    rm -f "$src/pom.xml.bak"

    echo "==> [$label] building archetype jar" >&2
    # some historical refs predate .mvn/jvm.config and OOM under the
    # default heap assembling the (~38MB) archetype jar
    ( cd "$src" && MAVEN_OPTS="${MAVEN_OPTS:--Xmx4g}" mvn -q -B -Darchetype.test.skip=true clean install )

    echo "==> [$label] generating project" >&2
    local out="$tmp/baseline-$label"
    mkdir -p "$out"
    ( cd "$out" && mvn -q -B archetype:generate \
        -DarchetypeGroupId="$ARCHETYPE_GROUP_ID" \
        -DarchetypeArtifactId="$ARCHETYPE_ARTIFACT_ID" \
        -DarchetypeVersion="$synthetic_version" \
        -DgroupId="$group_id" \
        -DartifactId="$artifact_id" \
        -Dversion="$version" \
        -Dpackage="$package" \
        -DappName="$app_name" \
        -DformsVersion="$forms_version" )

    # drop anything a build would produce — the diff should be template-only
    find "$out/$artifact_id" -type d \( -name target -o -name node_modules \) -prune -exec rm -rf {} +

    echo "$out/$artifact_id"
}

cmd_update() {
    local project_dir="." archetype_repo="" to_ref="HEAD"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --project-dir) project_dir="$2"; shift 2 ;;
            --archetype-repo) archetype_repo="$2"; shift 2 ;;
            --to-ref) to_ref="$2"; shift 2 ;;
            *) die "unknown argument: $1" ;;
        esac
    done

    local manifest="$project_dir/.archetype-sync.json"
    [[ -f "$manifest" ]] || die "no $manifest found in '$project_dir' — run 'init' first"

    git -C "$project_dir" rev-parse --git-dir >/dev/null 2>&1 || \
        die "'$project_dir' is not a git repository — required so 'git apply --3way' can merge template changes with your customizations"
    [[ -z "$(git -C "$project_dir" status --porcelain)" ]] || \
        die "'$project_dir' has uncommitted changes — commit or stash first so a failed merge is easy to undo"

    [[ -n "$archetype_repo" ]] || archetype_repo="$(jq -r '.archetypeRepo' "$manifest")"
    local from_ref group_id artifact_id version package app_name forms_version
    from_ref="$(jq -r '.ref' "$manifest")"
    group_id="$(jq -r '.parameters.groupId' "$manifest")"
    artifact_id="$(jq -r '.parameters.artifactId' "$manifest")"
    version="$(jq -r '.parameters.version' "$manifest")"
    package="$(jq -r '.parameters.package' "$manifest")"
    app_name="$(jq -r '.parameters.appName' "$manifest")"
    forms_version="$(jq -r '.parameters.formsVersion' "$manifest")"

    if [[ "$from_ref" == "$to_ref" ]]; then
        echo "already at $to_ref — nothing to do"
        return 0
    fi

    local tmp
    tmp="$(mktemp -d)"
    trap 'rm -rf "$tmp"' EXIT

    echo "==> Cloning $archetype_repo"
    git clone --quiet "$archetype_repo" "$tmp/repo"

    local old_dir new_dir
    old_dir="$(generate_baseline "$from_ref" "old" "$tmp" "$group_id" "$artifact_id" "$version" "$package" "$app_name" "$forms_version")"
    new_dir="$(generate_baseline "$to_ref" "new" "$tmp" "$group_id" "$artifact_id" "$version" "$package" "$app_name" "$forms_version")"

    echo "==> Building the template diff ($from_ref -> $to_ref)"
    local diffrepo="$tmp/diffrepo"
    mkdir -p "$diffrepo"
    git -C "$diffrepo" init -q
    git -C "$diffrepo" config user.email "sync@archetype-sync.local"
    git -C "$diffrepo" config user.name "archetype-sync"
    cp -a "$old_dir/." "$diffrepo/"
    git -C "$diffrepo" add -A
    git -C "$diffrepo" commit -q -m "template @ $from_ref"
    find "$diffrepo" -mindepth 1 -maxdepth 1 -not -name .git -exec rm -rf {} +
    cp -a "$new_dir/." "$diffrepo/"
    git -C "$diffrepo" add -A
    git -C "$diffrepo" commit -q -m "template @ $to_ref"

    local patch="$tmp/template.patch"
    git -C "$diffrepo" diff -M --no-color HEAD~1 HEAD > "$patch" || true

    if [[ ! -s "$patch" ]]; then
        echo "No template changes between $from_ref and $to_ref for these parameters."
        jq --arg ref "$to_ref" '.ref = $ref' "$manifest" > "$manifest.tmp" && mv "$manifest.tmp" "$manifest"
        return 0
    fi

    echo "==> Importing template history into $project_dir so 3-way merge has a base"
    git -C "$project_dir" remote add archetype-sync-tmp "$diffrepo" 2>/dev/null || \
        git -C "$project_dir" remote set-url archetype-sync-tmp "$diffrepo"
    git -C "$project_dir" fetch --quiet archetype-sync-tmp
    git -C "$project_dir" remote remove archetype-sync-tmp

    echo "==> Applying template diff to $project_dir"
    local apply_status=0
    git -C "$project_dir" apply --3way --whitespace=nowarn "$patch" || apply_status=$?

    local conflicts
    conflicts="$(git -C "$project_dir" diff --name-only --diff-filter=U || true)"

    jq --arg ref "$to_ref" '.ref = $ref' "$manifest" > "$manifest.tmp" && mv "$manifest.tmp" "$manifest"

    if [[ $apply_status -ne 0 || -n "$conflicts" ]]; then
        echo
        echo "Applied with conflicts. Files needing manual resolution:"
        if [[ -n "$conflicts" ]]; then
            echo "$conflicts" | sed 's/^/  - /'
        else
            echo "  (git apply reported errors — see output above; some hunks may not have applied at all)"
        fi
        echo
        echo "Resolve the <<<<<<< markers, then: git add -A && git commit"
        echo "$manifest was updated to ref '$to_ref' regardless — re-run update later and it will diff from here."
        exit 1
    fi

    echo
    echo "Applied cleanly. Review with 'git diff', then commit."
    echo "$manifest updated to ref '$to_ref'."
}

main() {
    need_bin git
    need_bin jq
    need_bin mvn

    local cmd="${1:-}"
    case "$cmd" in
        init) shift; cmd_init "$@" ;;
        update) shift; cmd_update "$@" ;;
        -h|--help|"") usage ;;
        *) die "unknown command: $cmd (expected 'init' or 'update')" ;;
    esac
}

main "$@"
