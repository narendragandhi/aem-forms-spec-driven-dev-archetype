# Molecules & Formulas - Workflow Templates for AI Agents

> **Status**: This document describes the Molecule system for reusable workflow templates.

## What are Molecules?

Molecules are **reusable workflow templates** that coordinate multi-step work. They consist of:

1. **Formula** - TOML definition of the workflow steps
2. **Molecule** - Instantiated workflow with tracked progress

## Why Use Molecules?

| Manual Workflows | Molecules |
|------------------|-----------|
| Repeated setup each time | Reusable templates |
| Manual step tracking | Automatic progress |
| No checkpoint recovery | Wisps provide recovery points |
| Hard to scale | Built for 20+ agents |

## Formula Structure

```toml
# .beads/formulas/component-development.formula.toml
description = "Standard AEM component development workflow"
formula = "component-dev"
version = 1

[vars.component_name]
description = "Name of the component (e.g., accordion)"
required = true

[vars.resource_type]
description = "AEM resource type path"
required = false

[[steps]]
id = "model"
title = "Create Sling Model"
description = "Create the Java Sling Model with annotations"

[[steps]]
id = "htl"
title = "Create HTL Template"
description = "Create the HTL template file"
needs = ["model"]

[[steps]]
id = "dialog"
title = "Create Authoring Dialog"
description = "Create the Touch UI dialog XML"
needs = ["htl"]

[[steps]]
id = "clientlib"
title = "Create Client Library"
description = "Create CSS/JS for the component"
needs = ["htl"]

[[steps]]
id = "tests"
title = "Write Unit Tests"
description = "Create Jest unit tests"
needs = ["clientlib"]

[[steps]]
id = "review"
title = "Code Review"
description = "Perform code review"
needs = ["tests"]
```

## Available Formulas

This archetype includes these built-in formulas:

### 1. Component Development

```bash
bd formula list
# Output: component-development, bug-fix, integration, release
```

### 2. Bug Fix

```bash
bd cook bug-fix --var title="Fix auth bug" --var severity=high
```

### 3. Integration

```bash
bd cook integration --var name="Adobe Analytics" --var fdm=true
```

### 4. Release

```bash
bd cook release --var version=1.2.0
```

## Using Formulas

### List Available Formulas

```bash
bd formula list
```

### Cook a Formula (Execute)

```bash
# Execute a formula directly
bd cook component-development --var component_name=accordion --var resource_type=bmad/components/accordion
```

### Pour a Molecule (Track)

```bash
# Create trackable instance
bd mol pour component-development --var component_name=hero --var resource_type=bmad/components/hero
```

### List Active Molecules

```bash
bd mol list
```

## Molecule Types

### Wisps (Lightweight)

Steps are materialized at runtime. Lightweight, good for simple workflows:

```bash
bd mol pour formula-name --var key=value
```

### Poured Wisps (Checkpoint Recovery)

Steps materialized as sub-wisps with checkpoint recovery:

```bash
bd mol pour formula-name --var key=value --pours
```

## Wisp States

Each step in a molecule has states:

| State | Description |
|-------|-------------|
| `pending` | Not yet started |
| `in_progress` | Currently executing |
| `completed` | Step finished |
| `failed` | Step failed |
| `skipped` | Skipped due to failure |

## Example: Component Development

```bash
# 1. Create and track the molecule
bd mol pour component-development \
  --var component_name=accordion \
  --var resource_type=bmad-showcase/components/content/accordion

# 2. Check progress
bd mol show aem-archetype-m123

# 3. Work through steps
# (each step tracked in Beads)

# 4. Complete
bd mol close aem-archetype-m123 "Completed accordion component"
```

## BMAD Integration

Molecules integrate with BMAD phases:

### Phase 3: Architecture → Component

```bash
bd mol pour component-development \
  --var component_name=hero \
  --var design_ref="bmad/03-Architecture-Design/component-design.md"
```

### Phase 4: Development Sprint

```bash
# Create sprint molecule
bd mol pour sprint \
  --var sprint_name="Sprint 1" \
  --var goals="Implement core forms"
```

## Custom Formulas

Create your own formula in `.beads/formulas/`:

```toml
# .beads/formulas/my-formula.toml
description = "My custom workflow"
formula = "my-formula"
version = 1

[vars.my_var]
description = "My variable"
required = true

[[steps]]
id = "step1"
title = "First Step"
description = "Do something first"

[[steps]]
id = "step2"
title = "Second Step"
description = "Do something second"
needs = ["step1"]
```

## Best Practices

1. **Use standard formulas** for common workflows
2. **Create custom formulas** for repeated patterns
3. **Track with molecules** for visibility
4. **Use poured wisps** for critical workflows needing recovery
5. **Link to BD issues** for full traceability

## Migration from BMAD v5

In BMAD v5, workflows were defined in YAML files. Molecules provide:

- **Reusable templates**: Define once, use many times
- **Variable substitution**: Dynamic parameters
- **Dependency tracking**: Automatic `needs` resolution
- **Progress tracking**: Real-time molecule status

## See Also

- [GasTown Molecules Documentation](https://github.com/steveyegge/gastown/blob/main/docs/concepts/molecules.md)
- [BEADS-SETUP.md](./BEADS-SETUP.md)
- [CONVOY.md](./CONVOY.md)
- [BMAD Methodology](../BMAD-README.md)
