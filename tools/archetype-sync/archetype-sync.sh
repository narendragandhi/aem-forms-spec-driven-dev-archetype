#!/usr/bin/env bash
#
# archetype-sync.sh — three ways to connect an existing project to
# aem-forms-bmad-archetype, which (like all Maven archetypes) has no
# built-in update mechanism once a project is generated:
#
#   init      Start tracking a project that was already generated from
#             this archetype, so 'update' can pull in template fixes later.
#   update    Bring a tracked project's template-derived files up to date
#             with a newer archetype ref.
#   onboard   Add just the spec-driven-dev framework layer (bmad/, specs/,
#             AGENTS.md, optionally the SpecToCodeGenerator stub) onto a
#             project that was NEVER generated from this archetype —
#             without touching anything else in that project.
#
# Mechanism (same idea as `cruft update` for cookiecutter, adapted for
# Maven archetypes, which have no native notion of "regenerate in place"):
#
#   1. Regenerate the archetype's output at an OLD ref and a NEW ref, using
#      the exact archetype:generate parameters the target project uses (or
#      will use). Both regenerations are apples-to-apples: same inputs,
#      only the template differs. For 'onboard', the OLD side is an empty
#      tree — the "diff" is just "here is everything being added".
#      Both sides are pruned to a path allowlist first, if one is given
#      ('onboard' always gives one; 'update' only if 'init'/'onboard' set
#      one), so unrelated parts of the archetype never enter the diff.
#   2. Diff those two (pruned) trees. That diff is exactly what changed —
#      already resolved through Velocity, not a diff of template source.
#   3. Import that diff's git objects into the target project's repo and
#      apply it with `git apply --3way`, so the user's own customizations
#      merge with the incoming changes (conflict markers where they
#      overlap) instead of being silently overwritten or blindly skipped.
#
# Requires the target project to be a git repository — that's what lets
# --3way merge incoming changes against local edits instead of clobbering
# them, and what makes a bad merge trivial to undo.
set -euo pipefail

ARCHETYPE_GROUP_ID="com.example.aem.archetype"
ARCHETYPE_ARTIFACT_ID="aem-forms-bmad-archetype"
DEFAULT_ONBOARD_PATHS="bmad,specs,AGENTS.md"

