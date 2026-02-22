# Information Architecture

This document outlines the sitemap, navigation structure, and content organization for the AEM BMAD Showcase website.

## Content Repository Structure

The site uses the standard AEM content hierarchy with language-rooted structure for multi-lingual support.

### Root Structure

```
/content/
  aem-bmad-showcase/
    en/                           # English (Master Language)
    fr/                           # French
    de/                           # German
    es/                           # Spanish

/content/dam/
  aem-bmad-showcase/
    images/
      hero/
      products/
      team/
    documents/
    videos/

/content/experience-fragments/
  aem-bmad-showcase/
    en/
      header/
      footer/
    fr/
    de/
    es/

/conf/
  aem-bmad-showcase/
    settings/
      wcm/
        templates/
        policies/
      cloudconfigs/
```

## Multi-lingual Site Structure

### Language Configuration

| Language | Code | Root Path | Master |
|----------|------|-----------|--------|
| English (US) | en | `/content/aem-bmad-showcase/en` | Yes |
| French | fr | `/content/aem-bmad-showcase/fr` | No |
| German | de | `/content/aem-bmad-showcase/de` | No |
| Spanish | es | `/content/aem-bmad-showcase/es` | No |

### Translation Workflow

1. Content is authored in English (master language)
2. Translation projects are created for target languages
3. Content is sent to translation provider via AEM Translation Framework
4. Translated content is reviewed and approved
5. Language copies are published independently

### Language-Specific Considerations

- **URL Structure**: Each language uses its own URL path (e.g., `/en/products`, `/fr/produits`)
- **Locale-Specific Content**: Some pages may have locale-specific variations
- **Shared Assets**: DAM assets are shared across languages with localized metadata

## Complete Sitemap

### English (`/content/aem-bmad-showcase/en`)

```
/content/aem-bmad-showcase/en                    # Home Page
├── /products                                     # Products Landing
│   ├── /product-category-a                       # Category Page
│   │   ├── /product-alpha                        # Product Detail
│   │   ├── /product-beta                         # Product Detail
│   │   └── /product-gamma                        # Product Detail
│   └── /product-category-b                       # Category Page
│       ├── /product-delta                        # Product Detail
│       └── /product-epsilon                      # Product Detail
├── /solutions                                    # Solutions Landing
│   ├── /enterprise                               # Solution Page
│   ├── /small-business                           # Solution Page
│   └── /startups                                 # Solution Page
├── /resources                                    # Resources Landing
│   ├── /blog                                     # Blog Listing
│   │   └── /yyyy/mm/article-name                 # Blog Article
│   ├── /case-studies                             # Case Studies Listing
│   │   └── /case-study-name                      # Case Study Detail
│   ├── /whitepapers                              # Whitepapers Listing
│   └── /webinars                                 # Webinars Listing
├── /about-us                                     # About Us
│   ├── /our-story                                # Company History
│   ├── /leadership                               # Leadership Team
│   ├── /careers                                  # Careers Landing
│   │   └── /job-title                            # Job Detail
│   └── /press                                    # Press Releases
│       └── /yyyy/mm/press-release-name           # Press Release
├── /support                                      # Support Landing
│   ├── /documentation                            # Docs Landing
│   ├── /faq                                      # FAQ Page
│   └── /contact-support                          # Support Contact
├── /contact                                      # Contact Page
├── /search-results                               # Search Results
├── /privacy-policy                               # Legal
├── /terms-of-service                             # Legal
├── /cookie-policy                                # Legal
└── /accessibility-statement                      # Legal
```

## Navigation Structure

### Primary Navigation (Header)

```
├── Products [Mega Menu]
│   ├── Category A
│   │   ├── Product Alpha
│   │   ├── Product Beta
│   │   └── Product Gamma
│   ├── Category B
│   │   ├── Product Delta
│   │   └── Product Epsilon
│   └── View All Products →
├── Solutions [Mega Menu]
│   ├── Enterprise
│   ├── Small Business
│   └── Startups
├── Resources [Mega Menu]
│   ├── Blog
│   ├── Case Studies
│   ├── Whitepapers
│   └── Webinars
├── About Us [Standard Dropdown]
│   ├── Our Story
│   ├── Leadership
│   ├── Careers
│   └── Press
└── Contact
```

### Utility Navigation (Header)

```
├── Search [Icon]
├── Language Switcher [Dropdown]
└── Login / Account [Icon]
```

### Footer Navigation

```
├── Column 1: Products
│   ├── Category A
│   ├── Category B
│   └── All Products
├── Column 2: Solutions
│   ├── Enterprise
│   ├── Small Business
│   └── Startups
├── Column 3: Resources
│   ├── Blog
│   ├── Case Studies
│   └── Documentation
├── Column 4: Company
│   ├── About Us
│   ├── Careers
│   ├── Press
│   └── Contact
└── Bottom Bar
    ├── © 2024 Company Name
    ├── Privacy Policy
    ├── Terms of Service
    ├── Cookie Policy
    ├── Accessibility
    ├── Social Links (LinkedIn, Twitter, Facebook, YouTube)
    └── Language Switcher
```

### Breadcrumb Structure

Breadcrumbs are automatically generated based on page hierarchy:

```
Home > Products > Category A > Product Alpha
Home > Resources > Blog > 2024 > January > Article Title
Home > About Us > Careers > Job Title
```

## URL Structure

### Vanity URLs

| Friendly URL | Target Path |
|--------------|-------------|
| /products | /content/aem-bmad-showcase/en/products |
| /blog | /content/aem-bmad-showcase/en/resources/blog |
| /contact | /content/aem-bmad-showcase/en/contact |
| /careers | /content/aem-bmad-showcase/en/about-us/careers |

### SEO URL Guidelines

- Use lowercase letters and hyphens
- Keep URLs short and descriptive
- Avoid special characters and parameters when possible
- Include relevant keywords
- Maintain consistent structure across languages

## Search Configuration

### Searchable Content

- Page titles and content
- Product descriptions and specifications
- Blog articles and resources
- Career listings
- FAQs

### Search Filters

- Content Type (Products, Solutions, Blog, etc.)
- Category/Tags
- Date Range (for blog/news)
- Language

### Search Results Display

- Title with link
- Meta description or excerpt
- Content type indicator
- Publication date (where applicable)
- Relevance score
