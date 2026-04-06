# AEM Forms Version Compatibility

> This document explains the `formsVersion` archetype property and helps you choose the right version for your project.

## Property: `formsVersion`

| Value | Description | AEM Version |
|-------|-------------|--------------|
| `afaacs` | Adaptive Forms as a Cloud Service | AEM as a Cloud Service |
| `6.5` | AEM Forms on-premise/AMS | AEM 6.5 + Forms Add-on |

## Choosing Your Version

```bash
# For AEM as a Cloud Service (default)
mvn archetype:generate ... -DformsVersion=afaacs

# For AEM 6.5 on-premise or AMS
mvn archetype:generate ... -DformsVersion=6.5
```

## Version Differences

### AFaaCS (Adaptive Forms as a Cloud Service)

**Advantages:**
- Auto-scaling with traffic
- Continuous delivery of new features
- No infrastructure management
- Edge Delivery Services integration
- Visual Editor for headless forms

**Considerations:**
- Limited offline capability
- Network dependency
- Some legacy APIs not available

**Key Features:**
- Visual Editor for form creation
- GraphQL for form schema
- React/Vue/Angular SDKs
- Edge Delivery Services (DocUMENTS)
- Automated PDF generation

### AEM Forms 6.5

**Advantages:**
- Full control over infrastructure
- Complete API parity
- Offline forms support
- Extended document services

**Considerations:**
- Manual scaling
- Self-managed updates

**Key Features:**
- Adaptive Forms (Core Components)
- Automated PDF generation
- Document of Record (DoR)
- Reader Extensions

## API Differences

| Feature | AFaaCS | AEM 6.5 |
|---------|--------|---------|
| Visual Editor | ✅ | ❌ |
| GraphQL | ✅ | ❌ |
| Edge Delivery | ✅ | ❌ |
| Forms Portal | ✅ | ✅ |
| PDF Services API | ✅ | ✅ |
| Reader Extensions | ✅ | ✅ |

## Migration Path

### From 6.5 to AFaaCS

1. **Form Migration**: Export forms as XDP, re-import in AFaaCS
2. **FDM Changes**: REST → GraphQL/OData
3. **Workflows**: Update workflow steps for cloud
4. **Custom Code**: Review for deprecated APIs

### Code Compatibility

```java
// AFaaCS - Use sling model exporters
@Model(adaptables = SlingHttpServletRequest.class)
@Exporter(name = "jackson", extensions = "json")
public class MyModel implements ComponentExporter {
    // Cloud-native approach
}

// 6.5 - Can still use older patterns
@Model(adaptables = SlingHttpServletRequest.class)
public class MyModel {
    // Legacy approach still works
}
```

## Template Selection

| Use Case | Recommended Version |
|----------|---------------------|
| New cloud-native project | AFaaCS |
| Enterprise AMS deployment | 6.5 |
| Need Visual Editor | AFaaCS |
| Complex custom integrations | 6.5 |
| Edge Delivery Services | AFaaCS |
| Offline forms | 6.5 |

## Project Structure by Version

### AFaaCS Structure

```
project/
├── ui.frontend.react.forms.af/  # React components
├── core/                        # OSGi bundles
├── bmad/
│   └── gastown/                 # AI orchestration
└── specs/                       # JSON schemas
```

### 6.5 Structure

```
project/
├── ui.apps/                     # Components + forms
├── core/                        # OSGi bundles
├── bmad/
│   └── gastown/
└── forms-portal/               # Forms Portal
```

## See Also

- [AFaaCS Documentation](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/forms/adaptive-forms/introduction-forms.html)
- [AEM 6.5 Forms Documentation](https://experienceleague.adobe.com/docs/experience-manager-65/forms/administrative-understand/introducing-forms.html)
- [BMAD Methodology](../BMAD-README.md)