usage() {
    cat <<'EOF'
Usage:
  archetype-sync.sh init --project-dir DIR --archetype-repo REPO --from-ref REF \
      --group-id ID --artifact-id ID --version V --package PKG --app-name NAME \
      [--forms-version afaacs|6.5]

      Start tracking a project that was already generated wholesale from
      this archetype. Writes DIR/.archetype-sync.json recording the
      archetype repo, the ref it was generated from, and the exact
      archetype:generate parameters used. You must supply these yourself
      (they aren't auto-detected) — check your shell history or
      bmad/00-Project-Initialization/ for the original command.

  archetype-sync.sh update --project-dir DIR [--archetype-repo REPO] [--to-ref REF]

      Bring the tracked project up to date with REF (default: HEAD).
      Works for projects tracked via either 'init' (whole project) or
      'onboard' (framework subset only — the update stays scoped to
      whatever subset 'onboard' recorded). Requires a clean git working
      tree in DIR. On conflict, leaves standard <<<<<<< markers for
      manual resolution — nothing is auto-committed.

  archetype-sync.sh onboard --project-dir DIR --archetype-repo REPO \
      --group-id ID --artifact-id ID --version V --package PKG --app-name NAME \
      [--ref REF] [--forms-version afaacs|6.5] [--paths "bmad,specs,AGENTS.md"] \
      [--include-spec-to-code-generator]

      For a project that was NOT generated from this archetype: adds just
      the spec-driven-dev framework layer (bmad/, specs/, AGENTS.md by
      default) without touching anything else in the project. --ref
      defaults to HEAD. archetype:generate requires --group-id/--package/
      --app-name/--version regardless of what lands where; note that
      bmad/specs/AGENTS.md are unfiltered in the archetype itself (see
      README), so the few ${appName}/${artifactId} placeholders inside
      this scope land unresolved, same as a full generate would leave
      them. Pass --include-spec-to-code-generator to also
      add the SpecToCodeGenerator stub under
      core/src/main/java/<package>/core/workflow/ — only do this if your
      project already has a 'core' OSGi bundle module at that package.
      Writes DIR/.archetype-sync.json with the onboarded path list
      recorded, so later 'update' calls stay scoped to the same subset.

Examples:
  archetype-sync.sh init \
      --project-dir ./my-forms-project \
      --archetype-repo git@github.com:narendragandhi/aem-forms-spec-driven-dev-archetype.git \
      --from-ref 9d48f28 \
      --group-id com.mycompany --artifact-id my-forms-project \
      --version 1.0.0-SNAPSHOT --package com.mycompany \
      --app-name MyFormsApp --forms-version afaacs

  archetype-sync.sh onboard \
      --project-dir ./existing-aem-forms-repo \
      --archetype-repo git@github.com:narendragandhi/aem-forms-spec-driven-dev-archetype.git \
      --group-id com.mycompany --artifact-id existing-aem-forms-repo \
      --version 1.0.0-SNAPSHOT --package com.mycompany \
      --app-name ExistingApp

  archetype-sync.sh update --project-dir ./my-forms-project --to-ref main
EOF
}

die() { echo "error: $*" >&2; exit 1; }

need_bin() {
    command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not on PATH"
}

require_clean_git_repo() {
    local dir="$1" purpose="$2"
    git -C "$dir" rev-parse --git-dir >/dev/null 2>&1 || \
        die "'$dir' is not a git repository — required so 'git apply --3way' can $purpose"
    [[ -z "$(git -C "$dir" status --porcelain)" ]] || \
        die "'$dir' has uncommitted changes — commit or stash first so a failed merge is easy to undo"
}

write_manifest() {
    local manifest="$1" archetype_repo="$2" ref="$3" group_id="$4" artifact_id="$5"
    local version="$6" package="$7" app_name="$8" forms_version="$9"
    shift 9
    local paths=("$@")

    local paths_json="[]"
    if [[ ${#paths[@]} -gt 0 ]]; then
        paths_json="$(printf '%s\n' "${paths[@]+"${paths[@]}"}" | jq -R . | jq -s .)"
    fi

    jq -n \
        --arg archetypeRepo "$archetype_repo" \
        --arg ref "$ref" \
        --arg groupId "$group_id" \
        --arg artifactId "$artifact_id" \
        --arg version "$version" \
        --arg package "$package" \
        --arg appName "$app_name" \
        --arg formsVersion "$forms_version" \
        --argjson paths "$paths_json" \
        '{
            archetypeRepo: $archetypeRepo,
            ref: $ref,
            paths: $paths,
            parameters: {
                groupId: $groupId,
                artifactId: $artifactId,
                version: $version,
                package: $package,
                appName: $appName,
                formsVersion: $formsVersion
            }
        }' > "$manifest"
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

    write_manifest "$manifest" "$archetype_repo" "$from_ref" "$group_id" "$artifact_id" \
        "$version" "$package" "$app_name" "$forms_version"

    echo "Wrote $manifest"
    echo "Tracking $project_dir against $archetype_repo @ $from_ref"
    echo "Commit $manifest before running 'update' — it needs a clean working tree."
}

# Produces a directory containing the requested subset (or everything, if
# no paths are given) of the archetype's output at $ref, generated with
# the given archetype:generate parameters. Prints the resulting directory.
#
# ref="EMPTY" is a sentinel meaning "nothing" — used by 'onboard' as the
# baseline being diffed *from*, so the whole diff reads as "add these".
generate_baseline() {
    local ref="$1" label="$2" tmp="$3"
    local group_id="$4" artifact_id="$5" version="$6" package="$7" app_name="$8" forms_version="$9"
    shift 9
    local paths=("$@")

    local pruned="$tmp/pruned-$label"
    mkdir -p "$pruned"

    if [[ "$ref" == "EMPTY" ]]; then
        echo "$pruned"
        return 0
    fi

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

    local generated="$out/$artifact_id"
    # drop anything a build would produce — the diff should be template-only
    find "$generated" -type d \( -name target -o -name node_modules \) -prune -exec rm -rf {} +

    if [[ ${#paths[@]} -eq 0 ]]; then
        cp -a "$generated/." "$pruned/"
    else
        local p
        for p in "${paths[@]+"${paths[@]}"}"; do
            if [[ -e "$generated/$p" ]]; then
                mkdir -p "$pruned/$(dirname "$p")"
                cp -a "$generated/$p" "$pruned/$p"
            fi
        done
    fi

    echo "$pruned"
}

# Shared engine for 'update' and 'onboard': regenerates the archetype at
# $from_ref and $to_ref (either may be "EMPTY"), diffs the two outputs
# (restricted to $paths if any are given), and 3-way merges the result
# into $project_dir. Returns non-zero if the merge left conflicts.
do_sync() {
    local project_dir="$1" archetype_repo="$2" from_ref="$3" to_ref="$4"
    local group_id="$5" artifact_id="$6" version="$7" package="$8" app_name="$9" forms_version="${10}"
    shift 10
    local paths=("$@")

    if [[ "$from_ref" == "$to_ref" ]]; then
        echo "already at $to_ref — nothing to do"
        return 0
    fi

    # Everything below runs in a subshell so its `trap ... EXIT` and working
    # variables are confined to this one sync — a `trap ... RETURN` set
    # directly in this function would be dynamically scoped and keep firing
    # on every later function return in the *caller*, well after $tmp is
    # gone, blowing up under `set -u`.
    (
        tmp="$(mktemp -d)"
        trap 'rm -rf "$tmp"' EXIT

        if [[ "$from_ref" != "EMPTY" || "$to_ref" != "EMPTY" ]]; then
            echo "==> Cloning $archetype_repo"
            git clone --quiet "$archetype_repo" "$tmp/repo"
        fi

        old_dir="$(generate_baseline "$from_ref" "old" "$tmp" "$group_id" "$artifact_id" "$version" "$package" "$app_name" "$forms_version" "${paths[@]+"${paths[@]}"}")"
        new_dir="$(generate_baseline "$to_ref" "new" "$tmp" "$group_id" "$artifact_id" "$version" "$package" "$app_name" "$forms_version" "${paths[@]+"${paths[@]}"}")"

        echo "==> Building the diff ($from_ref -> $to_ref)"
        diffrepo="$tmp/diffrepo"
        mkdir -p "$diffrepo"
        git -C "$diffrepo" init -q
        git -C "$diffrepo" config user.email "sync@archetype-sync.local"
        git -C "$diffrepo" config user.name "archetype-sync"
        cp -a "$old_dir/." "$diffrepo/"
        git -C "$diffrepo" add -A
        git -C "$diffrepo" commit -q -m "template @ $from_ref" --allow-empty
        find "$diffrepo" -mindepth 1 -maxdepth 1 -not -name .git -exec rm -rf {} +
        cp -a "$new_dir/." "$diffrepo/"
        git -C "$diffrepo" add -A
        git -C "$diffrepo" commit -q -m "template @ $to_ref" --allow-empty

        patch="$tmp/template.patch"
        git -C "$diffrepo" diff -M --no-color HEAD~1 HEAD > "$patch" || true

        if [[ ! -s "$patch" ]]; then
            echo "No changes between $from_ref and $to_ref for these parameters/paths."
            exit 0
        fi

        echo "==> Importing template history into $project_dir so 3-way merge has a base"
        git -C "$project_dir" remote add archetype-sync-tmp "$diffrepo" 2>/dev/null || \
            git -C "$project_dir" remote set-url archetype-sync-tmp "$diffrepo"
        git -C "$project_dir" fetch --quiet archetype-sync-tmp
        git -C "$project_dir" remote remove archetype-sync-tmp

        echo "==> Applying diff to $project_dir"
        apply_status=0
        git -C "$project_dir" apply --3way --whitespace=nowarn "$patch" || apply_status=$?

        conflicts="$(git -C "$project_dir" diff --name-only --diff-filter=U || true)"

        if [[ $apply_status -ne 0 || -n "$conflicts" ]]; then
            echo
            echo "Applied with conflicts. Files needing manual resolution:"
            if [[ -n "$conflicts" ]]; then
                echo "$conflicts" | sed 's/^/  - /'
            else
                echo "  (git apply reported errors — see output above; some hunks may not have applied at all)"
            fi
            exit 1
        fi

        echo
        echo "Applied cleanly. Review with 'git diff', then commit."
        exit 0
    )
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
    [[ -f "$manifest" ]] || die "no $manifest found in '$project_dir' — run 'init' or 'onboard' first"

    require_clean_git_repo "$project_dir" "merge template changes with your customizations"

    [[ -n "$archetype_repo" ]] || archetype_repo="$(jq -r '.archetypeRepo' "$manifest")"
    local from_ref group_id artifact_id version package app_name forms_version
    from_ref="$(jq -r '.ref' "$manifest")"
    group_id="$(jq -r '.parameters.groupId' "$manifest")"
    artifact_id="$(jq -r '.parameters.artifactId' "$manifest")"
    version="$(jq -r '.parameters.version' "$manifest")"
    package="$(jq -r '.parameters.package' "$manifest")"
    app_name="$(jq -r '.parameters.appName' "$manifest")"
    forms_version="$(jq -r '.parameters.formsVersion' "$manifest")"

    local paths=()
    while IFS= read -r p; do paths+=("$p"); done < <(jq -r '.paths[]? // empty' "$manifest")

    local sync_status=0
    do_sync "$project_dir" "$archetype_repo" "$from_ref" "$to_ref" \
        "$group_id" "$artifact_id" "$version" "$package" "$app_name" "$forms_version" \
        "${paths[@]+"${paths[@]}"}" || sync_status=$?

    jq --arg ref "$to_ref" '.ref = $ref' "$manifest" > "$manifest.tmp" && mv "$manifest.tmp" "$manifest"

    if [[ $sync_status -ne 0 ]]; then
        echo
        echo "Resolve the <<<<<<< markers, then: git add -A && git commit"
        echo "$manifest was updated to ref '$to_ref' regardless — re-run update later and it will diff from here."
        exit 1
    fi

    echo "$manifest updated to ref '$to_ref'."
}

cmd_onboard() {
    local project_dir="" archetype_repo="" ref="HEAD" group_id="" artifact_id=""
    local version="" package="" app_name="" forms_version="afaacs"
    local paths_csv="$DEFAULT_ONBOARD_PATHS"
    local include_spec_to_code=false

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --project-dir) project_dir="$2"; shift 2 ;;
            --archetype-repo) archetype_repo="$2"; shift 2 ;;
            --ref) ref="$2"; shift 2 ;;
            --group-id) group_id="$2"; shift 2 ;;
            --artifact-id) artifact_id="$2"; shift 2 ;;
            --version) version="$2"; shift 2 ;;
            --package) package="$2"; shift 2 ;;
            --app-name) app_name="$2"; shift 2 ;;
            --forms-version) forms_version="$2"; shift 2 ;;
            --paths) paths_csv="$2"; shift 2 ;;
            --include-spec-to-code-generator) include_spec_to_code=true; shift ;;
            *) die "unknown argument: $1" ;;
        esac
    done

    [[ -n "$project_dir" ]] || die "--project-dir is required"
    [[ -n "$archetype_repo" ]] || die "--archetype-repo is required"
    [[ -n "$group_id" ]] || die "--group-id is required"
    [[ -n "$artifact_id" ]] || die "--artifact-id is required"
    [[ -n "$version" ]] || die "--version is required"
    [[ -n "$package" ]] || die "--package is required"
    [[ -n "$app_name" ]] || die "--app-name is required"
    [[ -d "$project_dir" ]] || die "project dir '$project_dir' does not exist"

    local manifest="$project_dir/.archetype-sync.json"
    [[ -f "$manifest" ]] && die "$manifest already exists — this project is already tracked; use 'update' instead"

    require_clean_git_repo "$project_dir" "add files without clobbering anything already there"

    local paths=()
    IFS=',' read -ra paths <<< "$paths_csv"

    if [[ "$include_spec_to_code" == true ]]; then
        local package_path
        package_path="$(echo "$package" | tr '.' '/')"
        paths+=("core/src/main/java/${package_path}/core/workflow/SpecToCodeGenerator.java")
        paths+=("core/src/test/java/${package_path}/core/workflow/SpecToCodeGeneratorTest.java")
    fi

    echo "==> Onboarding spec-driven-dev framework (${paths[*]}) from $archetype_repo @ $ref"

    local sync_status=0
    do_sync "$project_dir" "$archetype_repo" "EMPTY" "$ref" \
        "$group_id" "$artifact_id" "$version" "$package" "$app_name" "$forms_version" \
        "${paths[@]+"${paths[@]}"}" || sync_status=$?

    write_manifest "$manifest" "$archetype_repo" "$ref" "$group_id" "$artifact_id" \
        "$version" "$package" "$app_name" "$forms_version" "${paths[@]+"${paths[@]}"}"
    echo "Wrote $manifest"

    if [[ $sync_status -ne 0 ]]; then
        echo
        echo "Resolve the <<<<<<< markers, then: git add -A && git commit (including $manifest)."
        exit 1
    fi

    echo "Onboarded $project_dir @ $ref. Review with 'git status'/'git diff', then commit"
    echo "(including $manifest, so future 'update' runs stay scoped to this same subset)."
}

main() {
    need_bin git
    need_bin jq
    need_bin mvn

    local cmd="${1:-}"
    case "$cmd" in
        init) shift; cmd_init "$@" ;;
        update) shift; cmd_update "$@" ;;
        onboard) shift; cmd_onboard "$@" ;;
        -h|--help|"") usage ;;
        *) die "unknown command: $cmd (expected 'init', 'update', or 'onboard')" ;;
    esac
}

main "$@"
