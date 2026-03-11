# Design System Integration

This document defines the design system guidelines for the AEM BMAD Showcase project, ensuring visual consistency and efficient component development.

## Design Tokens

Design tokens are the foundational values that define the visual language of the application. They are used consistently across all components.

### Color Palette

```scss
// Primary Colors
$color-primary: #0066CC;
$color-primary-dark: #004A99;
$color-primary-light: #3385D6;

// Secondary Colors
$color-secondary: #2E3440;
$color-secondary-dark: #1E222A;
$color-secondary-light: #4C566A;

// Accent Colors
$color-accent: #88C0D0;
$color-accent-success: #A3BE8C;
$color-accent-warning: #EBCB8B;
$color-accent-error: #BF616A;

// Neutral Colors
$color-neutral-100: #FFFFFF;
$color-neutral-200: #F5F7FA;
$color-neutral-300: #E5E9F0;
$color-neutral-400: #D8DEE9;
$color-neutral-500: #B0B8C4;
$color-neutral-600: #6B7280;
$color-neutral-700: #4B5563;
$color-neutral-800: #1F2937;
$color-neutral-900: #111827;
```

### Typography

```scss
// Font Families
$font-family-primary: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
$font-family-heading: 'Poppins', $font-family-primary;
$font-family-mono: 'JetBrains Mono', monospace;

// Font Sizes
$font-size-xs: 0.75rem;    // 12px
$font-size-sm: 0.875rem;   // 14px
$font-size-base: 1rem;     // 16px
$font-size-lg: 1.125rem;   // 18px
$font-size-xl: 1.25rem;    // 20px
$font-size-2xl: 1.5rem;    // 24px
$font-size-3xl: 1.875rem;  // 30px
$font-size-4xl: 2.25rem;   // 36px
$font-size-5xl: 3rem;      // 48px
$font-size-6xl: 3.75rem;   // 60px

// Font Weights
$font-weight-normal: 400;
$font-weight-medium: 500;
$font-weight-semibold: 600;
$font-weight-bold: 700;

// Line Heights
$line-height-tight: 1.25;
$line-height-normal: 1.5;
$line-height-relaxed: 1.75;
```

### Spacing

```scss
// Spacing Scale (based on 4px grid)
$spacing-0: 0;
$spacing-1: 0.25rem;   // 4px
$spacing-2: 0.5rem;    // 8px
$spacing-3: 0.75rem;   // 12px
$spacing-4: 1rem;      // 16px
$spacing-5: 1.25rem;   // 20px
$spacing-6: 1.5rem;    // 24px
$spacing-8: 2rem;      // 32px
$spacing-10: 2.5rem;   // 40px
$spacing-12: 3rem;     // 48px
$spacing-16: 4rem;     // 64px
$spacing-20: 5rem;     // 80px
$spacing-24: 6rem;     // 96px
```

### Breakpoints

```scss
// Responsive Breakpoints
$breakpoint-sm: 640px;
$breakpoint-md: 768px;
$breakpoint-lg: 1024px;
$breakpoint-xl: 1280px;
$breakpoint-2xl: 1536px;
```

### Shadows

```scss
// Box Shadows
$shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
$shadow-base: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
$shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
$shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
$shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
```

### Border Radius

```scss
// Border Radius
$radius-none: 0;
$radius-sm: 0.125rem;   // 2px
$radius-base: 0.25rem;  // 4px
$radius-md: 0.375rem;   // 6px
$radius-lg: 0.5rem;     // 8px
$radius-xl: 0.75rem;    // 12px
$radius-2xl: 1rem;      // 16px
$radius-full: 9999px;
```

## Component Patterns

### Button Styles

```scss
// Button Base
.btn {
  padding: $spacing-3 $spacing-6;
  font-family: $font-family-primary;
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  border-radius: $radius-md;
  transition: all 0.2s ease;
}

// Button Variants
.btn--primary {
  background-color: $color-primary;
  color: $color-neutral-100;
}

.btn--secondary {
  background-color: $color-neutral-200;
  color: $color-neutral-800;
}

.btn--outline {
  border: 2px solid $color-primary;
  color: $color-primary;
}
```

### Card Pattern

```scss
.card {
  background-color: $color-neutral-100;
  border-radius: $radius-lg;
  box-shadow: $shadow-base;
  padding: $spacing-6;
}
```

## Grid System

The layout uses a 12-column grid system with responsive gutters.

```scss
// Container
.container {
  max-width: $breakpoint-xl;
  margin: 0 auto;
  padding: 0 $spacing-4;
  
  @media (min-width: $breakpoint-md) {
    padding: 0 $spacing-6;
  }
  
  @media (min-width: $breakpoint-lg) {
    padding: 0 $spacing-8;
  }
}
```

## Accessibility Requirements

- All color combinations must meet WCAG 2.1 AA contrast requirements (4.5:1 for normal text, 3:1 for large text)
- Focus states must be clearly visible with a minimum 3:1 contrast ratio
- Interactive elements must have a minimum touch target size of 44x44 pixels
- All design tokens support both light and dark mode (future enhancement)

## Implementation in AEM

### Client Library Structure

```
/apps/aem-bmad-showcase/clientlibs/
  clientlib-base/
    css/
      tokens.scss        # Design tokens
      reset.scss         # CSS reset
      typography.scss    # Typography styles
      utilities.scss     # Utility classes
    js.txt
    css.txt
  clientlib-components/
    css/
      hero.scss
      text-with-image.scss
      carousel.scss
      card-grid.scss
```

### Using Tokens in Components

When developing components, always reference design tokens instead of hardcoded values:

```scss
// Good - Uses design tokens
.hero__heading {
  font-family: $font-family-heading;
  font-size: $font-size-5xl;
  color: $color-neutral-100;
  margin-bottom: $spacing-4;
}

// Bad - Hardcoded values
.hero__heading {
  font-family: 'Poppins', sans-serif;
  font-size: 48px;
  color: #FFFFFF;
  margin-bottom: 16px;
}
```

## Version Control

Design token updates should be tracked with semantic versioning:
- **Major**: Breaking changes to existing tokens
- **Minor**: New tokens added
- **Patch**: Bug fixes or adjustments to token values

## Omnichannel Token Sync

To ensure visual consistency across AEM-rendered forms and Headless React SPAs, this project employs a **Unified Token Contract** using CSS Variables (`--bmad-`).

### The Sync Mechanism
1. **AEM Theme (Source of Truth)**: Tokens are defined in `ui.theme.forms` within `variables.css`.
2. **Headless SPA (Consumer)**: The React application in `ui.frontend.react.forms.af` mirrors these tokens in its local `App.css`.
3. **Runtime Override**: When the Headless app is embedded or integrated, it can dynamically ingest tokens from the AEM ClientLib, ensuring that a brand change in AEM instantly reflects in the SPA.

### Token Naming Convention
All tokens MUST follow the `--bmad-{category}-{property}` pattern:
- `--bmad-color-primary`
- `--bmad-type-font-family`
- `--bmad-space-margin-md`

### Enforcement Rules
- **No Hardcoded Values**: Any UI code (Java, HTL, or React) containing hex codes, RGB, or hardcoded pixel values for theme-related properties will be rejected during Code Review.
- **Token-Only Styling**: All styling must reference a `--bmad-` variable or a standard design system utility class.
