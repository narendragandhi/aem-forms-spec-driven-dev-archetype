# Headless AEM Forms with BMAD - ${appName}

This document describes the implementation of headless Adaptive Forms within the ${appName} project.

## Architecture

The headless forms architecture consists of:

1.  **AEM Adaptive Form**: The source of truth for the form model.
2.  **Headless Form Service (BFF)**: An OSGi servlet at `/bin/bmad/headless-form-service`.
3.  **Headless Submit Servlet**: A mock servlet at `/bin/bmad/headless-submit`.
4.  **React Frontend Module**: Located in `ui.frontend.react.forms.af`.

## Headless Forms Options

| Approach | Description | Use Case |
|----------|-------------|----------|
| Visual Editor | Form created in AEM, rendered headless | AFaaCS, non-technical authors |
| SPA Editor | React app with AEM integration | Complex custom UIs |
| GraphQL | JSON schema via GraphQL | Mobile apps, custom frontends |

---

## Visual Editor Workflow (AFaaCS)

### What is the Visual Editor?

The Visual Editor in AEM Forms as a Cloud Service allows non-technical users to create Adaptive Forms using a drag-and-drop interface, then render them headlessly in React/Vue/Angular applications.

### Workflow

```
1. Create Form in AEM Forms (Visual Editor)
       │
       ▼
2. Configure Form Model (JSON Schema or FDM)
       │
       ▼
3. Add components (fields, panels, rules)
       │
       ▼
4. Publish form
       │
       ▼
5. Integrate with React/Vue/Angular app
```

### Creating a Form with Visual Editor

1. **Navigate to AEM Forms** → Forms → Forms & Documents
2. **Create** → Adaptive Form → **Using Visual Editor**
3. **Add components** from the component panel
4. **Configure** properties (labels, validation, binding)
5. **Add rules** using the rule editor
6. **Preview** form
7. **Publish** form

### Form Model Options

| Model | Description |
|-------|-------------|
| JSON Schema | Define form structure with JSON Schema |
| Form Data Model (FDM) | Connect to enterprise data sources |
| Blank Form | Start from scratch |

### JSON Schema Generation

```bash
# Generate JSON Schema from form
curl -X GET "https://publish-pXXX.adobeaemcloud.com/forms/forms/{formPath}/schema" \
  -H "Authorization: Bearer {token}"
```

### Example JSON Schema Output

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "firstName": {
      "type": "string",
      "title": "First Name",
      "minLength": 1
    },
    "lastName": {
      "type": "string",
      "title": "Last Name",
      "minLength": 1
    },
    "email": {
      "type": "string",
      "title": "Email",
      "format": "email"
    },
    "address": {
      "type": "object",
      "properties": {
        "street": { "type": "string" },
        "city": { "type": "string" },
        "state": { "type": "string" },
        "zip": { "type": "string" }
      }
    }
  },
  "required": ["firstName", "lastName", "email"]
}
```

---

## React SDK Integration

### Installation

```bash
npm install @adobe/aem-forms-af-react @adobe/aem-forms-af-react-components
```

### Basic Usage

```jsx
import { AdaptiveForm } from '@adobe/aem-forms-af-react';
import { mappingJsonToAdaptiveForm } from '@adobe/aem-forms-af-react-components';

const MyForm = () => {
    const [form, setForm] = useState(null);
    
    useEffect(() => {
        // Fetch form model
        fetch('/forms/forms/loan-application/schema')
            .then(res => res.json())
            .then(schema => {
                setForm(mappingJsonToAdaptiveForm(schema));
            });
    }, []);
    
    if (!form) return <Loading />;
    
    return (
        <AdaptiveForm
            formModel={form}
            onSubmit={handleSubmit}
            onError={handleError}
        />
    );
};
```

### Form Submission

```jsx
const handleSubmit = (data) => {
    console.log('Form submitted:', data);
    // data structure matches JSON Schema
};

const handleError = (errors) => {
    console.log('Validation errors:', errors);
};
```

---

## Vue SDK Integration

```bash
npm install @adobe/aem-forms-af-vue
```

```vue
<template>
  <AdaptiveForm
    :form-model="formModel"
    @submit="handleSubmit"
  />
</template>

<script setup>
import { AdaptiveForm } from '@adobe/aem-forms-af-vue';

const formModel = ref(null);

onMounted(async () => {
    const response = await fetch('/forms/forms/loan-application/schema');
    formModel.value = await response.json();
});

const handleSubmit = (data) => {
    console.log('Submitted:', data);
};
</script>
```

---

## Angular SDK Integration

```bash
npm install @adobe/aem-forms-af-angular
```

```typescript
import { Component } from '@angular/core';
import { AdaptiveFormModule } from '@adobe/aem-forms-af-angular';

@Component({
  selector: 'app-form',
  standalone: true,
  imports: [AdaptiveFormModule],
  template: `
    <adaptive-form
      [formModel]="formModel"
      (onSubmit)="onSubmit($event)"
    />
  `
})
export class FormComponent {
  formModel: any;
  
  async ngOnInit() {
    const response = await fetch('/forms/forms/loan-application/schema');
    this.formModel = await response.json();
  }
  
  onSubmit(data: any) {
    console.log('Submitted:', data);
  }
}
```

---

## Components

### 1. Backend Services (Core)

*   **`${package}.services.HeadlessFormService`**: Metadata orchestration for forms.
*   **`${package}.services.HeadlessSubmitServlet`**: Mock submission handler.

### 2. Frontend Module (React)

The `ui.frontend.react.forms.af` module uses Adobe's Headless Forms SDK.

### 3. Custom Components

Custom React components are developed in `ui.frontend.react.forms.af/src/main/webpack/components`.

Example: `CustomAddressField.js`

## How to use

1.  **Generate a Form**: Create an Adaptive Form in AEM.
2.  **Access Headless View**: Navigate to the React app with the `formPath` parameter.

## BMAD Benefits for Headless

*   **Model-First Design**: Unified form structure.
*   **Metadata Orchestration**: Versioning and agent hints.
*   **Decoupled Development**: Independent React development.
