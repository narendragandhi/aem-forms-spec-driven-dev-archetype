# Custom Adaptive Form Component Design

This document provides detailed technical designs for custom Adaptive Form components, focusing on their React implementation, AEM Forms framework integration, and authoring dialog structure.

## Component Architecture Overview

Custom Adaptive Form components are built using React and integrated into the AEM Forms editor. The primary files are located in the `ui.frontend.react.forms.af` and `ui.apps` modules.

```
# React component source code
ui.frontend.react.forms.af/src/main/webpack/components/{component-name}.js

# AEM authoring configuration
ui.apps/src/main/content/jcr_root/apps/{app-id}/components/adaptiveForm/{component-name}/
  _cq_dialog/.content.xml       # Dialog for authoring in the Adaptive Forms editor
  .content.xml                  # Component node definition
```

---

## Custom Form Components

### Address Lookup Component

**Purpose**: To provide an enhanced user experience for address entry. Instead of multiple separate fields, this component provides a single text input that autocompletes addresses by calling a third-party API. This reduces user effort and improves data accuracy.

**Resource Type**: `aem-forms-bmad-showcase/components/adaptiveForm/addresslookup`

**Frontend (React Component)**: Located at `ui.frontend.react.forms.af/src/main/webpack/components/CustomAddressField.js`.

The component is built with React and uses the `@aem-forms/af-react-components` library to integrate with the Adaptive Forms framework.

```javascript
import React, { useState } from "react";
import { Field } from "@aem-forms/af-react-components";

const CustomAddressField = ({
  label,
  description,
  name,
  value,
  id,
  visible,
  enabled,
}) => {
  // ... component logic ...
};

// The wrapper function makes the component usable by AEM Forms
export default function (props) {
  return <Field {...props} render={CustomAddressField} />;
}
```

**Key Responsibilities of the React Component:**
-   Render the input field, label, and description.
-   Handle user input and trigger API calls to an address suggestion service.
-   Display a list of suggested addresses to the user.
-   When a user selects an address, update multiple fields in the Form Data Model (FDM) simultaneously (e.g., `street`, `city`, `state`, `postalCode`). This is achieved by calling form APIs provided by the framework.

**Authoring Dialog (`_cq_dialog/.content.xml`)**:

The authoring dialog allows form creators to configure the component's behavior. It is a standard AEM dialog structure.

| Field Name | Type | Tab | Label | Description |
|---|---|---|---|---|
| `./label` | `textfield` | Basic | Label | The text to display as the field's label. |
| `./description` | `textarea` | Basic | Description | Help text shown to the end-user. |
| `./bindRef` | `textfield` | Basic | Data Binding | The path to the parent object in the Form Data Model (e.g., `/applicant/address`). |
| `./apiKey` | `textfield` | Advanced | API Key | The API key for the address lookup service. Should be configured to pull from an OSGi service for security. |

**Backend (Supporting OSGi Service)**:

While most of the logic is in the frontend, a backend OSGi service is recommended for security and configuration management.

`com.example.forms.services.AppConfigurationService`
```java
@Component(service = AppConfigurationService.class)
@Designate(ocd = AppConfigurationService.Config.class)
public class AppConfigurationService {

    @interface Config {
        @AttributeDefinition(name = "Address Lookup API Key")
        String address_api_key() default "";
    }

    private String apiKey;

    @Activate
    protected void activate(final Config config) {
        this.apiKey = config.address_api_key();
    }

    public String getAddressApiKey() {
        return apiKey;
    }
}
```
The React component can then fetch this key via a custom Sling Servlet, preventing the key from being exposed directly in the frontend code.

---

## Component Best Practices for AEM Forms

### Accessibility

1.  All custom form fields must be fully keyboard accessible.
2.  Associate labels with form inputs correctly using the `for` attribute.
3.  Provide clear focus indicators.
4.  Use `aria-live` regions to announce dynamic changes, such as validation errors or address suggestions.

### Internationalization (i18n)

1.  All static text in the component (including labels in the dialog) must be authored and support i18n.
2.  The React component should fetch translated strings from AEM's i18n dictionaries.

### Performance

1.  **Debounce API Calls**: For autocomplete components like Address Lookup, debounce the input to avoid sending an API request on every keystroke.
2.  **Lazy Load Data**: For custom dropdowns with large datasets, fetch the data only when the user first interacts with the field.
3.  **Code Splitting**: Use Webpack's code splitting capabilities to ensure that the code for a complex custom component is only loaded when it is actually present on a form.

### Testing Requirements

Each custom component must have:
-   **Unit Tests (Jest/React Testing Library)** for the React component itself. Mock any external APIs and test the component's state and rendering logic.
-   **AEM Integration Tests**: Verify that the component's authoring dialog works correctly and that data is saved properly to the form model.
-   **UI Tests (Cypress)**: Write end-to-end tests that simulate a user filling out the custom component within a live Adaptive Form.
