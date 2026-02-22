#!/bin/bash
#
# GasTown Workflow Initializer
# Creates BEAD issues for a new workflow based on workflow templates
#
# Usage: ./init-workflow.sh <workflow-type> "<task-description>" [priority]
#
# Examples:
#   ./init-workflow.sh component-development "Develop Hero Component" high
#   ./init-workflow.sh bug-fix "Fix null pointer in CardModel" critical
#   ./init-workflow.sh integration-development "Integrate Adobe Analytics"
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GASTOWN_DIR="$(dirname "$SCRIPT_DIR")"
BEAD_DIR="$GASTOWN_DIR/bead"
ISSUES_DIR="$BEAD_DIR/.issues"
WORKFLOWS_DIR="$GASTOWN_DIR/workflows"
TEMPLATES_DIR="$BEAD_DIR/templates"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_usage() {
    echo "Usage: $0 <workflow-type> \"<task-description>\" [priority]"
    echo ""
    echo "Workflow types:"
    echo "  component-development  - New AEM component"
    echo "  integration-development - External service integration"
    echo "  bug-fix                - Bug fix workflow"
    echo "  content-migration      - Content migration task"
    echo ""
    echo "Priority levels: critical, high, medium, low (default: medium)"
    echo ""
    echo "Examples:"
    echo "  $0 component-development \"Develop Accordion Component\" high"
    echo "  $0 bug-fix \"Fix XSS in Title component\" critical"
}

generate_workflow_id() {
    local prefix="$1"
    local timestamp=$(date +%Y%m%d%H%M%S)
    local random=$(printf '%04d' $((RANDOM % 10000)))
    echo "${prefix}-${timestamp:8:6}-${random}"
}

create_issue() {
    local agent="$1"
    local issue_type="$2"
    local issue_id="$3"
    local workflow_id="$4"
    local title="$5"
    local priority="$6"
    local depends_on="$7"
    local blocks="$8"
    local template_file="$9"

    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    local issue_file="$ISSUES_DIR/$agent/$issue_id.md"

    # Create agent directory if needed
    mkdir -p "$ISSUES_DIR/$agent"

    # Generate issue content
    cat > "$issue_file" << EOF
---
id: $issue_id
workflow_id: $workflow_id
type: $issue_type
agent: $agent
status: pending
priority: $priority
created: $timestamp
updated: $timestamp
depends_on: [$depends_on]
blocks: [$blocks]
---

# $title

## Context

<!-- Task context will be populated by Mayor during workflow initialization -->

## Acceptance Criteria

- [ ] All requirements met
- [ ] Code follows project standards
- [ ] Documentation complete

## Technical Details

<!-- Technical specifications -->

## Progress Log

### $timestamp
Issue created by init-workflow.sh during $workflow_id workflow initialization.

## Handoff Notes

<!-- For next agent in workflow -->

## Files Changed

<!-- Updated as work progresses -->

## Related Issues

EOF

    # Add related issues based on workflow
    if [ -n "$depends_on" ]; then
        echo "- Depends on: #$depends_on" >> "$issue_file"
    fi
    if [ -n "$blocks" ]; then
        for blocked in $(echo "$blocks" | tr ',' ' '); do
            echo "- Blocks: #$blocked" >> "$issue_file"
        done
    fi

    echo "$issue_file"
}

update_context_json() {
    local agent="$1"
    local workflow_id="$2"
    local workflow_name="$3"
    local issue_id="$4"
    local waiting_on="$5"
    local blocking="$6"

    local context_file="$ISSUES_DIR/$agent/context.json"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

    # Read existing context or create new
    if [ -f "$context_file" ]; then
        # Update existing context using a temp file approach
        local temp_file=$(mktemp)

        # Use Python for JSON manipulation (more reliable than jq on all systems)
        python3 << PYTHON > "$temp_file"
import json
import sys

with open("$context_file", 'r') as f:
    ctx = json.load(f)

ctx['last_updated'] = "$timestamp"
ctx['current_workflow'] = {
    "workflow_id": "$workflow_id",
    "workflow_name": "$workflow_name",
    "started_at": "$timestamp",
    "phase": "pending"
}

# Update dependencies
waiting_on_list = [x.strip() for x in "$waiting_on".split(',') if x.strip()]
blocking_list = [x.strip() for x in "$blocking".split(',') if x.strip()]

if waiting_on_list:
    ctx['dependencies']['waiting_on'] = [
        {"issue_id": issue_id, "blocking_agent": "coder", "since": "$timestamp"}
        for issue_id in waiting_on_list
    ]

if blocking_list:
    ctx['dependencies']['blocking'] = [
        {"issue_id": issue_id, "waiting_agent": agent}
        for issue_id, agent in [
            (b.split(':')[0], b.split(':')[1] if ':' in b else 'unknown')
            for b in blocking_list
        ]
    ]

print(json.dumps(ctx, indent=2))
PYTHON

        mv "$temp_file" "$context_file"
    fi
}

