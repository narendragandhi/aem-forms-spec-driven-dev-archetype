#!/bin/bash
#
# GasTown Status Reporter
# Shows current status of all BEAD issues and agent states
#
# Usage: ./status.sh [options]
#
# Options:
#   -a, --agent <name>     Show issues for specific agent
#   -w, --workflow <id>    Show issues for specific workflow
#   -s, --status <status>  Filter by status (pending, in_progress, blocked, completed)
#   -v, --verbose          Show full issue details
#   -j, --json             Output as JSON
#   -h, --help             Show this help
#

set -e
shopt -s nullglob

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m'

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GASTOWN_DIR="$(dirname "$SCRIPT_DIR")"
BEAD_DIR="$GASTOWN_DIR/bead"
ISSUES_DIR="$BEAD_DIR/.issues"

# Options
FILTER_AGENT=""
FILTER_WORKFLOW=""
FILTER_STATUS=""
VERBOSE=false
JSON_OUTPUT=false

show_usage() {
    echo "GasTown Status Reporter"
    echo ""
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -a, --agent <name>     Show issues for specific agent (coder, tester, reviewer, etc.)"
    echo "  -w, --workflow <id>    Show issues for specific workflow ID"
    echo "  -s, --status <status>  Filter by status (pending, in_progress, blocked, completed)"
    echo "  -v, --verbose          Show full issue details"
    echo "  -j, --json             Output as JSON"
    echo "  -h, --help             Show this help"
    echo ""
    echo "Examples:"
    echo "  $0                     # Show all active issues"
    echo "  $0 -a coder            # Show coder's issues only"
    echo "  $0 -s blocked          # Show blocked issues"
    echo "  $0 -w COMP-001         # Show issues for workflow COMP-001"
}

parse_frontmatter() {
    local file="$1"
    local field="$2"

    # Extract value from YAML frontmatter
    sed -n '/^---$/,/^---$/p' "$file" | grep "^${field}:" | head -1 | sed "s/${field}:[[:space:]]*//" | tr -d '[]'
}

get_status_color() {
    local status="$1"
    case "$status" in
        pending) echo "$YELLOW" ;;
        in_progress) echo "$BLUE" ;;
        blocked) echo "$RED" ;;
        completed) echo "$GREEN" ;;
        *) echo "$NC" ;;
    esac
}

get_status_icon() {
    local status="$1"
    case "$status" in
        pending) echo "○" ;;
        in_progress) echo "◐" ;;
        blocked) echo "✗" ;;
        completed) echo "✓" ;;
        *) echo "?" ;;
    esac
}

get_priority_color() {
    local priority="$1"
    case "$priority" in
        critical) echo "$RED" ;;
        high) echo "$YELLOW" ;;
        medium) echo "$CYAN" ;;
        low) echo "$NC" ;;
        *) echo "$NC" ;;
    esac
}

