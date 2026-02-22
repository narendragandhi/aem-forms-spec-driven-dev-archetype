#!/bin/bash
#
# GasTown Validation Script
# Validates BEAD issues and context.json files for consistency
#
# Usage: ./validate.sh [options]
#
# Options:
#   -f, --fix          Attempt to auto-fix issues
#   -v, --verbose      Show detailed output
#   -h, --help         Show this help
#

set -e
shopt -s nullglob

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GASTOWN_DIR="$(dirname "$SCRIPT_DIR")"
BEAD_DIR="$GASTOWN_DIR/bead"
ISSUES_DIR="$BEAD_DIR/.issues"

# Options
FIX_MODE=false
VERBOSE=false

# Counters
ERRORS=0
WARNINGS=0
FIXED=0

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    ((ERRORS++))
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    ((WARNINGS++))
}

log_ok() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_info() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${BLUE}[INFO]${NC} $1"
    fi
}

log_fixed() {
    echo -e "${GREEN}[FIXED]${NC} $1"
    ((FIXED++))
}

show_usage() {
    echo "GasTown Validation Script"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -f, --fix          Attempt to auto-fix issues"
    echo "  -v, --verbose      Show detailed output"
    echo "  -h, --help         Show this help"
}

# Parse frontmatter field
parse_frontmatter() {
    local file="$1"
    local field="$2"
    sed -n '/^---$/,/^---$/p' "$file" | grep "^${field}:" | head -1 | sed "s/${field}:[[:space:]]*//" | tr -d '[]'
}

# Validate issue file structure
validate_issue() {
    local file="$1"
    local filename=$(basename "$file")
    local agent_dir=$(dirname "$file")
    local agent=$(basename "$agent_dir")

    log_info "Validating: $agent/$filename"

    # Check frontmatter exists
    if ! head -1 "$file" | grep -q "^---$"; then
        log_error "$agent/$filename: Missing YAML frontmatter"
        return
    fi

    # Check required fields
    local id=$(parse_frontmatter "$file" "id")
    local status=$(parse_frontmatter "$file" "status")
    local agent_field=$(parse_frontmatter "$file" "agent")
    local priority=$(parse_frontmatter "$file" "priority")

    if [ -z "$id" ]; then
        log_error "$agent/$filename: Missing 'id' field"
    fi

    if [ -z "$status" ]; then
        log_error "$agent/$filename: Missing 'status' field"
    elif ! echo "$status" | grep -qE "^(pending|in_progress|blocked|completed|cancelled)$"; then
        log_error "$agent/$filename: Invalid status '$status'"
    fi

    if [ -z "$agent_field" ]; then
        log_error "$agent/$filename: Missing 'agent' field"
    elif [ "$agent_field" != "$agent" ] && [ "$agent" != "completed" ] && [ "$agent" != "blocked" ] && [ "$agent" != "inbox" ]; then
        log_warn "$agent/$filename: Agent mismatch - file in '$agent' but agent field is '$agent_field'"
    fi

    if [ -z "$priority" ]; then
        log_warn "$agent/$filename: Missing 'priority' field"
    elif ! echo "$priority" | grep -qE "^(critical|high|medium|low)$"; then
        log_warn "$agent/$filename: Invalid priority '$priority'"
    fi

    # Check for required sections
    if ! grep -q "^## Context" "$file"; then
        log_warn "$agent/$filename: Missing '## Context' section"
    fi

    if ! grep -q "^## Acceptance Criteria" "$file"; then
        log_warn "$agent/$filename: Missing '## Acceptance Criteria' section"
    fi

    if ! grep -q "^## Progress Log" "$file"; then
        log_warn "$agent/$filename: Missing '## Progress Log' section"
    fi
}

# Validate context.json file
validate_context() {
    local file="$1"
    local agent_dir=$(dirname "$file")
    local agent=$(basename "$agent_dir")

    log_info "Validating context: $agent/context.json"

    # Check if valid JSON
    if ! python3 -c "import json; json.load(open('$file'))" 2>/dev/null; then
        log_error "$agent/context.json: Invalid JSON"
        return
    fi

    # Validate structure
    python3 << PYTHON
import json
import sys

try:
    with open("$file", 'r') as f:
        ctx = json.load(f)

    errors = []
    warnings = []

    # Required fields
    if 'agent' not in ctx:
        errors.append("Missing 'agent' field")
    elif ctx['agent'] != "$agent":
        warnings.append(f"Agent mismatch: file in '$agent' but agent field is '{ctx['agent']}'")

    if 'last_updated' not in ctx:
        warnings.append("Missing 'last_updated' field")

    if 'dependencies' not in ctx:
        warnings.append("Missing 'dependencies' field")
    else:
        if 'waiting_on' not in ctx['dependencies']:
            warnings.append("Missing 'dependencies.waiting_on' field")
        if 'blocking' not in ctx['dependencies']:
            warnings.append("Missing 'dependencies.blocking' field")

    if 'handoffs' not in ctx:
        warnings.append("Missing 'handoffs' field")

    if 'metrics' not in ctx:
        warnings.append("Missing 'metrics' field")

    # Print results
    for e in errors:
        print(f"ERROR:$agent/context.json: {e}")
    for w in warnings:
        print(f"WARN:$agent/context.json: {w}")

except Exception as e:
    print(f"ERROR:$agent/context.json: {str(e)}")
PYTHON
}