# Main script
main() {
    if [ $# -lt 2 ]; then
        show_usage
        exit 1
    fi

    local workflow_type="$1"
    local task_description="$2"
    local priority="${3:-medium}"

    # Validate workflow type
    local workflow_file="$WORKFLOWS_DIR/$workflow_type.yaml"
    if [ ! -f "$workflow_file" ]; then
        log_error "Unknown workflow type: $workflow_type"
        echo "Available workflows:"
        ls -1 "$WORKFLOWS_DIR"/*.yaml 2>/dev/null | xargs -n1 basename | sed 's/.yaml$//'
        exit 1
    fi

    # Generate workflow ID
    local prefix=$(echo "$workflow_type" | cut -c1-4 | tr '[:lower:]' '[:upper:]')
    local workflow_id=$(generate_workflow_id "$prefix")

    log_info "Initializing workflow: $workflow_id"
    log_info "Type: $workflow_type"
    log_info "Description: $task_description"
    log_info "Priority: $priority"
    echo ""

    # Create issues based on workflow type
    case "$workflow_type" in
        component-development)
            # Implementation issue
            local impl_id="${workflow_id}-impl-001"
            local test_id="${workflow_id}-test-001"
            local review_id="${workflow_id}-review-001"
            local docs_id="${workflow_id}-docs-001"

            log_info "Creating BEAD issues..."

            # Create implementation issue (no dependencies, blocks test and review)
            create_issue "coder" "implementation" "$impl_id" "$workflow_id" \
                "Implement: $task_description" "$priority" "" "$test_id,$review_id"
            log_success "Created: coder/$impl_id.md"

            # Create test issue (depends on impl, blocks review)
            create_issue "tester" "testing" "$test_id" "$workflow_id" \
                "Test: $task_description" "$priority" "$impl_id" "$review_id"
            log_success "Created: tester/$test_id.md"

            # Create review issue (depends on impl and test)
            create_issue "reviewer" "review" "$review_id" "$workflow_id" \
                "Review: $task_description" "$priority" "$impl_id,$test_id" ""
            log_success "Created: reviewer/$review_id.md"

            # Create docs issue (depends on review)
            create_issue "docs" "documentation" "$docs_id" "$workflow_id" \
                "Document: $task_description" "$priority" "$review_id" ""
            log_success "Created: docs/$docs_id.md"
            ;;

        bug-fix)
            local fix_id="${workflow_id}-fix-001"
            local test_id="${workflow_id}-test-001"
            local review_id="${workflow_id}-review-001"

            log_info "Creating BEAD issues..."

            create_issue "coder" "bugfix" "$fix_id" "$workflow_id" \
                "Fix: $task_description" "$priority" "" "$test_id,$review_id"
            log_success "Created: coder/$fix_id.md"

            create_issue "tester" "testing" "$test_id" "$workflow_id" \
                "Test fix: $task_description" "$priority" "$fix_id" "$review_id"
            log_success "Created: tester/$test_id.md"

            create_issue "reviewer" "review" "$review_id" "$workflow_id" \
                "Review fix: $task_description" "$priority" "$fix_id,$test_id" ""
            log_success "Created: reviewer/$review_id.md"
            ;;

        integration-development)
            local impl_id="${workflow_id}-impl-001"
            local config_id="${workflow_id}-config-001"
            local test_id="${workflow_id}-test-001"
            local review_id="${workflow_id}-review-001"

            log_info "Creating BEAD issues..."

            create_issue "coder" "implementation" "$impl_id" "$workflow_id" \
                "Implement: $task_description" "$priority" "" "$config_id,$test_id,$review_id"
            log_success "Created: coder/$impl_id.md"

            create_issue "dispatcher" "configuration" "$config_id" "$workflow_id" \
                "Configure Dispatcher: $task_description" "$priority" "$impl_id" "$review_id"
            log_success "Created: dispatcher/$config_id.md"

            create_issue "tester" "testing" "$test_id" "$workflow_id" \
                "Test: $task_description" "$priority" "$impl_id" "$review_id"
            log_success "Created: tester/$test_id.md"

            create_issue "reviewer" "review" "$review_id" "$workflow_id" \
                "Review: $task_description" "$priority" "$impl_id,$config_id,$test_id" ""
            log_success "Created: reviewer/$review_id.md"
            ;;

        *)
            log_warn "Generic workflow - creating basic issue set"
            local task_id="${workflow_id}-task-001"
            create_issue "coder" "task" "$task_id" "$workflow_id" \
                "$task_description" "$priority" "" ""
            log_success "Created: coder/$task_id.md"
            ;;
    esac

    echo ""
    log_success "Workflow $workflow_id initialized!"
    echo ""
    echo "Next steps:"
    echo "  1. Review created issues in: $ISSUES_DIR/"
    echo "  2. Add context to each issue's Context section"
    echo "  3. Invoke Mayor AI to orchestrate: "
    echo "     'Please read bmad/gastown/agents/mayor.md and orchestrate workflow $workflow_id'"
    echo ""
    echo "Check status with: ./status.sh"
}

main "$@"