print_header() {
    echo ""
    echo -e "${BOLD}GasTown Status Report${NC}"
    echo -e "${CYAN}$(date)${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

print_summary() {
    local total=0
    local pending=0
    local in_progress=0
    local blocked=0
    local completed=0

    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            for issue_file in "$agent_dir"/*.md; do
                if [ -f "$issue_file" ]; then
                    total=$((total + 1))
                    local status=$(parse_frontmatter "$issue_file" "status")
                    case "$status" in
                        pending) pending=$((pending + 1)) ;;
                        in_progress) in_progress=$((in_progress + 1)) ;;
                        blocked) blocked=$((blocked + 1)) ;;
                        completed) completed=$((completed + 1)) ;;
                    esac
                fi
            done
        fi
    done

    echo ""
    echo -e "${BOLD}Summary${NC}"
    echo -e "  Total Issues:    $total"
    echo -e "  ${YELLOW}○ Pending:${NC}       $pending"
    echo -e "  ${BLUE}◐ In Progress:${NC}   $in_progress"
    echo -e "  ${RED}✗ Blocked:${NC}       $blocked"
    echo -e "  ${GREEN}✓ Completed:${NC}     $completed"
}

print_agent_context() {
    local agent="$1"
    local context_file="$ISSUES_DIR/$agent/context.json"

    if [ -f "$context_file" ]; then
        echo -e "\n  ${CYAN}Context:${NC}"

        # Use Python for JSON parsing (cross-platform)
        python3 << PYTHON 2>/dev/null || true
import json
try:
    with open("$context_file", 'r') as f:
        ctx = json.load(f)

    if ctx.get('current_workflow'):
        wf = ctx['current_workflow']
        print(f"    Workflow: {wf.get('workflow_id', 'none')} ({wf.get('phase', 'unknown')})")

    waiting = ctx.get('dependencies', {}).get('waiting_on', [])
    if waiting:
        print(f"    Waiting on: {', '.join([w.get('issue_id', '?') for w in waiting])}")

    blocking = ctx.get('dependencies', {}).get('blocking', [])
    if blocking:
        print(f"    Blocking: {', '.join([b.get('issue_id', '?') for b in blocking])}")

    metrics = ctx.get('metrics', {})
    if metrics.get('issues_completed', 0) > 0:
        print(f"    Completed: {metrics['issues_completed']} issues")
except:
    pass
PYTHON
    fi
}

print_issues_for_agent() {
    local agent="$1"
    local agent_dir="$ISSUES_DIR/$agent"
    local has_issues=false

    if [ ! -d "$agent_dir" ]; then
        return
    fi

    shopt -s nullglob
    for issue_file in "$agent_dir"/*.md; do
        if [ -f "$issue_file" ] && [ "$(basename "$issue_file")" != "README.md" ]; then
            local issue_id=$(parse_frontmatter "$issue_file" "id")
            local status=$(parse_frontmatter "$issue_file" "status")
            local priority=$(parse_frontmatter "$issue_file" "priority")
            local workflow_id=$(parse_frontmatter "$issue_file" "workflow_id")
            local depends_on=$(parse_frontmatter "$issue_file" "depends_on")
            local title=$(grep "^# " "$issue_file" | head -1 | sed 's/^# //')

            # Apply filters
            if [ -n "$FILTER_STATUS" ] && [ "$status" != "$FILTER_STATUS" ]; then
                continue
            fi
            if [ -n "$FILTER_WORKFLOW" ] && [ "$workflow_id" != "$FILTER_WORKFLOW" ]; then
                continue
            fi

            if [ "$has_issues" = false ]; then
                echo ""
                echo -e "${BOLD}${MAGENTA}[$agent]${NC}"
                has_issues=true
            fi

            local status_color=$(get_status_color "$status")
            local status_icon=$(get_status_icon "$status")
            local priority_color=$(get_priority_color "$priority")

            echo -e "  ${status_color}${status_icon}${NC} ${BOLD}$issue_id${NC} [$priority_color$priority${NC}]"
            echo -e "    $title"

            if [ "$VERBOSE" = true ]; then
                echo -e "    ${CYAN}Workflow:${NC} $workflow_id"
                if [ -n "$depends_on" ] && [ "$depends_on" != "" ]; then
                    echo -e "    ${CYAN}Depends on:${NC} $depends_on"
                fi

                # Show last progress entry
                local last_progress=$(grep -A1 "^### " "$issue_file" | tail -2 | head -1)
                if [ -n "$last_progress" ]; then
                    echo -e "    ${CYAN}Last update:${NC} ${last_progress:0:60}..."
                fi
            fi
        fi
    done

    if [ "$has_issues" = true ] && [ "$VERBOSE" = true ]; then
        print_agent_context "$agent"
    fi
}

print_dependency_graph() {
    echo ""
    echo -e "${BOLD}Dependency Graph${NC}"
    echo ""

    # Collect all issues with dependencies
    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            local agent=$(basename "$agent_dir")
            for issue_file in "$agent_dir"/*.md; do
                if [ -f "$issue_file" ] && [ "$(basename "$issue_file")" != "README.md" ]; then
                    local issue_id=$(parse_frontmatter "$issue_file" "id")
                    local status=$(parse_frontmatter "$issue_file" "status")
                    local depends_on=$(parse_frontmatter "$issue_file" "depends_on")
                    local blocks=$(parse_frontmatter "$issue_file" "blocks")

                    local status_icon=$(get_status_icon "$status")
                    local status_color=$(get_status_color "$status")

                    if [ -n "$blocks" ] && [ "$blocks" != "" ]; then
                        echo -e "  ${status_color}${status_icon}${NC} $issue_id"
                        for blocked in $(echo "$blocks" | tr ',' ' '); do
                            blocked=$(echo "$blocked" | tr -d ' ')
                            if [ -n "$blocked" ]; then
                                echo -e "     └─► $blocked"
                            fi
                        done
                    fi
                fi
            done
        fi
    done
}

output_json() {
    echo "{"
    echo "  \"timestamp\": \"$(date -u +"%Y-%m-%dT%H:%M:%SZ")\","
    echo "  \"agents\": {"

    local first_agent=true
    for agent_dir in "$ISSUES_DIR"/*/; do
        if [ -d "$agent_dir" ]; then
            local agent=$(basename "$agent_dir")

            if [ "$first_agent" = false ]; then
                echo ","
            fi
            first_agent=false

            echo "    \"$agent\": {"
            echo "      \"issues\": ["

            local first_issue=true
            for issue_file in "$agent_dir"/*.md; do
                if [ -f "$issue_file" ] && [ "$(basename "$issue_file")" != "README.md" ]; then
                    if [ "$first_issue" = false ]; then
                        echo ","
                    fi
                    first_issue=false

                    local issue_id=$(parse_frontmatter "$issue_file" "id")
                    local status=$(parse_frontmatter "$issue_file" "status")
                    local priority=$(parse_frontmatter "$issue_file" "priority")
                    local workflow_id=$(parse_frontmatter "$issue_file" "workflow_id")

                    echo "        {"
                    echo "          \"id\": \"$issue_id\","
                    echo "          \"status\": \"$status\","
                    echo "          \"priority\": \"$priority\","
                    echo "          \"workflow_id\": \"$workflow_id\""
                    echo -n "        }"
                fi
            done

            echo ""
            echo "      ]"
            echo -n "    }"
        fi
    done

    echo ""
    echo "  }"
    echo "}"
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -a|--agent)
            FILTER_AGENT="$2"
            shift 2
            ;;
        -w|--workflow)
            FILTER_WORKFLOW="$2"
            shift 2
            ;;
        -s|--status)
            FILTER_STATUS="$2"
            shift 2
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -j|--json)
            JSON_OUTPUT=true
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
    if [ "$JSON_OUTPUT" = true ]; then
        output_json
        exit 0
    fi

    print_header

    if [ -n "$FILTER_AGENT" ]; then
        print_issues_for_agent "$FILTER_AGENT"
    else
        # Print issues by agent (excluding completed and special dirs)
        for agent in coder tester reviewer dispatcher docs; do
            print_issues_for_agent "$agent"
        done

        # Check inbox and blocked
        print_issues_for_agent "inbox"
        print_issues_for_agent "blocked"
    fi

    if [ "$VERBOSE" = true ]; then
        print_dependency_graph
    fi

    print_summary
    echo ""
}

main