# Check dependency consistency
validate_dependencies() {
    echo ""
    echo -e "${BLUE}Validating dependency consistency...${NC}"

    # Build dependency map
    declare -A blocks_map
    declare -A depends_on_map

    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            for issue_file in "$agent_dir"/*.md; do
                if [ -f "$issue_file" ] && [ "$(basename "$issue_file")" != "README.md" ]; then
                    local id=$(parse_frontmatter "$issue_file" "id")
                    local blocks=$(parse_frontmatter "$issue_file" "blocks")
                    local depends_on=$(parse_frontmatter "$issue_file" "depends_on")

                    if [ -n "$id" ]; then
                        blocks_map["$id"]="$blocks"
                        depends_on_map["$id"]="$depends_on"
                    fi
                fi
            done
        fi
    done

    # Check bidirectional consistency
    for id in "${!blocks_map[@]}"; do
        local blocks="${blocks_map[$id]}"
        if [ -n "$blocks" ]; then
            for blocked_id in $(echo "$blocks" | tr ',' ' '); do
                blocked_id=$(echo "$blocked_id" | tr -d ' ')
                if [ -n "$blocked_id" ]; then
                    local their_depends="${depends_on_map[$blocked_id]}"
                    if [ -z "$their_depends" ] || ! echo "$their_depends" | grep -q "$id"; then
                        log_warn "Dependency mismatch: $id blocks $blocked_id, but $blocked_id doesn't depend on $id"
                    fi
                fi
            done
        fi
    done

    for id in "${!depends_on_map[@]}"; do
        local depends="${depends_on_map[$id]}"
        if [ -n "$depends" ]; then
            for dep_id in $(echo "$depends" | tr ',' ' '); do
                dep_id=$(echo "$dep_id" | tr -d ' ')
                if [ -n "$dep_id" ]; then
                    local their_blocks="${blocks_map[$dep_id]}"
                    if [ -z "$their_blocks" ] || ! echo "$their_blocks" | grep -q "$id"; then
                        log_warn "Dependency mismatch: $id depends on $dep_id, but $dep_id doesn't block $id"
                    fi
                fi
            done
        fi
    done
}

# Check for orphaned issues
validate_orphans() {
    echo ""
    echo -e "${BLUE}Checking for orphaned issues...${NC}"

    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            local agent=$(basename "$agent_dir")
            # Skip system directories
            if [ "$agent" = "completed" ] || [ "$agent" = "blocked" ]; then
                continue
            fi

            for issue_file in "$agent_dir"/*.md; do
                if [ -f "$issue_file" ] && [ "$(basename "$issue_file")" != "README.md" ]; then
                    local status=$(parse_frontmatter "$issue_file" "status")
                    local updated=$(parse_frontmatter "$issue_file" "updated")

                    # Check if completed issues are in wrong directory
                    if [ "$status" = "completed" ] && [ "$agent" != "completed" ]; then
                        log_warn "$(basename "$issue_file"): Status is 'completed' but file is not in completed/"

                        if [ "$FIX_MODE" = true ]; then
                            mv "$issue_file" "$ISSUES_DIR/completed/"
                            log_fixed "Moved $(basename "$issue_file") to completed/"
                        fi
                    fi

                    # Check if blocked issues are tracked
                    if [ "$status" = "blocked" ] && [ "$agent" != "blocked" ]; then
                        log_info "$(basename "$issue_file"): Status is 'blocked' - consider moving to blocked/"
                    fi
                fi
            done
        fi
    done
}

# Validate all agent directories exist
validate_structure() {
    echo ""
    echo -e "${BLUE}Validating directory structure...${NC}"

    local required_dirs=("inbox" "coder" "tester" "reviewer" "dispatcher" "docs" "blocked" "completed")

    for dir in "${required_dirs[@]}"; do
        if [ ! -d "$ISSUES_DIR/$dir" ]; then
            log_warn "Missing agent directory: $dir"

            if [ "$FIX_MODE" = true ]; then
                mkdir -p "$ISSUES_DIR/$dir"
                log_fixed "Created directory: $dir"
            fi
        else
            log_ok "Directory exists: $dir"
        fi

        # Check for context.json
        if [ "$dir" != "inbox" ] && [ "$dir" != "blocked" ] && [ "$dir" != "completed" ]; then
            if [ ! -f "$ISSUES_DIR/$dir/context.json" ]; then
                log_warn "Missing context.json in $dir"
            fi
        fi
    done
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--fix)
            FIX_MODE=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Main
main() {
    echo ""
    echo -e "${BLUE}GasTown Validation${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    # Validate structure
    validate_structure

    # Validate all issues
    echo ""
    echo -e "${BLUE}Validating BEAD issues...${NC}"

    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            for issue_file in "$agent_dir"/*.md; do
                if [ -f "$issue_file" ] && [ "$(basename "$issue_file")" != "README.md" ]; then
                    validate_issue "$issue_file"
                fi
            done
        fi
    done

    # Validate context files
    echo ""
    echo -e "${BLUE}Validating context.json files...${NC}"

    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            local context_file="$agent_dir/context.json"
            if [ -f "$context_file" ]; then
                validate_context "$context_file"
            fi
        fi
    done

    # Validate dependencies
    validate_dependencies

    # Check for orphans
    validate_orphans

    # Summary
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo -e "${BLUE}Validation Summary${NC}"
    echo ""

    if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}All validations passed!${NC}"
    else
        echo -e "  ${RED}Errors:${NC}   $ERRORS"
        echo -e "  ${YELLOW}Warnings:${NC} $WARNINGS"
        if [ "$FIX_MODE" = true ]; then
            echo -e "  ${GREEN}Fixed:${NC}    $FIXED"
        fi
    fi

    echo ""

    if [ $ERRORS -gt 0 ]; then
        exit 1
    fi
}

main
